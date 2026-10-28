package com.mirrorself.consolidation;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import com.mirrorself.common.Hashing;
import com.mirrorself.evaluation.RetrievalRelevanceBands;
import com.mirrorself.retrieval.evidence.EvidenceSupportJudge.Relation;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReferenceV3Test {
    @Test void preservesV2AndKeepsV3PendingWithAffirmativeRefutation() throws Exception {
        assertEquals("4b0cde4f771356ea86c2bf70cfab27703c7bf7d06bbebb7acd308482526f2ad4",Hashing.sha256(Files.readString(Path.of("evaluation/datasets/retrieval-fictional-v2.json"))));
        assertEquals("cd1c9933d61460657eaed2247b490ae502bc7fc7dbccbce8a0ffc1e83f1285b2",Hashing.sha256(Files.readString(Path.of("evaluation/references/retrieval-reference-v2.json"))));
        var data=JsonParser.parseString(Files.readString(Path.of("evaluation/datasets/retrieval-fictional-v3.json"))).getAsJsonObject();
        var refs=JsonParser.parseString(Files.readString(Path.of("evaluation/references/retrieval-reference-v3.json"))).getAsJsonObject();
        assertEquals("aedb0a96d46dbcb06e5bee0481a3566855a55330fed842efd334db840035ab50",Hashing.sha256(Files.readString(Path.of("evaluation/datasets/retrieval-fictional-v3.json"))));
        assertEquals("0b6404d8752737a8eb3477f0d08d47b8738c6d874da29644ec014b2aeb2649dd",Hashing.sha256(Files.readString(Path.of("evaluation/references/retrieval-reference-v3.json"))));
        var old=JsonParser.parseString(Files.readString(Path.of("evaluation/references/retrieval-reference-v2.json"))).getAsJsonObject();
        Set<String> backgrounds=new HashSet<>(),kept=new HashSet<>();
        for(var a:old.getAsJsonArray("answers")) for(var ref:a.getAsJsonObject().getAsJsonArray("relevant")) if(ref.getAsJsonObject().get("grade").getAsInt()==1)
            backgrounds.add(a.getAsJsonObject().get("queryId").getAsString()+":"+ref.getAsJsonObject().get("memoryId").getAsString()+":"+ref.getAsJsonObject().get("relation").getAsString());
        for(var a:refs.getAsJsonArray("answers")) for(var ref:a.getAsJsonObject().getAsJsonArray("relevant")) if(ref.getAsJsonObject().get("grade").getAsInt()==1)
            kept.add(a.getAsJsonObject().get("queryId").getAsString()+":"+ref.getAsJsonObject().get("memoryId").getAsString()+":"+ref.getAsJsonObject().get("relation").getAsString());
        assertEquals(backgrounds,kept);
        assertEquals(45,data.getAsJsonArray("queries").size());assertEquals(45,refs.getAsJsonArray("answers").size());
        assertFalse(refs.get("fullHumanReviewComplete").getAsBoolean());assertEquals("SYNTHETIC_REFERENCE_PENDING_HUMAN_REVIEW",refs.get("annotationStatus").getAsString());
        for(var q:data.getAsJsonArray("queries")) if("R2Q13".equals(q.getAsJsonObject().get("id").getAsString())) {
            String text=q.getAsJsonObject().get("text").getAsString();assertTrue(text.contains("2025-01-01"));assertTrue(text.contains("当时我喜欢"));
        }
        for(var a:refs.getAsJsonArray("answers")) if("R2Q13".equals(a.getAsJsonObject().get("queryId").getAsString()))
            assertEquals("REFUTES",a.getAsJsonObject().get("expectedAggregate").getAsString());
    }
    @Test void backgroundsCannotInflatePrimaryRelevanceOrEvidenceSupport() {
        var score=RetrievalRelevanceBands.score(List.of("background","context"),Map.of("direct",3,"context",2,"background",1),
            Map.of("direct",Relation.REFUTES),Map.of("background",Relation.SUPPORTS),3);
        assertEquals(0,score.primary().ranking().recallAtK());assertEquals(1,score.background().ranking().recallAtK());
        assertEquals(1,score.timeContextPendingReview().ranking().recallAtK());assertEquals(0,score.correctDirect());assertEquals(1,score.totalDirect());
    }
}
