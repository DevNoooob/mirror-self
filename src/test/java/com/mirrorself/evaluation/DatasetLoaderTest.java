package com.mirrorself.evaluation;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DatasetLoaderTest {
    @TempDir Path temp;
    final Path dataset=Path.of("evaluation/datasets/memory-quality-v1.json");
    @Test void fictionalCorpusHasStableMultiTurnCasesAndExactEvidence() throws Exception {
        var loaded=new DatasetLoader().load(dataset);var d=loaded.dataset();
        assertEquals(66,d.cases().size());assertTrue(d.fictional());
        assertEquals("PENDING_HUMAN_REVIEW",d.humanReviewStatus());assertTrue(loaded.sha256().matches("[a-f0-9]{64}"));
        var categories=d.cases().stream().flatMap(c->c.categories().stream()).collect(java.util.stream.Collectors.toSet());
        assertTrue(categories.size()>=20);
        assertTrue(d.cases().stream().anyMatch(c->c.expectedMemories().isEmpty()));
        assertTrue(d.cases().stream().allMatch(c->c.messages().size()>=2));
    }
    @Test void publishedSchemaMemoryTypesMatchTheProductionEnum() throws Exception {
        var schema=JsonParser.parseString(Files.readString(Path.of("evaluation/dataset.schema.json"))).getAsJsonObject();
        Set<String> declared=new HashSet<>();
        schema.getAsJsonObject("$defs").getAsJsonObject("gold").getAsJsonObject("properties")
                .getAsJsonObject("type").getAsJsonArray("enum").forEach(e->declared.add(e.getAsString()));
        assertEquals(Arrays.stream(com.mirrorself.memory.MemoryType.values()).map(Enum::name)
                .collect(java.util.stream.Collectors.toSet()),declared);
    }
    @Test void malformedOrPrivateContractFailsWithoutEchoingInput() throws Exception {
        Path bad=temp.resolve("bad.json");Files.writeString(bad,"PRIVATE_SENTINEL not json");
        var failure=assertThrows(IllegalArgumentException.class,()->new DatasetLoader().load(bad));
        assertEquals("invalid evaluation dataset contract",failure.getMessage());assertNull(failure.getCause());
    }
    @Test void refusesWrongOffsetsUnknownEnumsMissingEvidenceAndCycles() throws Exception {
        JsonObject root=JsonParser.parseString(Files.readString(dataset)).getAsJsonObject();
        var first=root.getAsJsonArray("cases").get(0).getAsJsonObject();
        var evidence=first.getAsJsonArray("expectedMemories").get(0).getAsJsonObject().getAsJsonArray("expectedEvidence").get(0).getAsJsonObject();
        evidence.addProperty("start",1);reject(root);
        root=JsonParser.parseString(Files.readString(dataset)).getAsJsonObject();first=root.getAsJsonArray("cases").get(0).getAsJsonObject();
        first.getAsJsonArray("expectedMemories").get(0).getAsJsonObject().addProperty("type","MADE_UP_TYPE");reject(root);
        root=JsonParser.parseString(Files.readString(dataset)).getAsJsonObject();first=root.getAsJsonArray("cases").get(0).getAsJsonObject();
        var user=first.getAsJsonArray("messages").get(0).getAsJsonObject();user.addProperty("parentId",user.get("messageId").getAsString());reject(root);
    }
    @Test void duplicateCaseIdsAndNonFictionalDatasetAreRejected() throws Exception {
        JsonObject root=JsonParser.parseString(Files.readString(dataset)).getAsJsonObject();
        root.getAsJsonArray("cases").add(root.getAsJsonArray("cases").get(0).deepCopy());reject(root);
        root=JsonParser.parseString(Files.readString(dataset)).getAsJsonObject();root.addProperty("fictional",false);reject(root);
    }
    private void reject(JsonObject tree) throws Exception {
        Path path=temp.resolve("invalid.json");Files.writeString(path,tree.toString());
        assertThrows(IllegalArgumentException.class,()->new DatasetLoader().load(path));
    }
}
