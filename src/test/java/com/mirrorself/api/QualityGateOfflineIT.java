package com.mirrorself.api;

import static org.junit.jupiter.api.Assertions.*;
import com.mirrorself.embedding.*;
import com.mirrorself.evaluation.consolidation.*;
import com.mirrorself.evaluation.retrieval.CountedEmbeddingProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.annotation.DirtiesContext;
import org.junit.jupiter.api.Test;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE,properties={"mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.extraction.mode=RULE","mirror.model-egress-enabled=false","mirror.embedding.mode=DISABLED"})
@Import(QualityGateOfflineIT.Configuration.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class QualityGateOfflineIT extends QualityGateTestSupport {
    @TestConfiguration static class Configuration {
        @Bean @Primary CountedEmbeddingProvider qgProvider() {return new CountedEmbeddingProvider(new EmbeddingProvider() {
            public Model model() {return new Model("qg-adversarial-fixture","v1");}public Location location() {return Location.LOCAL;}
            public float[] embed(String text) {float[] v=new float[1024];v[0]=1;return v;}
        },600);}
    }
    @Autowired QualityEvaluation evaluation;@Autowired CountedEmbeddingProvider counted;
    @Test void observesDevelopmentOnlyWithoutTuningOrOpeningHoldout() throws Exception {
        var result=evaluation.run(false,false,counted);assertFalse((Boolean)result.get("holdoutEvaluated"));assertFalse((Boolean)result.get("v03Ready"));
        assertEquals(0,result.get("realModelRequests"));assertEquals(0,counted.failures());assertTrue(counted.calls()<=300);
        // Observational CI checks plumbing and safety; --enforce separately fails quantitative gates.
    }
}
