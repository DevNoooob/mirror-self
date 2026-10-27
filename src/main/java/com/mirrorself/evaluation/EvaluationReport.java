package com.mirrorself.evaluation;

import java.time.Instant;
import java.util.*;

public record EvaluationReport(String reportVersion, String metricVersion, Run run,
        Aggregate overall, Map<String, Aggregate> categories, List<CaseResult> cases,
        BaselineComparison comparison, List<String> optimizationPriorities) {
    public record Run(String mode, String datasetVersion, String datasetSha256, String goldProvenance,
            String humanReviewStatus, String model, String extractorVersion, String codeVersion,
            boolean workingTreeDirty, Instant startedAt, long elapsedMs, int modelCalls,
            Long inputTokens, Long outputTokens, String usageAvailability) { }
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
    public record CaseResult(String caseId, List<String> categories, StageScore extracted, StageScore persisted,
            List<Finding> findings, Map<String,Integer> rejectedByStage, int replayChecks, int successfulReplays,
            int modelCalls, int unauthorizedCalls, long elapsedMs, boolean correctEmptyResult) { }
    public record Aggregate(StageScore extracted, StageScore persisted, Metric deduplicationAccuracy,
            Metric privacyViolationRate, int unauthorizedCalls, int policyViolations, int correctEmptyCases, int caseCount,
            Map<EvaluationError,Integer> errorCounts) { }
    public record BaselineComparison(boolean comparable, String reason, Map<String, Double> metricDeltas,
            Map<String,Map<String,Double>> categoryDeltas, List<String> newFailures, List<String> resolvedFailures) { }
}
