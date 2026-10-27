package com.mirrorself.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.gson.*;
import com.mirrorself.config.ExtractionProperties;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Actual Spring AI HTTP adapter, synthetic inputs and a local simulated provider only. */
class SpringAiModelGatewayTest {
    private static final String API_KEY = "PRIVATE_API_KEY_FIXTURE";
    private static final String CHAT = "PRIVATE_CHAT_FIXTURE";
    private static final String RESPONSE = "PRIVATE_RESPONSE_FIXTURE";
    private static final String REASONING = "PRIVATE_REASONING_FIXTURE";
    HttpServer server;
    ExecutorService executor;
    final ModelAuthorization auth = mock(ModelAuthorization.class);
    final AtomicInteger calls = new AtomicInteger();
    final AtomicReference<JsonObject> lastRequest = new AtomicReference<>();
    final AtomicReference<String> lastPath = new AtomicReference<>();
    final Logger logger = (Logger) LoggerFactory.getLogger(SpringAiModelGateway.class);
    final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    Level previousLevel;
    String rawResponse;
    boolean requireJsonInstruction;

    @BeforeEach void captureSafeDiagnostics() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach void stop() {
        if (server != null) server.stop(0);
        if (executor != null) executor.shutdownNow();
        logger.detachAppender(logs);
        logger.setLevel(previousLevel);
        logs.stop();
    }

    SpringAiModelGateway start(int status, String content, long delay, Duration timeout) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            var request = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
            lastRequest.set(request);
            lastPath.set(exchange.getRequestURI().getPath());
            assertEquals("none", request.get("tool_choice").getAsString());
            assertFalse(request.get("store").getAsBoolean());
            assertFalse(request.has("tools"));
            assertEquals(4096, request.get("max_completion_tokens").getAsInt());
            assertEquals("json_object", request.getAsJsonObject("response_format").get("type").getAsString());
            assertEquals("system", request.getAsJsonArray("messages").get(0).getAsJsonObject().get("role").getAsString());
            boolean mentionsJson = request.getAsJsonArray("messages").asList().stream()
                    .map(message -> message.getAsJsonObject().get("content").getAsString())
                    .anyMatch(text -> text.toLowerCase(Locale.ROOT).contains("json"));
            int responseStatus = requireJsonInstruction && !mentionsJson ? 400 : status;
            try { Thread.sleep(delay); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            JsonObject message = new JsonObject();
            message.addProperty("role", "assistant");
            message.addProperty("content", content);
            message.addProperty("reasoning_content", REASONING);
            JsonObject choice = new JsonObject();
            choice.addProperty("index", 0);
            choice.add("message", message);
            choice.addProperty("finish_reason", "stop");
            JsonArray choices = new JsonArray();
            choices.add(choice);
            JsonObject response = new JsonObject();
            response.addProperty("id", "fictional-completion");
            response.addProperty("object", "chat.completion");
            response.addProperty("created", 1);
            response.addProperty("model", "fictional");
            response.add("choices", choices);
            String body = rawResponse != null ? rawResponse : responseStatus == 200 ? response.toString()
                    : "{\"error\":{\"message\":\"" + RESPONSE + " " + REASONING
                            + "\",\"type\":\"server_error\"}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        var props = new ExtractionProperties(ExtractionProperties.Mode.LLM,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", API_KEY,
                "fictional", timeout, 16000, 30, true);
        return new SpringAiModelGateway(props, auth);
    }

    String call(SpringAiModelGateway gateway) {
        return gateway.generate(UUID.randomUUID(), null, "Extract only JSON", CHAT, List.of(
                new SourceView(UUID.randomUUID(), "CHATGPT", "c:1", "test", "user", CHAT, "{}", null)));
    }

    private void assertNoPrivateDiagnostics() {
        assertFalse(logs.list.isEmpty());
        for (var event : logs.list) {
            assertNull(event.getThrowableProxy(), "Never attach provider exceptions to logging events");
            String text = event.getFormattedMessage();
            for (String privateValue : List.of(API_KEY, CHAT, RESPONSE, REASONING, "Authorization", "Bearer")) {
                assertFalse(text.contains(privateValue), "Diagnostic contains a private fixture marker");
            }
            assertTrue(text.matches("Model call (completed: elapsedMs=\\d+|failed: exceptionType=[\\w.$]+, "
                    + "causeTypes=\\[[\\w.$, ]*\\], httpStatus=(unknown|\\d{3}), elapsedMs=\\d+)"),
                    "Diagnostics must contain only exception types, status and elapsed time");
        }
    }

    @Test void realSpringAiSendsConstrainedRequestAndReadsJsonWithoutReasoningLeak() throws Exception {
        var gateway = start(200, "{\"memories\":[]}", 0, Duration.ofSeconds(5));
        assertEquals("{\"memories\":[]}", call(gateway));
        assertEquals(1, calls.get());
        assertEquals("/v1/chat/completions", lastPath.get());
        assertEquals("fictional", lastRequest.get().get("model").getAsString());
        verify(auth).check(any(), isNull(), anyList());
        assertNoPrivateDiagnostics();
    }

    @Test void jsonModeAlsoWorksForValidatorPromptWithoutJsonKeyword() throws Exception {
        var gateway = start(200, "{\"decisions\":[]}", 0, Duration.ofSeconds(5));
        requireJsonInstruction = true;
        String instructions = "Validate each indexed memory. Return ONLY {\"decisions\":[]}.";
        assertFalse(instructions.toLowerCase(Locale.ROOT).contains("json"));
        String result = gateway.generate(UUID.randomUUID(), null, instructions, CHAT, List.of(
                new SourceView(UUID.randomUUID(), "CHATGPT", "c:1", "test", "user", CHAT, "{}", null)));
        assertEquals("{\"decisions\":[]}", result);
        assertTrue(lastRequest.get().getAsJsonArray("messages").get(0).getAsJsonObject()
                .get("content").getAsString().endsWith("Return only a JSON object."));
        assertNoPrivateDiagnostics();
    }

    @Test void actualTimeoutFailsClosedWithSafeCauseTypes() throws Exception {
        var gateway = start(200, RESPONSE, 1000, Duration.ofMillis(100));
        var ex = assertThrows(ModelFailure.class, () -> call(gateway));
        assertEquals(ModelFailure.Code.MODEL_UNAVAILABLE, ex.code());
        assertNull(ex.getCause());
        String diagnostic = logs.list.getLast().getFormattedMessage();
        assertTrue(diagnostic.contains("httpStatus=unknown"));
        assertTrue(diagnostic.contains("java.io.InterruptedIOException")
                || diagnostic.contains("java.net.SocketTimeoutException"));
        assertNoPrivateDiagnostics();
    }

    @Test void providerErrorsAreSanitizedAndHttpStatusIsRetained() throws Exception {
        var gateway = start(500, RESPONSE, 0, Duration.ofSeconds(5));
        var ex = assertThrows(ModelFailure.class, () -> call(gateway));
        assertEquals("MODEL_UNAVAILABLE", ex.getMessage());
        assertNull(ex.getCause());
        assertEquals(1, calls.get());
        assertTrue(logs.list.getLast().getFormattedMessage().contains("httpStatus=500"));
        assertNoPrivateDiagnostics();
    }

    @Test void malformedSuccessfulResponseLogsOnlyTypesAndHttp200() throws Exception {
        var gateway = start(200, RESPONSE, 0, Duration.ofSeconds(5));
        rawResponse = "{\"choices\":[" + RESPONSE + " " + REASONING;
        var ex = assertThrows(ModelFailure.class, () -> call(gateway));
        assertEquals(ModelFailure.Code.MODEL_UNAVAILABLE, ex.code());
        assertNull(ex.getCause());
        assertTrue(logs.list.getLast().getFormattedMessage().contains("httpStatus=200"));
        assertNoPrivateDiagnostics();
    }

    @Test void rejectedOutputPreservesSafeModelFailureBoundary() throws Exception {
        var gateway = start(200, RESPONSE.repeat(5000), 0, Duration.ofSeconds(5));
        var ex = assertThrows(ModelFailure.class, () -> call(gateway));
        assertEquals(ModelFailure.Code.INVALID_MODEL_OUTPUT, ex.code());
        assertNull(ex.getCause());
        assertTrue(logs.list.getLast().getFormattedMessage().contains("exceptionType=com.mirrorself.model.ModelFailure"));
        assertNoPrivateDiagnostics();
    }

    @Test void authorizationStillRunsBeforeAnyNetworkCall() throws Exception {
        var gateway = start(200, RESPONSE, 0, Duration.ofSeconds(5));
        doThrow(new ModelFailure(ModelFailure.Code.EGRESS_DENIED)).when(auth).check(any(), any(), anyList());
        var ex = assertThrows(ModelFailure.class, () -> call(gateway));
        assertEquals(ModelFailure.Code.EGRESS_DENIED, ex.code());
        assertEquals(0, calls.get());
        assertTrue(logs.list.isEmpty());
    }

    @Test void httpStatusDoesNotLeakIntoLaterConnectionFailures() throws Exception {
        var gateway = start(500, RESPONSE, 0, Duration.ofSeconds(5));
        assertThrows(ModelFailure.class, () -> call(gateway));
        server.stop(0);
        assertThrows(ModelFailure.class, () -> call(gateway));
        assertTrue(logs.list.getLast().getFormattedMessage().contains("httpStatus=unknown"));
        assertNoPrivateDiagnostics();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ExtractionProperties.class)
    static class BindingConfiguration { }

    @Test void environmentVariablesBindThroughYamlIntoActualSpringAiRequest() throws Exception {
        start(200, "{\"memories\":[]}", 0, Duration.ofSeconds(5));
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/compatible/v1";
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(BindingConfiguration.class)
                .withPropertyValues("MIRROR_EXTRACTION_MODE=LLM", "MIRROR_LLM_LOCAL=true",
                        "MIRROR_LLM_BASE_URL=" + baseUrl, "MIRROR_LLM_MODEL=deepseek-flash",
                        "MIRROR_LLM_API_KEY=" + API_KEY)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    var properties = context.getBean(ExtractionProperties.class);
                    assertEquals(baseUrl, properties.baseUrl());
                    assertEquals("deepseek-flash", properties.model());
                    assertEquals(API_KEY, properties.apiKey());
                    assertEquals("{\"memories\":[]}", call(new SpringAiModelGateway(properties, auth)));
                    assertEquals("/compatible/v1/chat/completions", lastPath.get());
                    assertEquals("deepseek-flash", lastRequest.get().get("model").getAsString());
                });
        assertNoPrivateDiagnostics();
    }
}
