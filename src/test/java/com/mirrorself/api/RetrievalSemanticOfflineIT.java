package com.mirrorself.api;

import com.mirrorself.embedding.*;
import com.mirrorself.evaluation.retrieval.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE,properties={
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/mirror_self_evaluation_guard",
        "mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.extraction.mode=RULE","mirror.model-egress-enabled=false","mirror.embedding.mode=DISABLED"})
@Import(RetrievalSemanticOfflineIT.Configuration.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class RetrievalSemanticOfflineIT extends RetrievalQualityTestSupport {
    @TestConfiguration static class Configuration {
        @Bean @Primary CountedEmbeddingProvider fixture() {
            return new CountedEmbeddingProvider(new EmbeddingProvider() {
                public Model model() {return new Model("hashed-unigram","fixture-v2");}public Location location() {return Location.LOCAL;}
                public float[] embed(String text) {
                    // Generic deterministic hashing. Does not read reference labels or query lexicons.
                    var vector=new float[1024];byte[] bytes=text.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8);
                    for(int i=0;i<bytes.length;i++) vector[Math.floorMod(31*bytes[i]+(i+1<bytes.length?bytes[i+1]:0),1024)]++;
                    if(bytes.length==0) vector[0]=1;return vector;
                }
            },400);
        }
    }
    @Test void evaluatesFrozenBilingualCorpusWithActualSqlAndOfflineJudgement() throws Exception {evaluate(false);}
}
