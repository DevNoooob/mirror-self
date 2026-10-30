package com.mirrorself.consolidation;

import static org.junit.jupiter.api.Assertions.*;
import com.mirrorself.memory.*;
import com.mirrorself.retrieval.evidence.ConservativeEvidenceJudge;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ConsolidationClassifierTest {
    final ConsolidationClassifier rules=new ConsolidationClassifier(new ConservativeEvidenceJudge());
    MemoryRepository.MemoryView memory(String text,String quote,String time,String scope,MemoryType type,UUID source) {
        var at=time==null?null:Instant.parse(time);return new MemoryRepository.MemoryView(UUID.randomUUID(),type,text,AssertionOrigin.USER_EXPLICIT,
            MemoryStatus.ACTIVE,Sensitivity.NORMAL,at,List.of(new MemoryRepository.Evidence(source,"fictional:unit",quote,"user",at)),scope,"none","none",List.of());
    }
    MemoryRepository.MemoryView plain(String text,String time) {return memory(text,text,time,"SOURCE_TIME",MemoryType.OPINION,UUID.randomUUID());}
    @Test void polarityIsAConflictNotAMerge() {
        var a=plain("我喜欢摄影。","2023-01-01T00:00:00Z");var b=plain("我不喜欢摄影。","2023-01-01T00:00:00Z");
        assertEquals(ConsolidationRelation.CONTRADICTS,rules.classify(a,b).proposal().relation());
    }
    @Test void datedOppositePreferenceOnlySuggestsEvolution() {
        var a=plain("我喜欢摄影。","2021-01-01T00:00:00Z");var b=plain("我不喜欢摄影。","2024-01-01T00:00:00Z");
        var p=rules.classify(a,b).proposal();assertEquals(ConsolidationRelation.EVOLVED_FROM,p.relation());assertEquals("B_TO_A",p.direction());
        assertTrue(p.uncertainties().contains("POSSIBLE_EVOLUTION_OR_MISSING_CONTEXT"));
    }
    @Test void sharedSourceDoesNotEquateDifferentFacts() {
        UUID source=UUID.randomUUID();String quote="我喜欢摄影。我不喝茶。";
        var a=memory("我喜欢摄影。",quote,null,"SOURCE_TIME",MemoryType.OPINION,source);
        var b=memory("我不喝茶。",quote,null,"SOURCE_TIME",MemoryType.OPINION,source);
        assertNull(rules.classify(a,b).proposal());
    }
    @Test void sameClauseMustReallyExistInL0() {
        var a=plain("我喜欢摄影。","2023-01-01T00:00:00Z");
        var bad=memory("我喜欢摄影。","如果需要，我可能喜欢摄影。","2023-01-01T00:00:00Z","SOURCE_TIME",MemoryType.OPINION,UUID.randomUUID());
        assertNull(rules.classify(a,bad).proposal());
    }
    @Test void conditionAndCompletionStayDifferent() {
        var a=memory("如果获得资格，我可能去星湾学习。","如果获得资格，我可能去星湾学习。",null,"SOURCE_TIME",MemoryType.GOAL,UUID.randomUUID());
        var b=memory("我已经完成星湾课程。","我已经完成星湾课程。",null,"SOURCE_TIME",MemoryType.EVENT,UUID.randomUUID());
        assertNull(rules.classify(a,b).proposal());
    }
    @Test void sameSelfReportIsNeverAnObjectivePersonality() {
        var a=memory("我觉得自己很耐心。","我觉得自己很耐心。","2023-01-01T00:00:00Z","SOURCE_TIME",MemoryType.THOUGHT_PROCESS,UUID.randomUUID());
        var b=memory("I consider myself patient.","I consider myself patient.","2023-01-01T00:00:00Z","SOURCE_TIME",MemoryType.THOUGHT_PROCESS,UUID.randomUUID());
        var p=rules.classify(a,b).proposal();assertEquals(ConsolidationRelation.SAME_AS,p.relation());
        assertTrue(p.uncertainties().contains("SELF_REPORT_ONLY_NOT_OBJECTIVE_PERSONALITY"));
    }
}
