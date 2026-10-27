package com.mirrorself.evaluation;

import com.google.gson.*;
import com.mirrorself.common.Hashing;
import com.mirrorself.memory.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoldV11FinalizationTest {
    static final String SHA="5d8dc6680c41b14bda2e455e827765131a7cdec34a143ba8b75bac790d5a9ac0";
    static final List<String> IDS=List.of("MQ001","MQ019","MQ022","MQ025","MQ032","MQ043","MQ045","MQ047","MQ054");
    JsonObject json(String name) throws Exception {return JsonParser.parseString(Files.readString(Path.of(name))).getAsJsonObject();}
    JsonObject one(JsonObject data,String id) {
        for(var value:data.getAsJsonArray("cases"))if(value.getAsJsonObject().get("caseId").getAsString().equals(id))return value.getAsJsonObject();
        throw new AssertionError("missing case");
    }
    List<String> strings(JsonArray values) {List<String> out=new ArrayList<>();values.forEach(v->out.add(v.getAsString()));return out;}
    @Test void formalDatasetLoadsWithOnlyTheTwoAuthorizedCaseChanges() throws Exception {
        var loaded=new DatasetLoader().load(Path.of("evaluation/datasets/memory-quality-v1.1.json"));
        assertEquals(SHA,loaded.sha256());assertEquals("memory-quality-v1.1",loaded.dataset().datasetVersion());
        assertEquals(66,loaded.dataset().cases().size());assertEquals("PENDING_HUMAN_REVIEW",loaded.dataset().humanReviewStatus());
        var old=json("evaluation/datasets/memory-quality-v1.json");var current=json("evaluation/datasets/memory-quality-v1.1.json");
        assertEquals("MQ022",old.getAsJsonArray("cases").get(21).getAsJsonObject().get("caseId").getAsString());
        assertEquals("MQ032",old.getAsJsonArray("cases").get(31).getAsJsonObject().get("caseId").getAsString());
        var expected=old.deepCopy();expected.addProperty("datasetVersion","memory-quality-v1.1");
        var gold=one(expected,"MQ032").getAsJsonArray("expectedMemories").get(1).getAsJsonObject();
        assertEquals("G2",gold.get("memoryId").getAsString());gold.addProperty("type","FACT");
        var addition=new JsonObject();addition.addProperty("claimId","F2");addition.addProperty("kind","UNSUPPORTED_CLAIM");
        JsonArray phrases=new JsonArray();List.of("我已经决定去虚构的云港工作","我一定会去虚构的云港工作","我已经制定了去虚构的云港工作的计划").forEach(phrases::add);
        addition.add("phrases",phrases);one(expected,"MQ022").getAsJsonArray("forbiddenClaims").add(addition);
        assertEquals(expected,current,"No other case fields or corpus metadata may change");
    }
    @Test void evidenceTimeAndReviewStatusAreUnchangedAcrossEveryCase() throws Exception {
        var loader=new DatasetLoader();var old=loader.load(Path.of("evaluation/datasets/memory-quality-v1.json")).dataset();
        var current=loader.load(Path.of("evaluation/datasets/memory-quality-v1.1.json")).dataset();
        Map<String,EvaluationDataset.Case> previous=new HashMap<>();old.cases().forEach(c->previous.put(c.caseId(),c));
        for(var c:current.cases()) {
            var p=previous.get(c.caseId());assertEquals(p.messages(),c.messages());assertEquals(p.selectedLeaf(),c.selectedLeaf());
            assertEquals(p.expectedMemories().size(),c.expectedMemories().size());
            for(int i=0;i<c.expectedMemories().size();i++) {
                var a=p.expectedMemories().get(i);var b=c.expectedMemories().get(i);
                assertEquals(a.expectedEvidence(),b.expectedEvidence());assertEquals(a.expectedTemporalScope(),b.expectedTemporalScope());
                assertEquals(a.expectedReviewStatus(),b.expectedReviewStatus());assertEquals(a.acceptedContents(),b.acceptedContents());
            }
        }
        var chosen=EvaluationSelection.reviewed(new DatasetLoader.Loaded(current,SHA),String.join(",",IDS),String.join(",",IDS),true);
        assertEquals(9,chosen.dataset().cases().size());assertEquals(9,chosen.dataset().cases().stream().mapToInt(c->c.expectedMemories().size()).sum());
    }
    @Test void humanReviewIsBoundToNineCasesAndExplicitlyGrantsNoLiveOrOperations() throws Exception {
        var r=json("evaluation/reviews/memory-quality-v1.1-human-review.json");
        assertEquals("APPROVED_NINE_CASES_ONLY",r.get("reviewStatus").getAsString());assertEquals(SHA,r.get("datasetSha256").getAsString());
        assertEquals("2026-10-28",r.get("reviewedOn").getAsString());assertEquals(IDS,strings(r.getAsJsonArray("reviewedCaseIds")));
        assertEquals(9,r.get("reviewedCaseCount").getAsInt());assertEquals(66,r.get("corpusCaseCount").getAsInt());
        assertFalse(r.get("corpusHumanReviewComplete").getAsBoolean());
        var unreviewed=strings(r.getAsJsonArray("unreviewedCaseIds"));assertEquals(57,unreviewed.size());
        assertTrue(Collections.disjoint(unreviewed,IDS));Set<String> union=new HashSet<>(unreviewed);union.addAll(IDS);assertEquals(66,union.size());
        var auth=r.getAsJsonObject("authorization");assertTrue(auth.get("goldFinalization").getAsBoolean());
        for(String field:List.of("modelDataEgress","paidModelCalls","formalDeployment","businessDatabaseModification"))assertFalse(auth.get(field).getAsBoolean());
        var plan=json("evaluation/plans/deepseek-flash-nine-v1.1.json");assertEquals(SHA,plan.get("datasetSha256").getAsString());
        assertEquals(IDS,strings(plan.getAsJsonArray("caseIds")));assertEquals("NOT_AUTHORIZED",plan.getAsJsonObject("liveAuthorization").get("status").getAsString());
    }
    @Test void originalGoldOldPlanAndAllBaselinesRemainAvailableAndByteIdentical() throws Exception {
        assertEquals("b5801339e5a182dacbfd1563a67b86e66a4a6520c64d02757d3c986ac756a652",Hashing.sha256(Files.readString(Path.of("evaluation/datasets/memory-quality-v1.json"))));
        Map<String,String> hashes=Map.of("offline-mock-v0.2.1-a.json","952b0723865ad377af4b1d2e1f892bd8ed9a1710044b43bb571830cdb0f9b8eb","offline-mock-v1.json","c1fd304d88b7553fedca21b8188e77063aca0165820b66c8b1ec07d3a34572ab","offline-rule-v1.json","483a2a9cc4b582ed987cfe0a6e2cfe6de030729277b645cc69fa56f3457ea622");
        for(var e:hashes.entrySet())assertEquals(e.getValue(),Hashing.sha256(Files.readString(Path.of("evaluation/baselines",e.getKey()))));
        var plan=json("evaluation/plans/deepseek-flash-nine-v1.json");assertEquals("memory-quality-v1",plan.get("datasetVersion").getAsString());
        assertTrue(plan.get("humanApproval").isJsonNull());
    }
    @Test void differentGoldVersionsCannotProduceModelImprovementDeltas() throws Exception {
        var before=json("evaluation/baselines/offline-mock-v0.2.1-a.json");var after=before.deepCopy();
        // Metadata-only unit fixture; these are not scored v1.1 model outputs.
        after.getAsJsonObject("run").addProperty("datasetVersion","memory-quality-v1.1");
        after.getAsJsonObject("run").addProperty("datasetSha256",SHA);
        var result=new BaselineComparator().compare(EvaluationJson.GSON.fromJson(before,EvaluationReport.class),
                EvaluationJson.GSON.fromJson(after,EvaluationReport.class));
        assertFalse(result.comparable());assertEquals("dataset_hash_mismatch",result.reason());
        assertTrue(result.metricDeltas().isEmpty());assertTrue(result.categoryDeltas().isEmpty());
    }
}
