package com.mirrorself.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("mirror.extraction")
public record ExtractionProperties(@DefaultValue("RULE") Mode mode,
        @DefaultValue("https://api.openai.com/v1") String baseUrl,
        @DefaultValue("") String apiKey, @DefaultValue("") String model,
        @DefaultValue("30s") Duration timeout,
        @DefaultValue("16000") int batchChars, @DefaultValue("30") int batchMessages,
        @DefaultValue("false") boolean local) {
    public enum Mode { RULE, LLM }
    public ExtractionProperties {
        if (batchChars < 1000 || batchChars > 100000 || batchMessages < 2 || batchMessages > 100)
            throw new IllegalArgumentException("invalid extraction batch limits");
        if (timeout == null || timeout.isNegative() || timeout.isZero() || timeout.toSeconds() > 120)
            throw new IllegalArgumentException("extraction timeout must be 1-120 seconds");
        URI uri = URI.create(baseUrl);
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("invalid model endpoint");
        if (local) {
            if (!java.util.Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost()))
                throw new IllegalArgumentException("local models must use a loopback endpoint");
            if (!java.util.Set.of("http", "https").contains(uri.getScheme()))
                throw new IllegalArgumentException("invalid local model scheme");
        } else if (!"https".equals(uri.getScheme())) {
            throw new IllegalArgumentException("cloud model endpoint requires HTTPS");
        }
        if (mode == Mode.LLM && (model == null || !model.matches("[a-zA-Z0-9._:/-]{1,100}")))
            throw new IllegalArgumentException("set MIRROR_LLM_MODEL for LLM extraction");
    }
    // Never let configuration binding diagnostics accidentally print the API key.
    @Override public String toString() { return "ExtractionProperties[mode=" + mode + ", credentials=REDACTED]"; }
}
