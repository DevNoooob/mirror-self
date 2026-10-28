package com.mirrorself.evaluation;

import com.google.gson.*;
import com.mirrorself.memory.*;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import com.mirrorself.model.ModelGateway;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.extraction.mode=LLM",
        "mirror.extraction.local=true","mirror.model-egress-enabled=false","mirror.extraction.model=controlled-offline",
        "mirror.extraction.base-url=http://127.0.0.1:9999/v1","mirror.extraction.api-key=","server.port=0"})
@Import(ReplayPipelineIT.Configuration.class)
class ReplayPipelineIT extends EvaluationIntegrationSupport {
    @TestConfiguration(proxyBeanMethods=false) static class Configuration {
        @Bean ControlledModel controlled() {return new ControlledModel();}
        @Bean @Primary EvaluationGateway model(ControlledModel model) {return new EvaluationGateway(model);}
    }
    static final class ControlledModel implements ModelGateway {
        int round;String scenario;
        @Override public String generate(UUID owner,UUID grant,String instructions,String input,List<SourceView> sources) {
            if(instructions.startsWith("Extract")) {
                round++;if(round>1&&scenario.equals("EMPTY"))return "{\"memories\":[]}";
                MemoryType type=round>1&&scenario.equals("TYPE")?MemoryType.KNOWLEDGE:MemoryType.FACT;
                var source=sources.stream().filter(s->s.role().equals("user")).findFirst().orElseThrow();
                var proposal=new ExtractedMemory(type,source.text(),AssertionOrigin.USER_EXPLICIT,Sensitivity.NORMAL,
                    ExtractedMemory.TemporalScope.SOURCE_TIME,List.of(new ExtractedMemory.EvidenceRef(source.id(),source.text())));
                return "{\"memories\":"+new Gson().toJson(List.of(proposal))+"}";
            }
            return "{\"decisions\":[{\"index\":0,\"supported\":"+!(round>1&&scenario.equals("REJECT"))+"}]}";
        }
    }
    @Autowired ControlledModel model;
    EvaluationReport evaluateScenario(String scenario) throws Exception {
        model.round=0;model.scenario=scenario;
        String text="我用虚构的 PRIVATE_REPLAY_PIPELINE_SENTINEL 做笔记。";
        var gold=new EvaluationDataset.Gold("G1",MemoryType.FACT,AssertionOrigin.USER_EXPLICIT,text,List.of(text),List.of(),List.of(),
            List.of(new EvaluationDataset.Evidence("u1","user",text,0,text.length())),EvaluationDataset.EvidencePolicy.ALL,
            ExtractedMemory.TemporalScope.SOURCE_TIME,Sensitivity.NORMAL,MemoryStatus.ACTIVE);
        var c=new EvaluationDataset.Case("REPLAY_NEW001",List.of("offline_replay"),List.of(new EvaluationDataset.Message("u1","user",text,Instant.EPOCH,null)),
            "u1",List.of(gold),List.of(),EvaluationDataset.Egress.NORMAL_GRANT,false,false,2);
        var d=new EvaluationDataset("1.0","offline-new-replay",true,"TEST_FIXTURE","PENDING_HUMAN_REVIEW",List.of(c));
        return new MemoryEvaluationService(sources,memories,extraction,authorization,jdbc,properties,gateway)
            .run(new DatasetLoader.Loaded(d,"a".repeat(64)),MemoryEvaluationService.Mode.OFFLINE_MOCK,false,null,"0123456",false);
    }
    @Test void actualPipelineSeparatesEmptyOutputFromDatabaseIdempotency() throws Exception {
        var r=evaluateScenario("EMPTY");var a=r.replayDiagnostics().getFirst().replays().getFirst();
        assertEquals(1,r.overall().persisted().counts().tp());assertEquals(0,a.result().accepted());assertEquals(0,a.result().inserted());
        assertFalse(a.modelOutputConsistent());assertTrue(a.databaseRowSetIdempotent());assertTrue(a.idsEqualToFirst());
        assertEquals(0.0,r.overall().deduplicationAccuracy().value(),"Historical metric is preserved, not silently improved");
        assertEquals(1.0,r.replaySummary().databaseRowSetIdempotency().value());
    }
    @Test void actualPipelineRecordsCrossTypeConflictWithoutChangingReviewedData() throws Exception {
        var r=evaluateScenario("TYPE");var a=r.replayDiagnostics().getFirst().replays().getFirst();
        assertEquals(List.of("CLASSIFICATION_CONFLICT"),a.result().errorCodes());assertEquals(0,a.result().inserted());
        assertEquals(1,a.result().memoryCount());assertTrue(a.databaseRowSetIdempotent());assertEquals(1,a.typeDrifts().size());
    }
    @Test void actualPipelineRecordsReplayRejectionAndDoesNotSerializePrivateContent() throws Exception {
        var r=evaluateScenario("REJECT");var a=r.replayDiagnostics().getFirst().replays().getFirst();
        assertEquals(1,a.result().rejected());assertEquals(1,a.result().rejectedByStage().get("SEMANTIC_VALIDATION"));
        assertTrue(a.modelOutputConsistent());assertTrue(a.databaseRowSetIdempotent());
        assertFalse(EvaluationJson.GSON.toJson(r).contains("PRIVATE_REPLAY_PIPELINE_SENTINEL"));
        assertFalse(new EvaluationReportWriter().markdown(r).contains("PRIVATE_REPLAY_PIPELINE_SENTINEL"));
    }
}
