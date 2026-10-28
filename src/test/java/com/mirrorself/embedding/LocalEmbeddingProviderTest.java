package com.mirrorself.embedding;

import static org.junit.jupiter.api.Assertions.*;
import com.mirrorself.config.EmbeddingProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LocalEmbeddingProviderTest {
    @Test void springAiUsesConfiguredEndpointModelFloatResponseAndNoDimensionTruncation() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var calls=new AtomicInteger();
        server.createContext("/v1/embeddings",exchange->{
            calls.incrementAndGet();
            var json=com.google.gson.JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("fixture-model",json.get("model").getAsString());
            assertEquals("float",json.get("encoding_format").getAsString());
            String vector="1.0,"+"0.0,".repeat(1022)+"0.0";
            byte[] response=("{\"object\":\"list\",\"model\":\"fixture-model\",\"data\":[{\"object\":\"embedding\",\"index\":0,\"embedding\":["+vector+"]}],\"usage\":{\"prompt_tokens\":2,\"total_tokens\":2}}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(200,response.length);
            exchange.getResponseBody().write(response);exchange.close();
        });server.start();
        try {
            var provider=provider(server.getAddress().getPort());assertEquals(1024,provider.embed("fictional sentence").length);
            assertEquals(1,calls.get());
        } finally {server.stop(0);}
    }
    @Test void redirectsCannotSendPrivateInputsToAnotherEndpoint() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var redirected=new AtomicInteger();
        server.createContext("/v1/embeddings",e->{e.getResponseHeaders().add("Location","/capture");e.sendResponseHeaders(307,-1);e.close();});
        server.createContext("/capture",e->{redirected.incrementAndGet();e.sendResponseHeaders(500,-1);e.close();});server.start();
        try { assertThrows(EmbeddingFailure.class,()->provider(server.getAddress().getPort()).embed("fictional private input"));assertEquals(0,redirected.get()); }
        finally {server.stop(0);}
    }
    @Test void malformedProviderResponsesNeverExposeBodiesOrPrivateInputs() throws Exception {
        var context=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var capture=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();capture.start();context.addAppender(capture);
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/embeddings",e->{
            byte[] body="fictional-private-provider-body".getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().add("Content-Type","text/plain");e.sendResponseHeaders(500,body.length);e.getResponseBody().write(body);e.close();
        });server.start();
        try {
            var failure=assertThrows(EmbeddingFailure.class,()->provider(server.getAddress().getPort()).embed("fictional-private-chat"));
            assertEquals("UNAVAILABLE",failure.getMessage());assertNull(failure.getCause());
            for(var event:capture.list) {
                assertFalse(event.getFormattedMessage().contains("fictional-private"));assertNull(event.getThrowableProxy());
            }
        } finally {server.stop(0);context.detachAppender(capture);capture.stop();}
    }
    private LocalEmbeddingProvider provider(int port) {
        return new LocalEmbeddingProvider(new EmbeddingProperties(EmbeddingProperties.Mode.LOCAL,
                "http://127.0.0.1:"+port+"/v1","","fixture-model","v1",Duration.ofSeconds(2)));
    }
}
