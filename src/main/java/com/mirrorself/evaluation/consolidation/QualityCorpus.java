package com.mirrorself.evaluation.consolidation;

import com.google.gson.*;
import com.mirrorself.common.Hashing;
import com.mirrorself.consolidation.ConsolidationRelation;
import java.nio.file.*;
import java.util.*;

/** Frozen fictional inputs; expected answers are loaded only by the scorer, never by recall/classification. */
public final class QualityCorpus {
    private QualityCorpus() { }
    public static final String PLAN="evaluation/plans/m2-qg-bge-v1.json";
    public record Source(String key,String owner,String role,String privacy,String text,String quote,String time) { }
    public record Memory(String key,String content,String type,String owner,String status,String sensitivity,String origin,String scope,String time,List<Source> sources) { }
    public record Case(String id,String family,String split,List<Memory> memories) { }
    public record Inputs(String version,boolean fictional,List<Case> cases) { }
    public record Answer(String caseId,ConsolidationRelation relation,String fromKey,String rationale) { }
    public record Reference(String version,String datasetVersion,boolean createdBeforeModelExecution,String annotationStatus,List<Answer> answers) { }
    public record Plan(String datasetPath,String referencePath,String gatePath,String datasetSha256,String referenceSha256,String gateSha256,
        int cases,int devCases,int holdoutCases,int memories,String model,String modelVersion,String modelDigest,String endpoint,int maxEmbeddingRequests) { }
    public record Loaded(Inputs inputs,Plan plan,JsonObject gates,String planSha256) { }
    public static Loaded load() {
        try {
            String planText=read(Path.of(PLAN));var gson=new Gson();var p=gson.fromJson(planText,Plan.class);
            var manifest=JsonParser.parseString(read(Path.of("evaluation/reviews/m2-qg-input-manifest.json"))).getAsJsonObject();
            require(Hashing.sha256(planText).equals(manifest.get("planSha256").getAsString()));
            require(p.datasetPath().equals("evaluation/datasets/consolidation-blind-v1.json")&&p.referencePath().equals("evaluation/references/consolidation-blind-reference-v1.json")
                &&p.gatePath().equals("evaluation/gates/m2-quality-gate-v1.json"));
            String input=read(Path.of(p.datasetPath())),gates=read(Path.of(p.gatePath()));
            require(Hashing.sha256(input).equals(p.datasetSha256())&&Hashing.sha256(read(Path.of(p.referencePath()))).equals(p.referenceSha256())&&Hashing.sha256(gates).equals(p.gateSha256()));
            require(p.model().equals("bge-m3")&&p.modelVersion().equals("ollama-790764642607")&&p.modelDigest().equals("7907646426070047a77226ac3e684fbbe8410524f7b4a74d02837e43f2146bab")
                &&p.endpoint().equals("http://127.0.0.1:11434/v1")&&p.maxEmbeddingRequests()==600);
            var data=gson.fromJson(input,Inputs.class);validate(data,p);var gate=JsonParser.parseString(gates).getAsJsonObject();
            require(gate.get("frozenBeforeEvaluation").getAsBoolean()&&gate.get("candidateK").getAsInt()==10);
            return new Loaded(data,p,gate,Hashing.sha256(planText));
        } catch(Exception ex) {throw new IllegalArgumentException("invalid or changed quality-gate contract");}
    }
    public static Reference references(Loaded loaded) {
        try {
            var refs=new Gson().fromJson(read(Path.of(loaded.plan().referencePath())),Reference.class);
            require(refs.datasetVersion().equals(loaded.inputs().version())&&refs.createdBeforeModelExecution()
                &&refs.annotationStatus().equals("SYNTHETIC_REFERENCE_PENDING_HUMAN_REVIEW")&&refs.answers().size()==120);
            var ids=new HashSet<String>();for(var a:refs.answers()) {
                require(ids.add(a.caseId())&&loaded.inputs().cases().stream().anyMatch(c->c.id().equals(a.caseId())));
                require(a.relation()==ConsolidationRelation.SUPPORTS||a.relation()==ConsolidationRelation.EVOLVED_FROM
                    ?Set.of("left","right").contains(a.fromKey()):a.fromKey()==null);
            }
            return refs;
        } catch(Exception ex) {throw new IllegalArgumentException("invalid independent quality reference");}
    }
    public static void validate(Inputs data,Plan plan) {
        require(data.fictional()&&data.cases().size()==120&&plan.cases()==120&&plan.devCases()==60&&plan.holdoutCases()==60);
        var ids=new HashSet<String>();var dev=new HashSet<String>();var held=new HashSet<String>();int count=0,devCount=0;
        for(var c:data.cases()) {
            require(ids.add(c.id())&&Set.of("DEV","HOLDOUT").contains(c.split())&&c.memories().size()==5);
            if(c.split().equals("DEV")) {dev.add(c.family());devCount++;} else held.add(c.family());
            var keys=new HashSet<String>();var sources=new HashMap<String,Source>();
            for(var m:c.memories()) {
                count++;require(keys.add(m.key())&&Set.of("A","B").contains(m.owner())&&m.content()!=null&&!m.content().isBlank()&&m.content().length()<=1000&&m.sources()!=null);
                com.mirrorself.memory.MemoryType.valueOf(m.type());com.mirrorself.memory.MemoryStatus.valueOf(m.status());
                com.mirrorself.memory.Sensitivity.valueOf(m.sensitivity());com.mirrorself.memory.AssertionOrigin.valueOf(m.origin());
                if(m.time()!=null) java.time.Instant.parse(m.time());
                for(var s:m.sources()) {
                    require(Set.of("A","B").contains(s.owner())&&Set.of("user","assistant").contains(s.role())&&Set.of("PRIVATE","RESTRICTED","SECRET").contains(s.privacy()));
                    require(s.text()!=null&&!s.text().isBlank()&&s.text().length()<=2000&&s.quote()!=null&&s.quote().length()<=2000);
                    if(s.time()!=null) java.time.Instant.parse(s.time());
                    var prior=sources.putIfAbsent(s.owner()+":"+s.key(),s);require(prior==null||prior.equals(s));
                }
            }
            require(keys.equals(Set.of("left","right","d0","d1","d2")));
        }
        require(count==600&&count==plan.memories()&&devCount==60&&Collections.disjoint(dev,held));
    }
    public static String read(Path path) throws java.io.IOException {
        if(Files.size(path)>2_000_000) throw new IllegalArgumentException();String text=Files.readString(path);
        var reader=new com.google.gson.stream.JsonReader(new java.io.StringReader(text));reader.setStrictness(Strictness.STRICT);JsonParser.parseReader(reader);
        require(reader.peek()==com.google.gson.stream.JsonToken.END_DOCUMENT);return text;
    }
    public static void require(boolean ok) {if(!ok) throw new IllegalArgumentException("invalid quality contract");}
}
