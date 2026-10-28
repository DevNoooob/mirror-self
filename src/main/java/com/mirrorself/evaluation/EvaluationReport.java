package com.mirrorself.evaluation;

import java.time.Instant;
import java.util.*;

public record EvaluationReport(String reportVersion, String metricVersion, Run run,
        Aggregate overall, Map<String, Aggregate> categories, List<CaseResult> cases,
        BaselineComparison comparison, List<String> optimizationPriorities,
        ReplaySummary replaySummary, List<ReplayCase> replayDiagnostics) {
    public EvaluationReport {
        replayDiagnostics=replayDiagnostics==null?List.of():List.copyOf(replayDiagnostics);
    }
    public EvaluationReport(String reportVersion,String metricVersion,Run run,Aggregate overall,
            Map<String,Aggregate> categories,List<CaseResult> cases,BaselineComparison comparison,List<String> priorities) {
        this(reportVersion,metricVersion,run,overall,categories,cases,comparison,priorities,null,List.of());
    }
    public record ReplaySummary(String contractVersion,Metric modelOutputConsistency,Metric databaseRowSetIdempotency,
            int replayAttempts,int duplicateRowGroups,int typeDriftAttempts) { }
    public record Round(int round,int extracted,int accepted,int inserted,int rejected,List<String> errorCodes,
            List<String> jobStatuses,Map<String,Integer> rejectedByStage,List<String> memoryIds,int memoryCount) { }
    public record TypeDrift(List<EvidencePointer> evidence,List<com.mirrorself.memory.MemoryType> types,boolean sameProposition) { }
    public record DuplicateGroup(List<String> memoryIds,List<com.mirrorself.memory.MemoryType> types) { }
    public record ReplayAttempt(Round result,List<String> beforeIds,List<String> addedIds,List<String> removedIds,
            boolean idsEqualToFirst,boolean idsEqualToPrevious,Boolean modelOutputConsistent,
            boolean databaseRowSetIdempotent,boolean legacyCheckApplicable,boolean legacyCheckPassed,
            List<TypeDrift> typeDrifts,List<DuplicateGroup> exactDuplicateRows,List<String> reasons) { }
    public record ReplayCase(String caseId,Round first,List<ReplayAttempt> replays) { }
    public record Run(String mode, String datasetVersion, String datasetSha256, String goldProvenance,
            String humanReviewStatus, String model, String extractorVersion, String codeVersion,
            boolean workingTreeDirty, Instant startedAt, long elapsedMs, int modelCalls,
            Long inputTokens, Long outputTokens, String usageAvailability, Integer modelCallLimit,
            int budgetDenied, boolean operatorReviewAttested) { }
    public record Metric(long numerator, long denominator, Double value) {
        public static Metric of(long numerator, long denominator) {
            if (numerator < 0 || denominator < 0 || numerator > denominator) throw new IllegalArgumentException("invalid metric counts");
            return new Metric(numerator, denominator, denominator == 0 ? null : (double) numerator / denominator);
        }
    }
    public record Counts(int tp, int fp, int fn, int pendingReview, int predictions, int gold,
            int validEvidence, int attributionErrors, int temporalCorrect, int temporalAssessed,
            int reviewCorrect, int reviewAssessed, int unsupported, int sensitiveViolations) {
        public Counts plus(Counts b) {
            return new Counts(tp+b.tp,fp+b.fp,fn+b.fn,pendingReview+b.pendingReview,predictions+b.predictions,gold+b.gold,
                    validEvidence+b.validEvidence,attributionErrors+b.attributionErrors,temporalCorrect+b.temporalCorrect,
                    temporalAssessed+b.temporalAssessed,reviewCorrect+b.reviewCorrect,reviewAssessed+b.reviewAssessed,
                    unsupported+b.unsupported,sensitiveViolations+b.sensitiveViolations);
        }
        public static Counts zero() { return new Counts(0,0,0,0,0,0,0,0,0,0,0,0,0,0); }
    }
    public record StageScore(Counts counts, Map<String, Metric> metrics) { }
    public record Finding(String stage, EvaluationError kind, String memoryId, Integer predictionIndex) { }
    public enum CandidateOutcome { CONFIRMED_TP, CONFIRMED_FP, PENDING_HUMAN_REVIEW }
    public record EvidencePointer(String messageId,String role,int start,int end,boolean selected) { }
    /** Safe reference metadata only; no generated content, quotations, prompts or provider responses. */
    public record CandidateAudit(String stage,int index,CandidateOutcome outcome,String referenceMemoryId,
            com.mirrorself.memory.MemoryType type,com.mirrorself.memory.AssertionOrigin origin,
            com.mirrorself.memory.ExtractedMemory.TemporalScope temporalScope,com.mirrorself.memory.MemoryStatus status,
            List<EvidencePointer> evidence,List<EvaluationError> errors) { }
    public record CaseResult(String caseId, List<String> categories, StageScore extracted, StageScore persisted,
            List<Finding> findings, Map<String,Integer> rejectedByStage, int replayChecks, int successfulReplays,
            int modelCalls, int unauthorizedCalls, long elapsedMs, boolean correctEmptyResult,List<CandidateAudit> candidates) { }
    public record Aggregate(StageScore extracted, StageScore persisted, Metric deduplicationAccuracy,
            Metric privacyViolationRate, int unauthorizedCalls, int policyViolations, int correctEmptyCases, int caseCount,
            Map<EvaluationError,Integer> errorCounts) { }
    public record BaselineComparison(boolean comparable, String reason, Map<String, Double> metricDeltas,
            Map<String,Map<String,Double>> categoryDeltas, List<String> newFailures, List<String> resolvedFailures) { }
}
