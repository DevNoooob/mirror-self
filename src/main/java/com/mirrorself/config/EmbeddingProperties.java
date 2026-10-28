package com.mirrorself.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("mirror.embedding")
public record EmbeddingProperties(@DefaultValue("DISABLED") Mode mode,
        @DefaultValue("http://127.0.0.1:8090/v1") String baseUrl,
        @DefaultValue("") String apiKey, @DefaultValue("bge-m3") String model,
        @DefaultValue("unconfigured") String version, @DefaultValue("15s") Duration timeout) {
    public enum Mode { DISABLED, LOCAL }
    public EmbeddingProperties {
        URI uri;
        try { uri=URI.create(baseUrl); } catch(RuntimeException ex) { throw new IllegalArgumentException("invalid embedding endpoint"); }
        if (!java.util.Set.of("http","https").contains(uri.getScheme())
                || !java.util.Set.of("localhost","127.0.0.1","[::1]").contains(uri.getHost()==null?"":uri.getHost())
                || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null)
            throw new IllegalArgumentException("embedding endpoint must be local loopback without credentials");
        if (model==null || !model.matches("[a-zA-Z0-9._:/-]{1,100}")
                || version==null || !version.matches("[a-zA-Z0-9._-]{1,80}"))
            throw new IllegalArgumentException("invalid embedding model identity");
        if(mode==Mode.LOCAL && "unconfigured".equals(version))
            throw new IllegalArgumentException("set an immutable embedding model version");
        if(timeout==null || timeout.isZero() || timeout.isNegative() || timeout.compareTo(Duration.ofSeconds(60))>0)
            throw new IllegalArgumentException("embedding timeout must be positive and at most 60 seconds");
    }
    @Override public String toString() { return "EmbeddingProperties[mode="+mode+", credentials=REDACTED]"; }
}
