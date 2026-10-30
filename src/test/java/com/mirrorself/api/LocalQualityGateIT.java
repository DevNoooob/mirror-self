package com.mirrorself.api;

import static org.junit.jupiter.api.Assertions.*;
import com.mirrorself.config.EmbeddingProperties;
import com.mirrorself.embedding.*;
import com.mirrorself.evaluation.consolidation.*;
import com.mirrorself.evaluation.retrieval.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.annotation.DirtiesContext;

@EnabledIfEnvironmentVariable(named="MIRROR_RUN_LOCAL_QG",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE,properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:1/qg_guard",
 "mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.extraction.mode=RULE","mirror.model-egress-enabled=false",
 "mirror.embedding.mode=LOCAL","mirror.embedding.base-url=http://127.0.0.1:11434/v1","mirror.embedding.api-key=",
 "mirror.embedding.model=bge-m3","mirror.embedding.version=ollama-790764642607","mirror.embedding.timeout=30s"})
@Import(LocalQualityGateIT.Configuration.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class LocalQualityGateIT extends QualityGateTestSupport {
    @TestConfiguration static class Configuration {
        @Bean @Primary CountedEmbeddingProvider qgProvider(LocalEmbeddingProvider local) {return new CountedEmbeddingProvider(local,600);}
    }
    @Autowired QualityEvaluation evaluation;@Autowired CountedEmbeddingProvider counted;@Autowired EmbeddingProperties properties;
    @Test void evaluatesPinnedLocalModelOnlyWithFreshScopeApproval() throws Exception {
        var loaded=QualityCorpus.load();QualityLiveAuthorization.check(loaded,properties,System.getenv(),QualityLiveAuthorization.currentCode(),System.getProperty("os.name"),Runtime.version().feature());
        OllamaModelPin.verify(loaded.plan().model(),loaded.plan().modelDigest());
        try {var result=evaluation.run(true,true,counted);assertTrue(counted.calls()>0);assertTrue(counted.calls()<=600);assertEquals(0,counted.failures());assertFalse((Boolean)result.get("formalBaseline"));}
        finally {OllamaModelPin.verify(loaded.plan().model(),loaded.plan().modelDigest());}
    }
}
