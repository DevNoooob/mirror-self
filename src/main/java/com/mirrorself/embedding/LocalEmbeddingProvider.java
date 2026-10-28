package com.mirrorself.embedding;

import com.mirrorself.config.EmbeddingProperties;
import java.net.Proxy;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Component;

/** Opt-in OpenAI-compatible local server. No model downloads, cloud path or retries. */
@Component
public class LocalEmbeddingProvider implements EmbeddingProvider {
    private final EmbeddingProperties properties;
    private volatile OpenAiEmbeddingModel client;
    private okhttp3.OkHttpClient transport;
    public LocalEmbeddingProvider(EmbeddingProperties properties) { this.properties=properties; }
    @Override public Model model() { return new Model(properties.model(),properties.version()); }
    @Override public Location location() {
        return properties.mode()==EmbeddingProperties.Mode.LOCAL ? Location.LOCAL : Location.DISABLED;
    }
    @Override public float[] embed(String text) {
        if(location()!=Location.LOCAL) throw new EmbeddingFailure(EmbeddingFailure.Code.DISABLED);
        try {
            var response=client().call(new EmbeddingRequest(List.of(text),null));
            if(response.getResults().size()!=1 || response.getResult().getIndex()!=0)
                throw new EmbeddingFailure(EmbeddingFailure.Code.INVALID_VECTOR);
            return EmbeddingVectors.validate(response.getResult().getOutput());
        } catch(EmbeddingFailure ex) { throw ex; }
        catch(RuntimeException ex) { throw new EmbeddingFailure(EmbeddingFailure.Code.UNAVAILABLE); }
    }
    private synchronized OpenAiEmbeddingModel client() {
        if(client==null) {
            // Spring AI 2.0.1's customizer does not expose redirect policy. Supply the
            // local transport through its interceptor seam, without following redirects.
            transport=new okhttp3.OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
                    .proxy(Proxy.NO_PROXY).retryOnConnectionFailure(false).callTimeout(properties.timeout())
                    .connectTimeout(properties.timeout()).readTimeout(properties.timeout()).build();
            client=OpenAiEmbeddingModel.builder()
                .httpClientBuilderCustomizer(b->b.proxy(Proxy.NO_PROXY)
                    .interceptor(chain->transport.newCall(chain.request()).execute()))
                .options(OpenAiEmbeddingOptions.builder().baseUrl(properties.baseUrl())
                    .apiKey(properties.apiKey().isBlank()?"local":properties.apiKey())
                    .model(properties.model()).timeout(properties.timeout()).maxRetries(0)
                    .encodingFormat(OpenAiEmbeddingOptions.EncodingFormat.FLOAT).build()).build();
        }
        return client;
    }
    @jakarta.annotation.PreDestroy void close() {
        if(transport!=null) { transport.dispatcher().executorService().shutdown();transport.connectionPool().evictAll(); }
    }
}
