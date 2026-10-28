package com.mirrorself.api;

import com.mirrorself.embedding.*;
import com.mirrorself.evaluation.retrieval.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.annotation.DirtiesContext;

@EnabledIfEnvironmentVariable(named="MIRROR_RUN_LOCAL_RETRIEVAL",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE,properties={
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/mirror_self_evaluation_guard",
        "mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.extraction.mode=RULE","mirror.model-egress-enabled=false",
        "mirror.embedding.mode=LOCAL","mirror.embedding.base-url=http://127.0.0.1:11434/v1","mirror.embedding.api-key=",
        "mirror.embedding.model=bge-m3","mirror.embedding.version=ollama-790764642607","mirror.embedding.timeout=30s"})
@Import(LocalBgeRetrievalIT.Configuration.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class LocalBgeRetrievalIT extends RetrievalQualityTestSupport {
    @TestConfiguration static class Configuration {
        @Bean @Primary CountedEmbeddingProvider counted(LocalEmbeddingProvider local) {return new CountedEmbeddingProvider(local,400);}
    }
    @Test void evaluatesInstalledPinnedLocalBgeWithNewTemporaryDatabase() throws Exception {evaluate(true);}
}
