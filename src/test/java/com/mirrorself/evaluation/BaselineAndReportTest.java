package com.mirrorself.evaluation;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.mirrorself.evaluation.EvaluationReport.*;

class BaselineAndReportTest {
    @TempDir Path temp;
    private EvaluationReport report(boolean correct,String sha,String metricVersion) {
        var scorer=new MemoryScorer();
        var result=scorer.score(MemoryScorerTest.sample(List.of(MemoryScorerTest.gold()),List.of()),
                MemoryScorerTest.observation(correct?List.of(MemoryScorerTest.prediction(MemoryScorerTest.TEXT)):List.of()));
        var aggregate=scorer.aggregate(List.of(result));
        return new EvaluationReport("1.0",metricVersion,new Run("OFFLINE_MOCK","test-v1",sha,"ENGINEERING_CURATED",
                "PENDING_HUMAN_REVIEW","fixture-v1","llm-v1","0123456",false,Instant.parse("2026-01-01T00:00:00Z"),1,1,null,null,"unavailable",null,0,false),
                aggregate,Map.of("facts",aggregate),List.of(result),
                new BaselineComparison(false,"baseline_not_supplied",Map.of(),Map.of(),List.of(),List.of()),List.of("Inspect missing facts"));
    }
    @Test void comparesMetricAndCategoryDeltasAndFailureSignatures() {
        var before=report(false,"a".repeat(64),MemoryScorer.METRIC_VERSION);var after=report(true,"a".repeat(64),MemoryScorer.METRIC_VERSION);
        var delta=new BaselineComparator().compare(before,after);
        assertTrue(delta.comparable());assertEquals(1.0,delta.metricDeltas().get("persisted.recall"));
        assertEquals(1.0,delta.categoryDeltas().get("facts").get("persisted.recall"));
        assertTrue(delta.newFailures().isEmpty());assertFalse(delta.resolvedFailures().isEmpty());
        var reversed=new BaselineComparator().compare(after,before);assertFalse(reversed.newFailures().isEmpty());
    }
    @Test void incompatibleDatasetsMetricVersionsAndCaseSelectionsDoNotCompare() {
        var before=report(false,"a".repeat(64),MemoryScorer.METRIC_VERSION);
        assertFalse(new BaselineComparator().compare(before,report(true,"b".repeat(64),MemoryScorer.METRIC_VERSION)).comparable());
        assertFalse(new BaselineComparator().compare(before,report(true,"a".repeat(64),"other")).comparable());
        var subset=new EvaluationReport(before.reportVersion(),before.metricVersion(),before.run(),before.overall(),Map.of(),List.of(),before.comparison(),List.of());
        assertEquals("case_selection_mismatch",new BaselineComparator().compare(before,subset).reason());
    }
    @Test void reportRoundTripHasFractionsAndNeverSerializesSourcePredictionOrProviderBody() throws Exception {
        var scorer=new MemoryScorer();String privateText="PRIVATE_SENTINEL Authorization: Bearer FICTIONAL_KEY reasoning_content";
        var c=MemoryScorerTest.sample(List.of(MemoryScorerTest.gold()),List.of());
        var result=scorer.score(c,MemoryScorerTest.observation(List.of(MemoryScorerTest.prediction(privateText))));
        var base=report(true,"a".repeat(64),MemoryScorer.METRIC_VERSION);
        var safe=new EvaluationReport(base.reportVersion(),base.metricVersion(),base.run(),scorer.aggregate(List.of(result)),Map.of(),List.of(result),base.comparison(),List.of());
        var writer=new EvaluationReportWriter();writer.write(safe,temp);
        for(String name:List.of("report.json","report.md")) {
            String output=Files.readString(temp.resolve(name));
            assertFalse(output.contains(privateText));assertFalse(output.contains("PRIVATE_SENTINEL"));
            assertFalse(output.contains(MemoryScorerTest.TEXT));assertFalse(output.contains("FICTIONAL_KEY"));
        }
        assertEquals(safe,writer.read(temp.resolve("report.json")));
        assertTrue(Files.readString(temp.resolve("report.md")).contains("numerator/denominator"));
    }
    @Test void invalidThresholdsCannotSilentlyDisableTheGate() {
        assertThrows(IllegalArgumentException.class,()->new EvaluationThresholds(Map.of("precision",Double.NaN),Map.of(),0,0));
        assertThrows(IllegalArgumentException.class,()->new EvaluationThresholds(Map.of(),Map.of("unsupportedMemoryRate",1.1),0,0));
        assertThrows(IllegalArgumentException.class,()->new EvaluationThresholds(Map.of(),Map.of(),-1,0));
    }
    @Test void thresholdsAreExplicitAndUndefinedMetricsCannotPass() {
        var report=report(false,"a".repeat(64),MemoryScorer.METRIC_VERSION);
        var gate=new EvaluationThresholds(Map.of("precision",0.5),Map.of("unsupportedMemoryRate",0.1),0,0);
        assertTrue(gate.violations(report).contains("minimum:precision"));
        assertTrue(gate.violations(report).contains("maximum:unsupportedMemoryRate"));
    }
}
