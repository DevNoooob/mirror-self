package com.mirrorself.retrieval;

import static org.junit.jupiter.api.Assertions.*;
import com.mirrorself.retrieval.evidence.*;
import com.mirrorself.retrieval.evidence.EvidenceSupportJudge.*;
import com.mirrorself.memory.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class EvidenceSupportJudgeTest {
    final ConservativeEvidenceJudge judge=new ConservativeEvidenceJudge();
    MemoryRepository.MemoryView memory(String content,String quote,MemoryType type,MemoryStatus status,String scope,String role,List<UUID> conflicts) {
        return new MemoryRepository.MemoryView(UUID.randomUUID(),type,content,AssertionOrigin.USER_EXPLICIT,status,Sensitivity.NORMAL,
            Instant.parse("2021-06-01T00:00:00Z"),List.of(new MemoryRepository.Evidence(UUID.randomUUID(),"fictional:u1",quote,role,Instant.parse("2021-06-01T00:00:00Z"))),
            scope,"fictional","none",conflicts);
    }
    MemoryRepository.MemoryView normal(String quote) {return memory(quote,quote,MemoryType.OPINION,MemoryStatus.ACTIVE,"SOURCE_TIME","user",List.of());}
    @Test void affirmativeQuestionIsRefutedByNegativeQuotationAndNegativeQuestionIsSupported() {
        var m=normal("我不喜欢长途飞行。");var no=judge.assess("我喜欢长途飞行吗？",m);
        assertEquals(Relation.REFUTES,no.relation());assertFalse(no.proposition().negative());
        assertEquals(Relation.SUPPORTS,judge.assess("我不喜欢长途飞行吗？",m).relation());
        assertEquals("UNCONFIRMED",no.currentRecognition());assertEquals("SOURCE_TIME_ONLY",no.evidenceTimeScope());
    }
    @Test void englishNegationUsesThePropositionAndIgnoresContraryL1Paraphrase() {
        var m=memory("I drink orange juice.","I do not drink orange juice.",MemoryType.OPINION,MemoryStatus.ACTIVE,"SOURCE_TIME","user",List.of());
        assertEquals(Relation.REFUTES,judge.assess("Do I drink orange juice?",m).relation());
    }
    @Test void currentEndorsementCannotBeInferredFromSourceTime() {
        var d=judge.assess("我现在还喜欢长途飞行吗？",normal("我不喜欢长途飞行。"));
        assertEquals(Relation.RELATED_ONLY,d.relation());assertEquals(Relation.REFUTES,d.sourceTimeRelation());
        assertEquals(Relation.INSUFFICIENT_EVIDENCE,judge.conclude(List.of(d)).relation());
    }
    @Test void historicalYearMustMatchAndDoesNotBecomeCurrent() {
        var m=memory("In 2018 I lived in Amber Town.","In 2018 I lived in Amber Town.",MemoryType.FACT,MemoryStatus.ACTIVE,"HISTORICAL","user",List.of());
        assertEquals(Relation.SUPPORTS,judge.assess("Did I live in Amber Town in 2018?",m).relation());
        assertEquals(Relation.RELATED_ONLY,judge.assess("Did I live in Amber Town in 2019?",m).relation());
        assertEquals(Relation.RELATED_ONLY,judge.assess("Do I still live in Amber Town?",m).relation());
    }
    @Test void assistantPendingAndAbsentEvidenceCannotEstablishUserFacts() {
        var assistant=memory("我喜欢陶艺。","我喜欢陶艺。",MemoryType.OPINION,MemoryStatus.ACTIVE,"SOURCE_TIME","assistant",List.of());
        assertEquals(Relation.INSUFFICIENT_EVIDENCE,judge.assess("我喜欢陶艺吗？",assistant).relation());
        var pending=memory("我喜欢陶艺。","我喜欢陶艺。",MemoryType.OPINION,MemoryStatus.PENDING_REVIEW,"SOURCE_TIME","user",List.of());
        assertEquals(Relation.INSUFFICIENT_EVIDENCE,judge.assess("我喜欢陶艺吗？",pending).relation());
        assertEquals(Relation.INSUFFICIENT_EVIDENCE,judge.conclude(List.of()).relation());
    }
    @Test void conditionalGoalCannotBeConvertedToAnAccomplishment() {
        var m=memory("如果录取，我可能考虑搬去月泉市。","如果录取，我可能考虑搬去月泉市。",MemoryType.GOAL,MemoryStatus.ACTIVE,"SOURCE_TIME","user",List.of());
        assertEquals(Relation.RELATED_ONLY,judge.assess("我已经搬去月泉市了吗？",m).relation());
    }
    @Test void reviewedSelfReportDoesNotProveAnObjectivePersonality() {
        var m=memory("我觉得自己很谨慎。","我觉得自己很谨慎。",MemoryType.THOUGHT_PROCESS,MemoryStatus.ACTIVE,"SOURCE_TIME","user",List.of());
        assertEquals(Relation.SUPPORTS,judge.assess("我是否自认为很谨慎？",m).relation());
        assertEquals(Relation.RELATED_ONLY,judge.assess("我被证明具有谨慎人格了吗？",m).relation());
    }
    @Test void conflictsCannotBeResolvedFromOneRetrievedSide() {
        var yes=judge.assess("我喜欢陶艺吗？",normal("我喜欢陶艺。"));
        var no=judge.assess("我喜欢陶艺吗？",normal("我不喜欢陶艺。"));
        assertEquals(Relation.INSUFFICIENT_EVIDENCE,judge.conclude(List.of(yes,no)).relation());
        var marked=memory("我喜欢陶艺。","我喜欢陶艺。",MemoryType.OPINION,MemoryStatus.ACTIVE,"SOURCE_TIME","user",List.of(UUID.randomUUID()));
        assertEquals(Relation.INSUFFICIENT_EVIDENCE,judge.conclude(List.of(judge.assess("我喜欢陶艺吗？",marked))).relation());
    }
    @Test void unrelatedConflictCannotVetoDirectEvidenceForTheTestedProposition() {
        var direct=judge.assess("我喜欢陶艺吗？",normal("我不喜欢陶艺。"));
        var unrelated=memory("我喜欢摄影。","我喜欢摄影。",MemoryType.OPINION,MemoryStatus.ACTIVE,"SOURCE_TIME","user",List.of(UUID.randomUUID()));
        assertEquals(Relation.REFUTES,judge.conclude(List.of(direct,judge.assess("我喜欢陶艺吗？",unrelated))).relation());
    }
    @Test void longEvidenceAbstainsWithoutThrowingOrTruncatingItsMeaning() {
        var m=normal("我喜欢陶艺。".repeat(200));
        assertEquals(Relation.INSUFFICIENT_EVIDENCE,judge.assess("我喜欢陶艺吗？",m).relation());
    }
    @Test void doubleNegationAndMultiFactQuestionsAbstain() {
        assertFalse(judge.proposition("我并非不喜欢陶艺吗？").resolved());
        var result=judge.assess("我喜欢陶艺和摄影吗？",normal("我喜欢陶艺。"));
        assertNotEquals(Relation.SUPPORTS,result.relation());
    }
}
