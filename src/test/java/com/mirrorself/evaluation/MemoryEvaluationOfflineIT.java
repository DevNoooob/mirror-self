package com.mirrorself.evaluation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties={"mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.extraction.mode=LLM",
        "mirror.extraction.local=false","mirror.model-egress-enabled=true","mirror.extraction.model=offline-fixture-v1",
        "mirror.extraction.base-url=https://evaluation.invalid/v1","mirror.extraction.api-key=","server.port=0"})
@Import(EvaluationIntegrationSupport.OfflineConfiguration.class)
class MemoryEvaluationOfflineIT extends EvaluationIntegrationSupport {
    @org.springframework.beans.factory.annotation.Autowired com.mirrorself.ingestion.ChatGptImportService importer;
    @org.springframework.beans.factory.annotation.Autowired com.mirrorself.memory.ExtractionJobRepository jobs;
    @Test void importsGrantsExtractsPersistsAndRetrievesEvidenceWithoutChangingOriginalSources() throws Exception {
        var c=new DatasetLoader().load(java.nio.file.Path.of("evaluation/datasets/memory-quality-v1.json")).dataset().cases().getFirst();
        var owner=java.util.UUID.randomUUID();
        byte[] file=MemoryEvaluationService.conversation(c).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int before=gateway.calls();
        var imported=importer.load(owner,"fictional-evaluation.json",new java.io.ByteArrayInputStream(file));
        assertEquals(c.messages().size(),imported.importedSources());assertEquals(0,imported.newMemories());
        assertEquals("BLOCKED",jobs.get(owner,imported.extractionJobIds().getFirst()).status());
        assertEquals(before,gateway.calls(),"Import must not bypass source authorization");
        var sourceIds=sources.list(owner,100,0).stream().map(com.mirrorself.ingestion.SourceRepository.SourceSummary::id).toList();
        var originals=sourceIds.stream().map(id->sources.get(owner,id)).toList();
        var grant=authorization.grant(owner,sourceIds,java.time.Instant.now().plusSeconds(600),false);
        var job=extraction.extract(owner,sourceIds,grant);
        assertEquals("COMPLETE",job.status());assertEquals(1,job.newMemories());
        var retrieved=memories.search(owner,"星海",10);assertEquals(1,retrieved.size());
        assertEquals(com.mirrorself.memory.MemoryStatus.ACTIVE,retrieved.getFirst().status());
        assertEquals("user",retrieved.getFirst().evidence().getFirst().role());
        assertTrue(retrieved.getFirst().evidence().getFirst().locator().startsWith("MQ001:"));
        assertEquals(originals,sourceIds.stream().map(id->sources.get(owner,id)).toList());
        assertEquals(0,extraction.extract(owner,sourceIds,grant).newMemories());
        var repeated=importer.load(owner,"fictional-evaluation.json",new java.io.ByteArrayInputStream(file));
        assertEquals(0,repeated.importedSources());assertEquals(0,repeated.newMemories());
        assertEquals(1,memories.list(owner,null,100).size());
    }

    @Test void evaluatesAllFixedMockCasesThroughTheRealPipeline() throws Exception {
        var report=evaluate(MemoryEvaluationService.Mode.OFFLINE_MOCK);
        assertTrue(report.overall().caseCount()>=50);
        assertEquals(0,report.overall().unauthorizedCalls());
        assertTrue(report.run().modelCalls()>0);
        assertTrue(report.overall().errorCounts().containsKey(EvaluationError.MODEL_FORMAT_ERROR));
        assertTrue(report.overall().errorCounts().containsKey(EvaluationError.WRONG_ATTRIBUTION));
        assertTrue(report.overall().errorCounts().containsKey(EvaluationError.REVIEW_POLICY_VIOLATION));
        assertTrue(report.cases().stream().anyMatch(c->c.extracted().counts().tp()>c.persisted().counts().tp()));
        assertEquals(1.0,report.overall().deduplicationAccuracy().value());
        assertTrue(report.overall().correctEmptyCases()>0);
        assertNull(report.run().inputTokens());
    }
}
