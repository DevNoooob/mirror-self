package com.mirrorself.evaluation;

import com.mirrorself.memory.*;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.mirrorself.evaluation.EvaluationReport.*;

class ReplayDiagnosticsTest {
    static final UUID SOURCE=UUID.randomUUID(), ROW=UUID.randomUUID();
    static final String TEXT="PRIVATE_REPLAY_SENTINEL fictional source";
    ExtractedMemory candidate(MemoryType type) {return new ExtractedMemory(type,TEXT,AssertionOrigin.USER_EXPLICIT,Sensitivity.NORMAL,
        ExtractedMemory.TemporalScope.SOURCE_TIME,List.of(new ExtractedMemory.EvidenceRef(SOURCE,TEXT)));}
    MemoryRepository.MemoryView row(UUID id,MemoryType type) {return new MemoryRepository.MemoryView(id,type,TEXT,AssertionOrigin.USER_EXPLICIT,
        MemoryStatus.ACTIVE,Sensitivity.NORMAL,Instant.EPOCH,List.of(new MemoryRepository.Evidence(SOURCE,"D001:u1",TEXT,"user",Instant.EPOCH)),
        "SOURCE_TIME","fixture","none",List.of());}
    Round round(int n,int extracted,int accepted,int inserted,int rejected,List<String> errors,List<String> ids) {
        return new Round(n,extracted,accepted,inserted,rejected,errors,List.of(errors.isEmpty()?"COMPLETE":"PARTIAL"),
                rejected==0?Map.of():Map.of("SEMANTIC_VALIDATION",rejected),ids,ids.size());
    }
    ReplayAttempt assess(Round first,Round next,List<ExtractedMemory> a,List<ExtractedMemory> b,List<MemoryRepository.MemoryView> rows,boolean comparable) {
        return ReplayDiagnostics.assess(first,next,first.memoryIds(),a,b,comparable,rows,
            Map.of(SOURCE,new SourceView(SOURCE,"CHATGPT","D001:u1","fixture","user",TEXT,"{}",Instant.EPOCH)),Map.of(SOURCE,"u1"));
    }
    @Test void emptyReplayIsModelInconsistentButDatabaseStableInsteadOfAProvenDuplicate() {
        var ids=List.of(ROW.toString());var first=round(0,1,1,1,0,List.of(),ids);var next=round(1,0,0,0,0,List.of(),ids);
        var a=assess(first,next,List.of(candidate(MemoryType.FACT)),List.of(),List.of(row(ROW,MemoryType.FACT)),true);
        assertFalse(a.modelOutputConsistent());assertTrue(a.databaseRowSetIdempotent());assertFalse(a.legacyCheckPassed());
        assertTrue(a.reasons().contains("EMPTY_REPLAY_OUTPUT"));assertTrue(a.exactDuplicateRows().isEmpty());
        var summary=ReplayDiagnostics.aggregate(List.of(new ReplayCase("D001",first,List.of(a))));
        assertEquals(0.0,summary.modelOutputConsistency().value());assertEquals(1.0,summary.databaseRowSetIdempotency().value());
    }
    @Test void twoEmptyOutputsAreConsistentAndStableButDoNotExerciseAnAcceptedWrite() {
        var first=round(0,0,0,0,0,List.of(),List.of());var next=round(1,0,0,0,0,List.of(),List.of());
        var a=assess(first,next,List.of(),List.of(),List.of(),true);
        assertTrue(a.modelOutputConsistent());assertTrue(a.databaseRowSetIdempotent());assertFalse(a.legacyCheckApplicable());
    }
    @Test void semanticRejectionDoesNotImplyModelInconsistencyOrDuplicateInsertion() {
        var ids=List.of(ROW.toString());var first=round(0,1,1,1,0,List.of(),ids);
        var next=round(1,1,0,0,1,List.of("UNSUPPORTED_CLAIM"),ids);var proposals=List.of(candidate(MemoryType.FACT));
        var a=assess(first,next,proposals,proposals,List.of(row(ROW,MemoryType.FACT)),true);
        assertTrue(a.modelOutputConsistent());assertTrue(a.databaseRowSetIdempotent());
        assertTrue(a.reasons().contains("SEMANTIC_VALIDATION_REJECTED"));assertEquals(1,a.result().rejected());
    }
    @Test void typeDriftIsVisibleEvenWhenPersistenceBlocksItAndKeepsOriginalIds() {
        var ids=List.of(ROW.toString());var first=round(0,1,1,1,0,List.of(),ids);
        var next=round(1,1,0,0,0,List.of("CLASSIFICATION_CONFLICT"),ids);
        var a=assess(first,next,List.of(candidate(MemoryType.FACT)),List.of(candidate(MemoryType.KNOWLEDGE)),List.of(row(ROW,MemoryType.FACT)),true);
        assertFalse(a.modelOutputConsistent());assertTrue(a.databaseRowSetIdempotent());assertTrue(a.typeDrifts().getFirst().sameProposition());
        assertEquals(List.of(MemoryType.FACT,MemoryType.KNOWLEDGE),a.typeDrifts().getFirst().types());
        assertTrue(a.exactDuplicateRows().isEmpty());
    }
    @Test void actualNewCrossTypeRowsAreDistinguishedFromFailureLabels() {
        UUID other=UUID.randomUUID();var ids=List.of(ROW.toString());var all=List.of(ROW.toString(),other.toString()).stream().sorted().toList();
        var first=round(0,1,1,1,0,List.of(),ids);var next=round(1,1,1,1,0,List.of(),all);
        var a=assess(first,next,List.of(candidate(MemoryType.FACT)),List.of(candidate(MemoryType.KNOWLEDGE)),
                List.of(row(ROW,MemoryType.FACT),row(other,MemoryType.KNOWLEDGE)),true);
        assertFalse(a.databaseRowSetIdempotent());assertEquals(List.of(other.toString()),a.addedIds());assertEquals(1,a.exactDuplicateRows().size());
        assertTrue(a.reasons().contains("EXACT_DUPLICATE_ROWS_OBSERVED"));
    }
    @Test void failureHasUnknownOutputConsistencyAndIdReplacementIsNotStable() {
        UUID other=UUID.randomUUID();var first=round(0,1,1,1,0,List.of(),List.of(ROW.toString()));
        var next=round(1,0,0,0,0,List.of("MODEL_UNAVAILABLE"),List.of(other.toString()));
        var a=assess(first,next,List.of(candidate(MemoryType.FACT)),List.of(),List.of(row(other,MemoryType.FACT)),false);
        assertNull(a.modelOutputConsistent());assertFalse(a.databaseRowSetIdempotent());assertEquals(List.of(ROW.toString()),a.removedIds());
    }
    @Test void reportExtensionsContainNoTextQuotesOrContentHashesAndOldReportsStillRead() throws Exception {
        var ids=List.of(ROW.toString());var first=round(0,1,1,1,0,List.of(),ids);
        var a=assess(first,round(1,0,0,0,0,List.of(),ids),List.of(candidate(MemoryType.FACT)),List.of(),List.of(row(ROW,MemoryType.FACT)),true);
        var cases=List.of(new ReplayCase("D001",first,List.of(a)));
        String json=EvaluationJson.GSON.toJson(cases);assertFalse(json.contains(TEXT));assertFalse(json.contains("quote"));assertFalse(json.contains("content"));
        var prior=new EvaluationReportWriter().read(java.nio.file.Path.of("evaluation/runs/deepseek-flash-nine-v1.1-first/report.json"));
        assertEquals(31,prior.run().modelCalls());assertNull(prior.replaySummary());assertTrue(prior.replayDiagnostics().isEmpty());
        assertEquals(8,prior.overall().extracted().counts().tp());
    }
}
