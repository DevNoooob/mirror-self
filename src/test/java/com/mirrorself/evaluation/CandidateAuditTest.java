package com.mirrorself.evaluation;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CandidateAuditTest {
    @Test void candidateAuditRetainsReferenceAndEvidenceLocationWithoutAnyContent() {
        var c=MemoryScorerTest.sample(List.of(MemoryScorerTest.gold()),List.of());
        var result=new MemoryScorer().score(c,MemoryScorerTest.observation(List.of(MemoryScorerTest.prediction(MemoryScorerTest.TEXT))));
        var audit=result.candidates().getFirst();assertEquals("g1",audit.referenceMemoryId());
        assertEquals(EvaluationReport.CandidateOutcome.CONFIRMED_TP,audit.outcome());
        assertEquals("u1",audit.evidence().getFirst().messageId());assertEquals(0,audit.evidence().getFirst().start());
        assertEquals(MemoryScorerTest.TEXT.length(),audit.evidence().getFirst().end());
        assertFalse(EvaluationJson.GSON.toJson(result).contains(MemoryScorerTest.TEXT));
    }
    @Test void unknownModelReferencesAndTextNeverEnterAudit() {
        var c=MemoryScorerTest.sample(List.of(MemoryScorerTest.gold()),List.of());
        var prediction=MemoryScorerTest.prediction("PRIVATE_RESPONSE_SENTINEL",com.mirrorself.memory.MemoryType.FACT,
                com.mirrorself.memory.AssertionOrigin.USER_EXPLICIT,com.mirrorself.memory.ExtractedMemory.TemporalScope.SOURCE_TIME,
                com.mirrorself.memory.MemoryStatus.ACTIVE,"PRIVATE_SOURCE_SENTINEL","PRIVATE_ROLE_SENTINEL","PRIVATE_QUOTE_SENTINEL");
        var result=new MemoryScorer().score(c,MemoryScorerTest.observation(List.of(prediction)));
        String json=EvaluationJson.GSON.toJson(result);assertFalse(json.contains("PRIVATE_"));
        assertEquals("unknown",result.candidates().getFirst().evidence().getFirst().messageId());
        assertEquals(-1,result.candidates().getFirst().evidence().getFirst().start());
    }
}
