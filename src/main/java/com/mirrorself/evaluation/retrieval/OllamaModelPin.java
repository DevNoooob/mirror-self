package com.mirrorself.evaluation.retrieval;

import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

/** Only metadata GET on a fixed local endpoint; never pulls models or calls cloud. */
public final class OllamaModelPin {
    private OllamaModelPin() { }
    public static void verify(RetrievalCorpus.Plan plan) {verify(plan.model(),plan.modelDigest());}
    public static void verify(String model,String modelDigest) {
        try {
            var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER)
                    .proxy(new java.net.ProxySelector() {
                        public java.util.List<java.net.Proxy> select(URI u) {return java.util.List.of(java.net.Proxy.NO_PROXY);}
                        public void connectFailed(URI u,java.net.SocketAddress a,java.io.IOException e) { }
                    }).build();
            var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:11434/api/tags"))
                    .timeout(Duration.ofSeconds(5)).GET().build(),HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200 || response.body().length()>100000) throw new IllegalStateException();
            boolean found=false;
            for(var element:JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("models")) {
                var m=element.getAsJsonObject();
                if((model+":latest").equals(m.get("name").getAsString()) && modelDigest.equals(m.get("digest").getAsString())) found=true;
            }
            if(!found) throw new IllegalStateException();
        } catch(Exception ex) {throw new IllegalStateException("local Ollama model digest preflight failed");}
    }
}
