package com.mirrorself.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
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
import org.junit.jupiter.api.*;

/** Exercises the actual Spring AI 2.0.1 HTTP adapter against a local simulated provider. */
class SpringAiModelGatewayTest {
    HttpServer server; ExecutorService executor;
    final ModelAuthorization auth=mock(ModelAuthorization.class);
    final AtomicInteger calls=new AtomicInteger();
    @AfterEach void stop() { if(server!=null) server.stop(0);if(executor!=null) executor.shutdownNow(); }
    SpringAiModelGateway start(int status,String content,long delay,Duration timeout) throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        executor=Executors.newVirtualThreadPerTaskExecutor();server.setExecutor(executor);
        server.createContext("/v1/chat/completions",exchange->{
            calls.incrementAndGet();
            var request=JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("none",request.get("tool_choice").getAsString());assertFalse(request.get("store").getAsBoolean());
            assertFalse(request.has("tools"));assertEquals("system",request.getAsJsonArray("messages").get(0).getAsJsonObject().get("role").getAsString());
            try { Thread.sleep(delay); } catch(InterruptedException ex) { Thread.currentThread().interrupt(); }
            JsonObject message=new JsonObject();message.addProperty("role","assistant");message.addProperty("content",content);
            JsonObject choice=new JsonObject();choice.addProperty("index",0);choice.add("message",message);choice.addProperty("finish_reason","stop");
            JsonArray choices=new JsonArray();choices.add(choice);
            JsonObject response=new JsonObject();response.addProperty("id","fictional-completion");response.addProperty("object","chat.completion");
            response.addProperty("created",1);response.addProperty("model","fictional");response.add("choices",choices);
            byte[] bytes=(status==200?response.toString():"{\"error\":{\"message\":\"PRIVATE_MODEL_ERROR\",\"type\":\"server_error\"}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        var props=new ExtractionProperties(ExtractionProperties.Mode.LLM,"http://127.0.0.1:"+server.getAddress().getPort()+"/v1",
                "","fictional",timeout,16000,30,true);
        return new SpringAiModelGateway(props,auth);
    }
    String call(SpringAiModelGateway gateway) {
        return gateway.generate(UUID.randomUUID(),null,"Extract only JSON","fictional data",List.of(
                new SourceView(UUID.randomUUID(),"CHATGPT","c:1","test","user","fictional data","{}",null)));
    }
    @Test void realSpringAiSendsConstrainedRequestAndReadsJson() throws Exception {
        var gateway=start(200,"{\"memories\":[]}",0,Duration.ofSeconds(5));
        assertEquals("{\"memories\":[]}",call(gateway));assertEquals(1,calls.get());
        verify(auth).check(any(),isNull(),anyList());
    }
    @Test void actualTimeoutFailsClosedWithoutLeakingProviderContent() throws Exception {
        var gateway=start(200,"PRIVATE_CONTENT",1000,Duration.ofMillis(100));
        var ex=assertThrows(ModelFailure.class,()->call(gateway));
        assertEquals(ModelFailure.Code.MODEL_UNAVAILABLE,ex.code());assertNull(ex.getCause());
        assertFalse(ex.getMessage().contains("PRIVATE"));
    }
    @Test void providerErrorsAreSanitized() throws Exception {
        var gateway=start(500,"PRIVATE_CONTENT",0,Duration.ofSeconds(5));
        var ex=assertThrows(ModelFailure.class,()->call(gateway));
        assertEquals("MODEL_UNAVAILABLE",ex.getMessage());assertNull(ex.getCause());
        assertEquals(1,calls.get());
    }
}
