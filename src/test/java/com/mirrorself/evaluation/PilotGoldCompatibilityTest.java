package com.mirrorself.evaluation;

import com.mirrorself.ingestion.SourceRepository;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import com.mirrorself.memory.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Characterizes approved code checks, not real LLM behavior or independent human Gold approval. */
class PilotGoldCompatibilityTest {
    private final UUID owner=UUID.randomUUID();
    private EvaluationDataset.Case sample(String id) throws Exception {
        return new DatasetLoader().load(Path.of("evaluation/datasets/memory-quality-v1.json")).dataset().cases().stream()
                .filter(c->c.caseId().equals(id)).findFirst().orElseThrow();
    }
    private SourceView source(String text) {return new SourceView(UUID.randomUUID(),"CHATGPT","fixture:m1","fictional","user",text,"{}",null);}
    private MemoryValidator validator(List<SourceView> batch) {
        var repository=new SourceRepository(null) {
            @Override public SourceView get(UUID requested,UUID id) {
                assertEquals(owner,requested);return batch.stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();
            }
        };
        return new MemoryValidator(repository,(o,g,i,t,s)->{throw new AssertionError("No model calls in Gold compatibility checks");});
    }
    private ExtractedMemory memory(SourceView source,String quote) {
        return new ExtractedMemory(MemoryType.OPINION,quote,AssertionOrigin.USER_EXPLICIT,Sensitivity.NORMAL,
                ExtractedMemory.TemporalScope.SOURCE_TIME,List.of(new ExtractedMemory.EvidenceRef(source.id(),quote)));
    }
    @Test void originalGoldAndOldBaselinesAreNotRewritten() throws Exception {
        var loaded=new DatasetLoader().load(Path.of("evaluation/datasets/memory-quality-v1.json"));
        assertEquals("b5801339e5a182dacbfd1563a67b86e66a4a6520c64d02757d3c986ac756a652",loaded.sha256());
        assertEquals("PENDING_HUMAN_REVIEW",loaded.dataset().humanReviewStatus());
        for(var entry:Map.of("offline-mock-v1.json","c1fd304d88b7553fedca21b8188e77063aca0165820b66c8b1ec07d3a34572ab",
                "offline-rule-v1.json","483a2a9cc4b582ed987cfe0a6e2cfe6de030729277b645cc69fa56f3457ea622").entrySet())
            assertEquals(entry.getValue(),com.mirrorself.common.Hashing.sha256(Files.readString(Path.of("evaluation/baselines",entry.getKey()))));
        var plan=EvaluationJson.GSON.fromJson(Files.readString(Path.of("evaluation/plans/deepseek-flash-nine-v1.json")),com.google.gson.JsonObject.class);
        assertEquals(loaded.sha256(),plan.get("datasetSha256").getAsString());assertTrue(plan.get("humanApproval").isJsonNull());
        assertEquals(MemoryType.KNOWLEDGE,sample("MQ032").expectedMemories().get(1).type());
    }
    @Test void mixedEnglishPeriodAndChineseSentenceSupportBothUnchangedGoldReferences() throws Exception {
        var c=sample("MQ045");var source=source(c.messages().getFirst().text());var validator=validator(List.of(source));
        for(var gold:c.expectedMemories()) {
            var e=gold.expectedEvidence().getFirst();assertEquals(e.quote(),source.text().substring(e.start(),e.end()));
            assertTrue(validator.evidenceValid(owner,memory(source,e.quote()),List.of(source)));
        }
        assertFalse(validator.evidenceValid(owner,memory(source,"I enjoy repetitive tasks"),List.of(source)));
    }
    @Test void decimalsAbbreviationsInitialsAndRemovedNegationAreNotNewBoundaries() {
        for(var row:List.of(new String[]{"金额是3.14元。","14元"},
                new String[]{"I disagree with Dr. I like repetitive tasks.","I like repetitive tasks"},
                new String[]{"This is initial A. 我喜欢重复劳动。","我喜欢重复劳动"},
                new String[]{"我并不喜欢重复性工作。","喜欢重复性工作"})) {
            var source=source(row[0]);assertFalse(validator(List.of(source)).evidenceValid(owner,memory(source,row[1]),List.of(source)));
        }
    }
    @Test void semanticAliasesRemainScorableButCannotBypassLiteralProductionValidation() throws Exception {
        var c=sample("MQ043");var source=source(c.messages().getFirst().text());var gold=c.expectedMemories().getFirst();
        assertTrue(MemoryScorer.semanticallyMatches(gold.acceptedContents().get(1),gold));
        var candidate=new ExtractedMemory(gold.type(),gold.acceptedContents().get(1),gold.origin(),gold.sensitivity(),
                gold.expectedTemporalScope(),List.of(new ExtractedMemory.EvidenceRef(source.id(),gold.expectedEvidence().getFirst().quote())));
        assertFalse(validator(List.of(source)).evidenceValid(owner,candidate,List.of(source)));
    }
    @Test void relativePastStillRequiresSemanticTimeCheckAndWrongFactTypeStillRisksActivation() throws Exception {
        var c=sample("MQ032");var source=source(c.messages().getFirst().text());var g=c.expectedMemories().getFirst();
        var wrongTime=new ExtractedMemory(g.type(),g.acceptedContents().getFirst(),g.origin(),g.sensitivity(),
                ExtractedMemory.TemporalScope.SOURCE_TIME,List.of(new ExtractedMemory.EvidenceRef(source.id(),g.expectedEvidence().getFirst().quote())));
        // Known limitation: the lexical past marker list does not include "两年前"; independent semantic validation is required.
        assertTrue(validator(List.of(source)).evidenceValid(owner,wrongTime,List.of(source)));
        assertEquals(MemoryStatus.PENDING_REVIEW,MemoryPolicy.initialStatus(MemoryType.THOUGHT_PROCESS,AssertionOrigin.USER_EXPLICIT,Sensitivity.NORMAL));
        assertEquals(MemoryStatus.ACTIVE,MemoryPolicy.initialStatus(MemoryType.FACT,AssertionOrigin.USER_EXPLICIT,Sensitivity.NORMAL));
    }
}
