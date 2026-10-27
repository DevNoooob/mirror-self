package com.mirrorself.evaluation;

import com.mirrorself.memory.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.mirrorself.evaluation.EvaluationError.*;

class MemoryScorerTest {
    private final MemoryScorer scorer=new MemoryScorer();
    static final String TEXT="我正在学习 Java 后端开发。";
    static EvaluationDataset.Gold gold(String id,String content,MemoryType type,AssertionOrigin origin,
            ExtractedMemory.TemporalScope scope,MemoryStatus status) {
        return new EvaluationDataset.Gold(id,type,origin,content,List.of(content),List.of(),List.of(),
                List.of(new EvaluationDataset.Evidence("u1","user",TEXT,0,TEXT.length())),
                EvaluationDataset.EvidencePolicy.ANY,scope,Sensitivity.NORMAL,status);
    }
    static EvaluationDataset.Gold gold() {return gold("g1",TEXT,MemoryType.FACT,AssertionOrigin.USER_EXPLICIT,
            ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE);}
    static EvaluationDataset.Case sample(List<EvaluationDataset.Gold> gold,List<EvaluationDataset.Forbidden> forbidden) {
        return new EvaluationDataset.Case("TEST001",List.of("facts"),List.of(
                new EvaluationDataset.Message("u1","user",TEXT,Instant.parse("2026-01-01T00:00:00Z"),null),
                new EvaluationDataset.Message("a1","assistant","建议你学习 Rust。",null,"u1")),"a1",gold,forbidden,
                EvaluationDataset.Egress.NORMAL_GRANT,false,false,2);
    }
    static EvaluationObservation.Prediction prediction(String content,MemoryType type,AssertionOrigin origin,
            ExtractedMemory.TemporalScope scope,MemoryStatus status,String mid,String role,String quote) {
        return new EvaluationObservation.Prediction(type,content,origin,Sensitivity.NORMAL,scope,status,
                List.of(new EvaluationObservation.Evidence(mid,role,quote)));
    }
    static EvaluationObservation.Prediction prediction(String text) {return prediction(text,MemoryType.FACT,
            AssertionOrigin.USER_EXPLICIT,ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE,"u1","user",TEXT);}
    static EvaluationObservation observation(List<EvaluationObservation.Prediction> predictions) {
        return new EvaluationObservation(Set.of("u1","a1"),predictions,predictions,List.of(),List.of(),1,1,2,0,0,10);
    }
    @Test void computesIndependentTpFpFnWithOneToOneMatching() {
        var second=gold("g2","我还学习数据库。",MemoryType.FACT,AssertionOrigin.USER_EXPLICIT,
                ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE);
        var wrong=prediction("凭空的旅行经历",MemoryType.FACT,AssertionOrigin.USER_EXPLICIT,
                ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE,"missing","user","不存在");
        var result=scorer.score(sample(List.of(gold(),second),List.of()),observation(List.of(prediction(TEXT),wrong)));
        var counts=result.persisted().counts();
        assertEquals(1,counts.tp());assertEquals(1,counts.fp());assertEquals(1,counts.fn());
        assertEquals(0.5,result.persisted().metrics().get("precision").value());
        assertEquals(0.5,result.persisted().metrics().get("recall").value());
        assertEquals(2,result.persisted().metrics().get("f1").numerator());
        assertEquals(4,result.persisted().metrics().get("f1").denominator());
    }
    @Test void duplicateDoesNotInflateRecall() {
        var result=scorer.score(sample(List.of(gold()),List.of()),observation(List.of(prediction(TEXT),prediction(TEXT))));
        assertEquals(1,result.extracted().counts().tp());assertEquals(1,result.extracted().counts().fp());
        assertEquals(1.0,result.extracted().metrics().get("recall").value());
        assertTrue(result.findings().stream().anyMatch(f->f.kind()==DUPLICATE_MEMORY));
    }
    @Test void curatedParaphraseMatchesAndUnknownParaphraseWaitsForHuman() {
        var g=gold();var alias=new EvaluationDataset.Gold(g.memoryId(),g.type(),g.origin(),g.semantic(),
                List.of(TEXT,"目前主攻Java服务端"),List.of(),List.of(),g.expectedEvidence(),g.evidencePolicy(),
                g.expectedTemporalScope(),g.sensitivity(),g.expectedReviewStatus());
        var known=scorer.score(sample(List.of(alias),List.of()),observation(List.of(prediction("目前主攻Java服务端"))));
        assertEquals(1,known.persisted().counts().tp());
        var unknown=scorer.score(sample(List.of(g),List.of()),observation(List.of(prediction("正在探索服务器应用编程"))));
        assertEquals(0,unknown.persisted().counts().fp());assertEquals(1,unknown.persisted().counts().pendingReview());
        assertEquals(1,unknown.persisted().counts().fn());
        assertTrue(unknown.findings().stream().anyMatch(f->f.kind()==PENDING_HUMAN_REVIEW));
    }
    @Test void conceptMatchingHonorsExclusions() {
        var g=gold();var concepts=new EvaluationDataset.Gold(g.memoryId(),g.type(),g.origin(),g.semantic(),
                g.acceptedContents(),List.of(List.of("Java"),List.of("学习","钻研")),List.of("不学习"),g.expectedEvidence(),
                g.evidencePolicy(),g.expectedTemporalScope(),g.sensitivity(),g.expectedReviewStatus());
        assertTrue(MemoryScorer.semanticallyMatches("目前钻研 Java",concepts));
        assertFalse(MemoryScorer.semanticallyMatches("我不学习 Java",concepts));
    }
    @Test void maximumMatchingAvoidsGreedyLoss() {
        var g=gold();var flexible=new EvaluationDataset.Gold("g1",g.type(),g.origin(),g.semantic(),List.of(TEXT,"alias"),
                List.of(),List.of(),g.expectedEvidence(),g.evidencePolicy(),g.expectedTemporalScope(),g.sensitivity(),g.expectedReviewStatus());
        var strict=gold("g2",TEXT,MemoryType.FACT,AssertionOrigin.USER_EXPLICIT,
                ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE);
        var result=scorer.score(sample(List.of(flexible,strict),List.of()),observation(List.of(prediction(TEXT),prediction("alias"))));
        assertEquals(2,result.persisted().counts().tp());
    }
    @Test void emptyGoldAndPredictionAreUndefinedMetricsNotPerfectScore() {
        var result=scorer.score(sample(List.of(),List.of()),observation(List.of()));
        assertTrue(result.correctEmptyResult());assertNull(result.persisted().metrics().get("precision").value());
        assertNull(result.persisted().metrics().get("recall").value());assertNull(result.persisted().metrics().get("f1").value());
        var missing=scorer.score(sample(List.of(gold()),List.of()),observation(List.of()));
        assertEquals(1,missing.persisted().counts().fn());assertEquals(0.0,missing.persisted().metrics().get("f1").value());
    }
    @Test void assistantAttributionAndUnanchoredEvidenceCannotBeTruePositive() {
        var wrong=prediction(TEXT,MemoryType.FACT,AssertionOrigin.USER_EXPLICIT,
                ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE,"a1","assistant","建议你学习 Rust。");
        var result=scorer.score(sample(List.of(gold()),List.of()),observation(List.of(wrong)));
        assertEquals(0,result.persisted().counts().tp());assertEquals(1,result.persisted().counts().attributionErrors());
        assertEquals(1,result.persisted().counts().unsupported());
    }
    @Test void sourceRoleIsAuthoritativeEvenIfCandidateFalsifiesRole() {
        var wrong=prediction(TEXT,MemoryType.FACT,AssertionOrigin.USER_EXPLICIT,
                ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE,"a1","user","建议你学习 Rust。");
        var result=scorer.score(sample(List.of(gold()),List.of()),observation(List.of(wrong)));
        assertEquals(1,result.persisted().counts().attributionErrors());
    }
    @Test void knownWrongTemporalScopeIsConfirmedError() {
        var wrong=prediction(TEXT,MemoryType.FACT,AssertionOrigin.USER_EXPLICIT,
                ExtractedMemory.TemporalScope.HISTORICAL,MemoryStatus.ACTIVE,"u1","user",TEXT);
        var result=scorer.score(sample(List.of(gold()),List.of()),observation(List.of(wrong)));
        assertEquals(0,result.persisted().counts().tp());assertEquals(1,result.persisted().counts().fp());
        assertEquals(0.0,result.persisted().metrics().get("temporalAccuracy").value());
    }
    @Test void reviewPolicyIsScoredSeparatelyFromSemanticMatch() {
        var g=gold("g1",TEXT,MemoryType.THOUGHT_PROCESS,AssertionOrigin.MODEL_INFERRED,
                ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.PENDING_REVIEW);
        var wrong=prediction(TEXT,MemoryType.THOUGHT_PROCESS,AssertionOrigin.MODEL_INFERRED,
                ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE,"u1","user",TEXT);
        var result=scorer.score(sample(List.of(g),List.of()),observation(List.of(wrong)));
        assertEquals(1,result.persisted().counts().tp());
        assertEquals(0.0,result.persisted().metrics().get("reviewPolicyAccuracy").value());
        assertEquals(1,scorer.aggregate(List.of(result)).privacyViolationRate().numerator());
    }
    @Test void sensitiveActiveMemoryAndUnauthorizedCallArePolicyViolations() {
        var p=new EvaluationObservation.Prediction(MemoryType.FACT,TEXT,AssertionOrigin.USER_EXPLICIT,Sensitivity.SENSITIVE,
                ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE,List.of(new EvaluationObservation.Evidence("u1","user",TEXT)));
        var normal=observation(List.of(p));
        var observation=new EvaluationObservation(normal.selectedMessageIds(),normal.extracted(),normal.persisted(),List.of(),List.of(),
                1,0,1,1,0,10);
        var result=scorer.score(sample(List.of(gold()),List.of()),observation);
        assertTrue(result.findings().stream().anyMatch(f->f.kind()==SENSITIVE_DATA_VIOLATION));
        assertEquals(0,scorer.aggregate(List.of(result)).deduplicationAccuracy().numerator());
        assertEquals(1,scorer.aggregate(List.of(result)).unauthorizedCalls());
    }
    @Test void formatAndValidatorFailuresAreDistinctAndDoNotExposeExceptionText() {
        var o=new EvaluationObservation(Set.of("u1","a1"),List.of(),List.of(),
                List.of(new EvaluationObservation.Rejection(ExtractionObserver.Stage.EVIDENCE_VALIDATION,0)),
                List.of(new EvaluationObservation.Failure(ExtractionObserver.Stage.EXTRACTION,"INVALID_MODEL_OUTPUT"),
                        new EvaluationObservation.Failure(ExtractionObserver.Stage.SEMANTIC_VALIDATION,"INVALID_MODEL_OUTPUT")),0,0,1,0,0,1);
        var result=scorer.score(sample(List.of(),List.of()),o);
        assertFalse(result.correctEmptyResult());
        assertTrue(result.findings().stream().anyMatch(f->f.kind()==MODEL_FORMAT_ERROR));
        assertTrue(result.findings().stream().anyMatch(f->f.kind()==VALIDATOR_FORMAT_ERROR));
        assertEquals(1,result.rejectedByStage().get("EVIDENCE_VALIDATION"));
    }
}
