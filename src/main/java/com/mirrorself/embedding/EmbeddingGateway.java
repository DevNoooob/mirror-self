package com.mirrorself.embedding;

import org.springframework.stereotype.Service;

/** Privacy boundary for BOTH memory text and user queries. Cloud remains fail-closed in M1.
 * A future cloud implementation must check all contributing L0 source grants through
 * ModelAuthorization/PrivacyGateway, including query consent, BEFORE invoking a provider. */
@Service
public class EmbeddingGateway {
    private final EmbeddingProvider provider;
    public EmbeddingGateway(EmbeddingProvider provider) { this.provider=provider; }
    public EmbeddingProvider.Model model() { return provider.model(); }
    public void check() {
        if(provider.location()==EmbeddingProvider.Location.DISABLED)
            throw new EmbeddingFailure(EmbeddingFailure.Code.DISABLED);
        if(provider.location()!=EmbeddingProvider.Location.LOCAL)
            throw new EmbeddingFailure(EmbeddingFailure.Code.EGRESS_DENIED);
    }
    public float[] embed(String text) {
        check();
        try { return EmbeddingVectors.validate(provider.embed(text)); }
        catch(EmbeddingFailure ex) { throw ex; }
        catch(RuntimeException ex) { throw new EmbeddingFailure(EmbeddingFailure.Code.UNAVAILABLE); }
    }
}
