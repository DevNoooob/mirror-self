package com.mirrorself.api;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import com.mirrorself.common.Hashing;
import com.mirrorself.embedding.*;
import com.mirrorself.evaluation.RetrievalMetrics;
import com.mirrorself.ingestion.*;
import com.mirrorself.memory.*;
import com.mirrorself.retrieval.RetrievalService;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties={"mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.model-egress-enabled=false"})
@Import(RetrievalEvaluationIT.Configuration.class)
class RetrievalEvaluationIT extends ExtractionPostgresTestSupport {
    static JsonObject dataset() {
        try {return JsonParser.parseString(Files.readString(Path.of("evaluation/datasets/retrieval-fictional-v1.json"))).getAsJsonObject();}
        catch(Exception ex) {throw new IllegalStateException("retrieval fixture unavailable");}
    }
    @TestConfiguration static class Configuration {
        @Bean @Primary EmbeddingProvider fixtureEmbedding() {return new EmbeddingProvider() {
            final JsonArray lexicon=dataset().getAsJsonArray("lexicon");
            public Model model() {return new Model("controlled-lexicon","v1");}
            public Location location() {return Location.LOCAL;}
            public float[] embed(String text) {
                float[] vector=new float[1024];String lower=text.toLowerCase(Locale.ROOT);boolean found=false;
                for(int i=0;i<lexicon.size();i++) for(var word:lexicon.get(i).getAsJsonArray())
                    if(lower.contains(word.getAsString())) {vector[i]=1;found=true;}
                if(!found) vector[100+Math.floorMod(lower.hashCode(),924)]=1;
                return vector;
            }
        };}
    }
    @Autowired MemoryService memories;
    @Autowired SourceRepository sources;
    @Autowired MemoryRepository repository;
    @Autowired EmbeddingService indexing;
    @Autowired RetrievalService retrieval;
    @Autowired JdbcTemplate jdbc;
    @Test void comparesActualKeywordVectorAndHybridQueriesWithEvidenceAndTiming() throws Exception {
        JsonObject data=dataset();UUID owner=UUID.fromString("22222222-2222-4222-8222-222222222222");
        UUID other=UUID.fromString("33333333-3333-4333-8333-333333333333");
        Map<String,UUID> ids=new HashMap<>(),sourceIds=new HashMap<>();Map<UUID,String> labels=new HashMap<>();
        for(var element:data.getAsJsonArray("memories")) {
            var m=element.getAsJsonObject();String label=m.get("id").getAsString(),text=m.get("text").getAsString();
            UUID who="A".equals(m.get("owner").getAsString())?owner:other;
            // Fictional L0 with original roles; never rely on an extractor to manufacture test ranking labels.
            var source=sources.save(who,new SourceInput("CHATGPT","retrieval-v1:"+label,"fictional retrieval",m.get("role").getAsString(),
                    "{\"fixture\":true}",text,Instant.parse("2024-05-01T00:00:00Z")));
            UUID id=UUID.nameUUIDFromBytes(label.getBytes(StandardCharsets.UTF_8));
            jdbc.update("""
                INSERT INTO memory_item(id,owner_id,memory_type,content,assertion_origin,status,sensitivity,dedupe_key,temporal_scope,occurred_at)
                VALUES(?,?,?,?,'USER_EXPLICIT',?,?,?,?,'2024-05-01T00:00:00Z')
                """,id,who,m.get("type").getAsString(),text,m.get("status").getAsString(),m.get("sensitivity").getAsString(),
                    Hashing.sha256(label),m.get("temporalScope").getAsString());
            jdbc.update("INSERT INTO memory_evidence(memory_id,source_id,excerpt) VALUES(?,?,?)",id,source.id(),text);
            jdbc.update("UPDATE source_record SET sensitivity=? WHERE id=?",m.get("sourcePrivacy").getAsString(),source.id());
            ids.put(label,id);labels.put(id,label);sourceIds.put(label,source.id());
        }
        // Generate once, then delete fixtures to exercise removal of previously indexed records.
        assertEquals("COMPLETE",indexing.run(owner,100).status());indexing.run(other,100);
        memories.deleteMemory(owner,ids.get("R16"));memories.deleteSource(owner,sourceIds.get("R17"));
        assertEquals(9,jdbc.queryForObject("SELECT count(*) FROM memory_embedding WHERE owner_id=?",Integer.class,owner));
        List<Map<String,Object>> modes=new ArrayList<>();
        for(var mode:RetrievalService.Mode.values()) {
            List<Map<String,Object>> cases=new ArrayList<>();List<Double> recall=new ArrayList<>(),rr=new ArrayList<>(),ndcg=new ArrayList<>();
            List<Long> nanos=new ArrayList<>();int empty=0,correctEmpty=0,evidenceTotal=0,evidenceCorrect=0;
            retrieval.explain(owner,"bicycle",3,mode); // warm up outside timed sample
            for(var element:data.getAsJsonArray("queries")) {
                var q=element.getAsJsonObject();Map<String,Integer> relevance=new TreeMap<>();
                q.getAsJsonObject("relevance").entrySet().forEach(e->relevance.put(e.getKey(),e.getValue().getAsInt()));
                List<String> actual=null;RetrievalService.Result result=null;
                for(int repeat=0;repeat<5;repeat++) {
                    long start=System.nanoTime();result=retrieval.explain(owner,q.get("text").getAsString(),3,mode);
                    nanos.add(System.nanoTime()-start);
                    var ranked=result.hits().stream().map(h->labels.get(h.memory().id())).toList();
                    if(actual!=null) assertEquals(actual,ranked,"ranking must be deterministic");actual=ranked;
                }
                var score=RetrievalMetrics.score(actual,relevance,3);
                if(score.recallAtK()!=null) {recall.add(score.recallAtK());rr.add(score.reciprocalRank());ndcg.add(score.ndcgAtK());}
                else {empty++;if(score.correctEmpty()) correctEmpty++;}
                for(var hit:result.hits()) for(var evidence:hit.memory().evidence()) {
                    evidenceTotal++;String label=labels.get(hit.memory().id());
                    var original=sources.get(owner,evidence.sourceId());
                    if(sourceIds.get(label).equals(evidence.sourceId()) && "user".equals(evidence.role())
                            && original.text().contains(evidence.excerpt())) evidenceCorrect++;
                    assertEquals(MemoryStatus.ACTIVE,hit.memory().status());assertEquals(Sensitivity.NORMAL,hit.memory().sensitivity());
                }
                var detail=new LinkedHashMap<String,Object>();detail.put("caseId",q.get("id").getAsString());detail.put("coverage",q.get("coverage").getAsString());
                detail.put("rankedIds",actual);detail.put("expectedIds",relevance);detail.put("metrics",score);cases.add(detail);
                if(!q.get("id").getAsString().equals("RQ20") && relevance.isEmpty()) assertTrue(actual.isEmpty(),"visibility/no-match guard "+q.get("id"));
            }
            Collections.sort(nanos);var summary=new LinkedHashMap<String,Object>();summary.put("mode",mode.name());
            summary.put("recallAt3",average(recall));summary.put("mrr",average(rr));summary.put("ndcgAt3",average(ndcg));
            summary.put("positiveQueries",recall.size());summary.put("emptyQueries",empty);summary.put("correctEmptyQueries",correctEmpty);
            summary.put("evidenceCorrect",evidenceCorrect);summary.put("evidenceTotal",evidenceTotal);
            summary.put("evidenceAccuracy",evidenceTotal==0?null:(double)evidenceCorrect/evidenceTotal);
            summary.put("latencySamples",nanos.size());summary.put("p50Ms",nanos.get(nanos.size()/2)/1e6);
            summary.put("p95Ms",nanos.get((int)Math.ceil(nanos.size()*.95)-1)/1e6);summary.put("cases",cases);modes.add(summary);
            assertEquals(evidenceTotal,evidenceCorrect);
        }
        String input=Files.readString(Path.of("evaluation/datasets/retrieval-fictional-v1.json"));
        var report=new LinkedHashMap<String,Object>();report.put("reportVersion","retrieval-offline-1.0");report.put("datasetSha256",Hashing.sha256(input));
        report.put("dataset",data.get("version").getAsString());report.put("fixtureVersion",data.get("fixtureVersion").getAsString());
        report.put("codeRevision",System.getenv().getOrDefault("GITHUB_SHA","local-working-tree"));
        report.put("runtime",System.getProperty("java.version"));report.put("environment",System.getenv().containsKey("GITHUB_ACTIONS")?"GitHub Actions isolated PostgreSQL/pgvector":"isolated Testcontainers");
        report.put("k",3);report.put("candidateLimit",100);report.put("rrfConstant",60);report.put("minimumCosineSimilarity",.35);
        report.put("realModelCalls",0);report.put("corpusRows",18);report.put("eligibleIndexedRows",9);report.put("queryCount",20);
        report.put("limitations",List.of("Controlled lexicon fixture; does not measure a real embedding model.",
            "Negated query RQ20 deliberately exposes relevance errors; evidence correctness does not imply relevance.",
            "Tiny corpus; latency is not a production capacity benchmark. Mac/Ubuntu business DB not accessed."));report.put("modes",modes);
        // Compare only deterministic retrieval quality/IDs. Timing is observational, never a gate.
        var baseline=JsonParser.parseString(Files.readString(Path.of("evaluation/baselines/retrieval-offline-v1.json"))).getAsJsonObject();
        assertEquals(baseline.get("datasetSha256").getAsString(),report.get("datasetSha256"));
        assertEquals("OFFLINE_CONTROLLED_MOCK_ONLY",baseline.get("baselineKind").getAsString());
        var actualModes=new Gson().toJsonTree(modes).getAsJsonArray();
        for(int i=0;i<actualModes.size();i++) {
            var expected=baseline.getAsJsonArray("modes").get(i).getAsJsonObject();var actual=actualModes.get(i).getAsJsonObject();
            for(String key:List.of("mode","recallAt3","mrr","ndcgAt3","positiveQueries","emptyQueries","correctEmptyQueries", "evidenceAccuracy","evidenceTotal","evidenceCorrect"))
                assertEquals(expected.get(key),actual.get(key),"offline quality regression "+key);
            var expectedCases=expected.getAsJsonArray("cases");var actualCases=actual.getAsJsonArray("cases");
            assertEquals(expectedCases.size(),actualCases.size());
            for(int c=0;c<expectedCases.size();c++) {
                assertEquals(expectedCases.get(c).getAsJsonObject().get("caseId"),actualCases.get(c).getAsJsonObject().get("caseId"));
                assertEquals(expectedCases.get(c).getAsJsonObject().get("rankedIds"),actualCases.get(c).getAsJsonObject().get("rankedIds"));
            }
        }
        Path output=Path.of("target/evaluation/retrieval-offline-v1");Files.createDirectories(output);
        Files.writeString(output.resolve("report.json"),new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(report)+"\n");
        StringBuilder markdown=new StringBuilder("# Fictional retrieval offline comparison\n\nControlled lexicon Mock; **zero real embedding calls**.\n\nDataset SHA-256: `"+Hashing.sha256(input)+"`\n\n| Mode | Recall@3 | MRR | nDCG@3 | Evidence | Correct empty | p50 ms | p95 ms |\n|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for(var m:modes) markdown.append(String.format(Locale.ROOT,"| %s | %.4f | %.4f | %.4f | %.4f | %s/%s | %.3f | %.3f |%n",
                m.get("mode"),m.get("recallAt3"),m.get("mrr"),m.get("ndcgAt3"),m.get("evidenceAccuracy"),m.get("correctEmptyQueries"),m.get("emptyQueries"),m.get("p50Ms"),m.get("p95Ms")));
        markdown.append("\nRQ20 is a known false-positive case: query affirmation versus a negative memory. Returned text retains its negation.\nMetrics average the nine positive queries; empty-query accuracy is separate. Evidence accuracy checks actual L0 IDs/roles/exact quotes, not relevance.\n");
        Files.writeString(output.resolve("report.md"),markdown);
    }
    private double average(List<Double> values) {return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);}
}
