package com.mirrorself.evaluation;

import com.google.gson.*;
import com.google.gson.stream.*;
import com.mirrorself.common.Hashing;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** No model judges or generates its own gold. Invalid annotations fail before any model invocation. */
public final class DatasetLoader {
    public record Loaded(EvaluationDataset dataset, String sha256) { }
    public Loaded load(Path path) throws IOException {
        String input = Files.readString(path);
        if (input.length() > 2_000_000) throw new IllegalArgumentException("dataset too large");
        try {
            JsonReader reader = new JsonReader(new StringReader(input));
            reader.setStrictness(Strictness.STRICT);
            JsonElement tree = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException();
            var dataset = EvaluationJson.GSON.fromJson(tree, EvaluationDataset.class);
            validate(dataset);
            return new Loaded(dataset, Hashing.sha256(input));
        } catch (RuntimeException ex) {
            // Never include source text or parser exception messages.
            throw new IllegalArgumentException("invalid evaluation dataset contract");
        }
    }
    public void validate(EvaluationDataset d) {
        require(d != null && "1.0".equals(d.schemaVersion()) && d.fictional());
        require(id(d.datasetVersion()) && d.goldProvenance() != null && d.humanReviewStatus() != null);
        require(d.cases() != null && !d.cases().isEmpty() && d.cases().size() <= 500);
        Set<String> cases = new HashSet<>();
        for (var c : d.cases()) {
            require(c != null && id(c.caseId()) && cases.add(c.caseId()));
            require(c.categories() != null && !c.categories().isEmpty() && c.categories().stream().allMatch(DatasetLoader::id));
            require(c.egress() != null && (c.egress()!=EvaluationDataset.Egress.NO_GRANT || c.expectBlocked()) && c.repeatExtractions() >= 1 && c.repeatExtractions() <= 3);
            require(c.messages() != null && c.messages().size() >= 2 && c.messages().size() <= 100);
            Map<String, EvaluationDataset.Message> messages = new LinkedHashMap<>();
            for (var m : c.messages()) {
                require(m != null && id(m.messageId()) && messages.putIfAbsent(m.messageId(), m) == null);
                require(Set.of("user", "assistant", "system", "tool").contains(m.role()));
                require(m.text() != null && !m.text().isBlank() && m.text().length() <= 12000);
            }
            require(c.messages().stream().anyMatch(m -> "user".equals(m.role())));
            for (var m : messages.values()) require(m.parentId() == null || messages.containsKey(m.parentId()));
            for (var m : messages.values()) {
                Set<String> visited = new HashSet<>();
                for (var node = m; node != null; node = messages.get(node.parentId())) require(visited.add(node.messageId()));
            }
            require(c.selectedLeaf() == null || messages.containsKey(c.selectedLeaf()));
            Set<String> selected=new HashSet<>();
            if(c.selectedLeaf()==null)selected.addAll(messages.keySet());
            else for(var node=messages.get(c.selectedLeaf());node!=null;node=messages.get(node.parentId()))selected.add(node.messageId());
            require(c.expectedMemories() != null && c.forbiddenClaims() != null);
            Set<String> memories = new HashSet<>();
            for (var g : c.expectedMemories()) {
                require(g != null && id(g.memoryId()) && memories.add(g.memoryId()) && g.type() != null && g.origin() != null);
                require(g.semantic() != null && !g.semantic().isBlank() && g.acceptedContents() != null && !g.acceptedContents().isEmpty());
                require(g.acceptedContents().stream().allMatch(s -> s != null && !s.isBlank()));
                require(g.requiredConceptGroups() != null && g.excludedConcepts() != null);
                require(g.requiredConceptGroups().stream().allMatch(group -> group != null && !group.isEmpty()
                        && group.stream().allMatch(s -> s != null && !s.isBlank())));
                require(g.expectedTemporalScope() != null && g.sensitivity() != null && g.expectedReviewStatus() != null);
                if(g.origin()==com.mirrorself.memory.AssertionOrigin.MODEL_INFERRED
                        ||g.type()==com.mirrorself.memory.MemoryType.THOUGHT_PROCESS
                        ||g.sensitivity()!=com.mirrorself.memory.Sensitivity.NORMAL)
                    require(g.expectedReviewStatus()==com.mirrorself.memory.MemoryStatus.PENDING_REVIEW);
                require(g.evidencePolicy() != null && g.expectedEvidence() != null && !g.expectedEvidence().isEmpty());
                for (var e : g.expectedEvidence()) {
                    var m = messages.get(e.messageId());
                    require(m != null && selected.contains(e.messageId()) && "user".equals(m.role()) && "user".equals(e.role()));
                    require(e.start() >= 0 && e.end() > e.start() && e.end() <= m.text().length());
                    require(m.text().substring(e.start(), e.end()).equals(e.quote()));
                }
            }
            Set<String> forbidden = new HashSet<>();
            for (var f : c.forbiddenClaims()) require(f != null && id(f.claimId()) && forbidden.add(f.claimId())
                    && f.kind() != null && f.phrases() != null && !f.phrases().isEmpty()
                    && f.phrases().stream().allMatch(s -> s != null && !s.isBlank()));
        }
    }
    private static boolean id(String value) { return value != null && value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}"); }
    private static void require(boolean condition) { if (!condition) throw new IllegalArgumentException(); }
}
