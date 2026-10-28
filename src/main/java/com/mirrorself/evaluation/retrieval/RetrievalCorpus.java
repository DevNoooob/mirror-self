package com.mirrorself.evaluation.retrieval;

import com.google.gson.*;
import com.mirrorself.common.Hashing;
import com.mirrorself.retrieval.evidence.EvidenceSupportJudge.Relation;
import java.nio.file.*;
import java.util.*;

/** Independently stored inputs/reference, hash-locked before any local model request. */
public final class RetrievalCorpus {
    public record Memory(String id,String owner,String text,String type,String status,String sensitivity,String role,
            String sourcePrivacy,String temporalScope,boolean deleted,String origin,String occurredAt,String sourceText) { }
    public record Query(String id,String owner,String text,String category) { }
    public record Inputs(String version,boolean fictional,List<Memory> memories,List<Query> queries,List<List<String>> contradictions) { }
    public record Relevant(String memoryId,Relation relation,int grade) { }
    public record Answer(String queryId,List<Relevant> relevant,Relation expectedAggregate,String currentRecognition) { }
    public record Reference(String version,String datasetVersion,String annotationStatus,boolean createdBeforeModelExecution,
            String defaultUnlistedRelation,List<Answer> answers) { }
    public record Plan(String datasetPath,String referencePath,String datasetSha256,String referenceSha256,String model,
            String modelVersion,String modelDigest,String endpoint,int queryCount,int memories,int repeats,int maxEmbeddingRequests) { }
    public record Loaded(Inputs inputs,Reference reference,Plan plan) { }
    public static Loaded load() {
        try {
            var gson=new Gson();var plan=gson.fromJson(read(Path.of("evaluation/plans/bge-m3-retrieval-v2.json")),Plan.class);
            String input=read(Path.of(plan.datasetPath())),reference=read(Path.of(plan.referencePath()));
            require(Hashing.sha256(input).equals(plan.datasetSha256()) && Hashing.sha256(reference).equals(plan.referenceSha256()));
            var data=gson.fromJson(input,Inputs.class);var refs=gson.fromJson(reference,Reference.class);
            validate(data,refs,plan);return new Loaded(data,refs,plan);
        } catch(Exception ex) {throw new IllegalArgumentException("invalid or changed local retrieval evaluation contract");}
    }
    public static void validate(Inputs inputs,Reference reference,Plan plan) {
        require(inputs.fictional() && inputs.queries().size()>=30 && inputs.queries().size()==plan.queryCount()
                && inputs.memories().size()==plan.memories() && inputs.version().equals(reference.datasetVersion())
                && reference.createdBeforeModelExecution() && "SYNTHETIC_REFERENCE_PENDING_HUMAN_REVIEW".equals(reference.annotationStatus())
                && "INSUFFICIENT_EVIDENCE".equals(reference.defaultUnlistedRelation()));
        require(plan.repeats()>=1 && plan.repeats()<=5 && plan.maxEmbeddingRequests()>0 && plan.maxEmbeddingRequests()<=400);
        require("bge-m3".equals(plan.model()) && plan.modelVersion().equals("ollama-"+plan.modelDigest().substring(0,12))
                && plan.modelDigest().matches("[a-f0-9]{64}") && "http://127.0.0.1:11434/v1".equals(plan.endpoint()));
        Map<String,Memory> memories=new HashMap<>();Set<String> queries=new HashSet<>(),answers=new HashSet<>();
        for(var m:inputs.memories()) {
            require(memories.put(m.id(),m)==null && Set.of("A","B").contains(m.owner()) && !m.sourceText().isBlank());
            java.time.Instant.parse(m.occurredAt());
            com.mirrorself.memory.MemoryType.valueOf(m.type());com.mirrorself.memory.MemoryStatus.valueOf(m.status());
            com.mirrorself.memory.AssertionOrigin.valueOf(m.origin());com.mirrorself.memory.Sensitivity.valueOf(m.sensitivity());
        }
        for(var q:inputs.queries()) require(queries.add(q.id()) && Set.of("A","B").contains(q.owner()) && !q.text().isBlank() && q.text().length()<=1000);
        require(reference.answers().size()==queries.size());
        for(var a:reference.answers()) {
            require(queries.contains(a.queryId()) && answers.add(a.queryId()) && "UNCONFIRMED".equals(a.currentRecognition()));
            var q=inputs.queries().stream().filter(v->v.id().equals(a.queryId())).findFirst().orElseThrow();Set<String> seen=new HashSet<>();
            for(var ref:a.relevant()) {
                var m=memories.get(ref.memoryId());require(m!=null && seen.add(ref.memoryId()) && m.owner().equals(q.owner())
                        && visible(m) && ref.grade()>=1 && ref.grade()<=3 && ref.relation()!=Relation.INSUFFICIENT_EVIDENCE);
            }
        }
        for(var pair:inputs.contradictions()) require(pair.size()==2 && memories.containsKey(pair.get(0)) && memories.containsKey(pair.get(1)));
    }
    public static boolean visible(Memory m) {
        return !m.deleted() && "ACTIVE".equals(m.status()) && "NORMAL".equals(m.sensitivity())
                && "PRIVATE".equals(m.sourcePrivacy()) && "user".equals(m.role());
    }
    private static String read(Path path) throws java.io.IOException {
        if(Files.size(path)>2_000_000) throw new IllegalArgumentException();
        String text=Files.readString(path);var reader=new com.google.gson.stream.JsonReader(new java.io.StringReader(text));
        reader.setStrictness(com.google.gson.Strictness.STRICT);JsonParser.parseReader(reader);
        if(reader.peek()!=com.google.gson.stream.JsonToken.END_DOCUMENT) throw new IllegalArgumentException();return text;
    }
    private static void require(boolean ok) {if(!ok) throw new IllegalArgumentException();}
}
