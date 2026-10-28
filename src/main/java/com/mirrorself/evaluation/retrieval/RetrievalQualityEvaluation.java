package com.mirrorself.evaluation.retrieval;

import com.google.gson.*;
import com.mirrorself.common.Hashing;
import com.mirrorself.embedding.*;
import com.mirrorself.evaluation.*;
import com.mirrorself.ingestion.*;
import com.mirrorself.memory.*;
import com.mirrorself.retrieval.*;
import com.mirrorself.retrieval.evidence.*;
import com.mirrorself.retrieval.evidence.EvidenceSupportJudge.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

/** Runs against an EMPTY dedicated Testcontainers database only. No application endpoint. */
public class RetrievalQualityEvaluation {
    private final JdbcTemplate jdbc;
    private final MemoryService memoryService;
    private final MemoryRepository memories;
    private final SourceRepository sources;
    private final EmbeddingService indexing;
    private final RetrievalService retrieval;
    private final EvidenceSupportJudge judge;
    private final CountedEmbeddingProvider provider;
    public RetrievalQualityEvaluation(JdbcTemplate jdbc,MemoryService memoryService,MemoryRepository memories,
            SourceRepository sources,EmbeddingService indexing,RetrievalService retrieval,EvidenceSupportJudge judge,CountedEmbeddingProvider provider) {
        this.jdbc=jdbc;this.memoryService=memoryService;this.memories=memories;this.sources=sources;
        this.indexing=indexing;this.retrieval=retrieval;this.judge=judge;this.provider=provider;
    }
    public Map<String,Object> run(RetrievalCorpus.Loaded corpus,boolean real) throws Exception {
        EvaluationSafety.requireIsolatedDatabase(jdbc);
        if(jdbc.queryForObject("SELECT count(*) FROM memory_item",Integer.class)!=0
                || jdbc.queryForObject("SELECT count(*) FROM source_record",Integer.class)!=0)
            throw new IllegalStateException("local retrieval evaluation requires a new empty database");
        var plan=corpus.plan();
        if(real && (!"true".equals(System.getenv("MIRROR_RUN_LOCAL_RETRIEVAL"))
                || !plan.datasetSha256().equals(System.getenv("MIRROR_APPROVED_RETRIEVAL_DATASET_SHA"))
                || !plan.referenceSha256().equals(System.getenv("MIRROR_APPROVED_RETRIEVAL_REFERENCE_SHA"))
                || !provider.model().id().equals(plan.model()) || !provider.model().version().equals(plan.modelVersion())
                || provider.location()!=EmbeddingProvider.Location.LOCAL))
            throw new IllegalStateException("local model execution contract not approved");
        if(real) OllamaModelPin.verify(plan);
        Map<String,UUID> owners=Map.of("A",uuid("owner-A"),"B",uuid("owner-B"));
        Map<String,UUID> ids=new HashMap<>(),sourceIds=new HashMap<>();Map<UUID,String> labels=new HashMap<>();
        Map<String,RetrievalCorpus.Memory> fixture=new HashMap<>();
        for(var m:corpus.inputs().memories()) {
            UUID owner=owners.get(m.owner()),id=uuid(m.id());
            var raw=new JsonObject();raw.addProperty("role",m.role());raw.addProperty("text",m.sourceText());
            var source=sources.save(owner,new SourceInput("CHATGPT","retrieval-v2:"+m.id(),"fictional retrieval v2",m.role(),
                    raw.toString(),m.sourceText(),Instant.parse(m.occurredAt())));
            jdbc.update("""
                INSERT INTO memory_item(id,owner_id,memory_type,content,assertion_origin,status,sensitivity,dedupe_key,temporal_scope,occurred_at)
                VALUES(?,?,?,?,?,?,?,?,?,?::timestamptz)
                """,id,owner,m.type(),m.text(),m.origin(),m.status(),m.sensitivity(),Hashing.sha256("v2:"+m.id()),m.temporalScope(),m.occurredAt());
            jdbc.update("INSERT INTO memory_evidence(memory_id,source_id,excerpt) VALUES(?,?,?)",id,source.id(),m.sourceText());
            jdbc.update("UPDATE source_record SET sensitivity=? WHERE id=?",m.sourcePrivacy(),source.id());
            ids.put(m.id(),id);sourceIds.put(m.id(),source.id());labels.put(id,m.id());fixture.put(m.id(),m);
        }
        for(var pair:corpus.inputs().contradictions()) jdbc.update("""
            INSERT INTO memory_relation(owner_id,from_memory_id,to_memory_id,relation_type) VALUES(?,?,?,'CONTRADICTS')
            """,owners.get(fixture.get(pair.get(0)).owner()),ids.get(pair.get(0)),ids.get(pair.get(1)));
        var indexJobs=new ArrayList<EmbeddingService.Job>();
        for(var owner:owners.values()) {
            var job=indexing.run(owner,100);indexJobs.add(job);
            if(!"COMPLETE".equals(job.status())) {
                Path failure=Path.of("target/evaluation/"+(real?"retrieval-bge-m3-v2":"retrieval-hash-v2"));Files.createDirectories(failure);
                Files.writeString(failure.resolve("report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(
                    map("status","INCOMPLETE_INDEXING_FAILURE","formalBaseline",false,"indexJobs",indexJobs,
                        "embeddingRequests",provider.calls(),"providerFailures",provider.failures(),"datasetSha256",plan.datasetSha256(),"referenceSha256",plan.referenceSha256())));
                throw new IllegalStateException("local evaluation indexing failed; quality not measured");
            }
        }
        for(var m:corpus.inputs().memories()) if(m.deleted()) {
            if("R17".equals(m.id())) memoryService.deleteSource(owners.get(m.owner()),sourceIds.get(m.id()));
            else memoryService.deleteMemory(owners.get(m.owner()),ids.get(m.id()));
        }
        Map<String,MemoryRepository.MemoryView> actualMemories=new HashMap<>();
        for(var owner:owners.values()) for(var m:memories.retrievable(owner,new ArrayList<>(ids.values()))) actualMemories.put(labels.get(m.id()),m);
        int afterIndexCalls=provider.calls();var oracle=new ArrayList<Map<String,Object>>();
        int oracleCorrect=0,oracleTotal=0,oracleDirectCorrect=0,oracleDirectTotal=0;
        for(var q:corpus.inputs().queries()) {
            var gold=answer(corpus,q.id());
            for(var ref:gold.relevant()) {
                var decision=judge.assess(q.text(),Objects.requireNonNull(actualMemories.get(ref.memoryId())));
                boolean correct=decision.relation()==ref.relation();oracleTotal++;if(correct) oracleCorrect++;
                if(direct(ref.relation())) {oracleDirectTotal++;if(correct) oracleDirectCorrect++;}
                oracle.add(map("caseId",q.id(),"memoryId",ref.memoryId(),"expected",ref.relation(),"actual",decision));
            }
        }
        var modes=new ArrayList<Map<String,Object>>();int privacyViolations=0,evidenceErrors=0;
        var modelErrors=new ArrayList<Map<String,Object>>();
        for(var mode:RetrievalService.Mode.values()) {
            var recall=new ArrayList<Double>();var rr=new ArrayList<Double>();var ndcg=new ArrayList<Double>();var precision=new ArrayList<Double>();
            var durations=new ArrayList<Long>();var cases=new ArrayList<Map<String,Object>>();
            int empty=0,correctEmpty=0,totalEvidence=0,correctEvidence=0,pairTotal=0,pairCorrect=0,
                    directTotal=0,directCorrect=0,referenceDirect=0,conclusions=0,temporalOverclaims=0,falseDirect=0;
            retrieval.explain(owners.get("A"),corpus.inputs().queries().getFirst().text(),3,mode); // warm-up; counted, not timed
            int before=provider.calls();
            for(var q:corpus.inputs().queries()) {
                UUID owner=owners.get(q.owner());var gold=answer(corpus,q.id());Map<String,Integer> relevance=new LinkedHashMap<>();
                Map<String,Relation> expected=new HashMap<>();
                for(var ref:gold.relevant()) {relevance.put(ref.memoryId(),ref.grade());expected.put(ref.memoryId(),ref.relation());if(direct(ref.relation())) referenceDirect++;}
                RetrievalService.Result result=null;
                for(int repeat=0;repeat<plan.repeats();repeat++) {
                    long start=System.nanoTime();result=retrieval.explain(owner,q.text(),3,mode);durations.add(System.nanoTime()-start);
                    if(mode!=RetrievalService.Mode.KEYWORD && !Set.of("READY","NO_MATCH").contains(result.vectorStatus()))
                        modelErrors.add(map("caseId",q.id(),"mode",mode,"code",result.vectorStatus()));
                }
                var ranked=result.hits().stream().map(h->labels.get(h.memory().id())).toList();
                var score=RetrievalMetrics.score(ranked,relevance,3);
                if(relevance.isEmpty()) {empty++;if(score.correctEmpty()) correctEmpty++;}
                else {recall.add(score.recallAtK());rr.add(score.reciprocalRank());ndcg.add(score.ndcgAtK());precision.add(RetrievalMetrics.precisionAtK(ranked,relevance,3));}
                var decisions=new ArrayList<Decision>();var details=new ArrayList<Map<String,Object>>();
                for(var hit:result.hits()) {
                    String label=labels.get(hit.memory().id());var f=fixture.get(label);
                    if(f==null || !f.owner().equals(q.owner()) || !RetrievalCorpus.visible(f)) privacyViolations++;
                    if(f!=null && !f.temporalScope().equals(hit.memory().temporalScope())) temporalOverclaims++;
                    for(var evidence:hit.memory().evidence()) {
                        totalEvidence++;
                        if(f!=null && sourceIds.get(label).equals(evidence.sourceId()) && "user".equals(evidence.role())
                                && f.sourceText().contains(evidence.excerpt())) correctEvidence++;
                    }
                    var decision=judge.assess(q.text(),hit.memory());decisions.add(decision);
                    Relation wanted=expected.getOrDefault(label,Relation.INSUFFICIENT_EVIDENCE);
                    pairTotal++;if(wanted==decision.relation()) pairCorrect++;
                    if(direct(wanted)) {directTotal++;if(wanted==decision.relation()) directCorrect++;}
                    if(direct(decision.relation()) && wanted!=decision.relation()) falseDirect++;
                    if(!"UNCONFIRMED".equals(decision.currentRecognition())
                            || decision.proposition().requestedTime()==RequestedTime.CURRENT && direct(decision.relation())) temporalOverclaims++;
                    details.add(map("memoryId",label,"expectedRelation",wanted,"decision",decision,"methods",hit.methods(),
                        "cosine",hit.cosineSimilarity(),"keywordRank",hit.keywordRank(),"vectorRank",hit.vectorRank(),"rrfScore",hit.score()));
                }
                var conclusion=judge.conclude(decisions);if(conclusion.relation()==gold.expectedAggregate()) conclusions++;
                var missed=relevance.keySet().stream().filter(id->!ranked.contains(id)).toList();
                var extra=ranked.stream().filter(id->!relevance.containsKey(id)).toList();
                cases.add(map("caseId",q.id(),"category",q.category(),"question",q.text(),"rankedIds",ranked,"expected",gold.relevant(),
                    "retrievalMetrics",score,"precisionAt3",RetrievalMetrics.precisionAtK(ranked,relevance,3),
                    "missedReferenceIds",missed,"extraIds",extra,"evidence",details,"aggregate",conclusion,"expectedAggregate",gold.expectedAggregate()));
            }
            evidenceErrors+=totalEvidence-correctEvidence;
            modes.add(map("mode",mode,"positiveQueries",recall.size(),"emptyQueries",empty,"correctEmpty",correctEmpty,
                "correctEmptyRate",ratio(correctEmpty,empty),"recallAt3",avg(recall),"mrr",avg(rr),"ndcgAt3",avg(ndcg),"precisionAt3",avg(precision),
                "evidenceCorrect",correctEvidence,"evidenceTotal",totalEvidence,"evidenceAccuracy",ratio(correctEvidence,totalEvidence),
                "relationAccuracy",ratio(pairCorrect,pairTotal),"relationCorrect",pairCorrect,"relationTotal",pairTotal,
                "supportsRefutesAccuracy",ratio(directCorrect,directTotal),"directCorrect",directCorrect,"directTotalRetrieved",directTotal,
                "referenceDirectTotal",referenceDirect,"endToEndDirectRecall",ratio(directCorrect,referenceDirect),
                "falseDirectDecisions",falseDirect,"aggregateAccuracy",ratio(conclusions,corpus.inputs().queries().size()),
                "temporalOverclaims",temporalOverclaims,"timingSamples",durations.size(),"p50Ms",percentile(durations,.5),"p95Ms",percentile(durations,.95),
                "embeddingRequestsInMeasuredQueries",provider.calls()-before,"cases",cases));
        }
        if(real) OllamaModelPin.verify(plan);
        var report=map("status",modelErrors.isEmpty()?"COMPLETE":"INCOMPLETE_MODEL_ERRORS","reportVersion","retrieval-quality-2.0","runId",UUID.randomUUID().toString(),
            "condition",real?"REAL_LOCAL_BGE_M3":"OFFLINE_HASH_FIXTURE","formalBaseline",false,
            "datasetVersion",corpus.inputs().version(),"datasetSha256",plan.datasetSha256(),"referenceSha256",plan.referenceSha256(),
            "referenceReviewStatus",corpus.reference().annotationStatus(),"model",provider.model(),"modelDigest",real?plan.modelDigest():null,
            "modelDigestVerifiedBeforeAndAfter",real,"judgeVersion",ConservativeEvidenceJudge.VERSION,
            "codeRevision",System.getenv().getOrDefault("MIRROR_EVALUATION_CODE_REVISION",System.getenv().getOrDefault("GITHUB_SHA","local-working-tree")),
            "java",System.getProperty("java.version"),"wallClockUtcUnverified",Instant.now().toString(),"latencyClock","MONOTONIC_NANOTIME",
            "database",jdbc.queryForObject("SELECT current_database()",String.class),"queryCount",corpus.inputs().queries().size(),"memoryFixtures",fixture.size(),
            "eligibleMemoriesAfterDeletion",actualMemories.size(),"k",3,"rrfConstant",60,"candidateLimit",100,"minimumCosineSimilarity",.35,
            "repeats",plan.repeats(),"indexJobs",indexJobs,"indexingEmbeddingRequests",afterIndexCalls,
            "embeddingRequests",provider.calls(),"realEmbeddingRequests",real?provider.calls():0,"providerFailures",provider.failures(),
            "cloudCalls",0,"privacyViolations",privacyViolations,"evidenceErrors",evidenceErrors,"modelErrors",modelErrors,
            "oracleJudge",map("correct",oracleCorrect,"total",oracleTotal,"accuracy",ratio(oracleCorrect,oracleTotal),
                "directCorrect",oracleDirectCorrect,"directTotal",oracleDirectTotal,"supportsRefutesAccuracy",ratio(oracleDirectCorrect,oracleDirectTotal),"pairs",oracle),
            "modes",modes,"limitations",List.of("Synthetic reference has not had independent human review.",
                "BGE measures relevance; evidence judgement uses bounded abstaining rules, not BGE entailment.",
                "Reference relevance includes refutation; v1 Mock scores are not comparable.",
                "SOURCE_TIME is not present endorsement. Conflicts do not cause automatic memory updates.",
                "Warm retrieval latency includes provider and database; not a throughput benchmark.",
                "Ubuntu wall clock is unverified; no formal baseline freeze. No production database or API deployment."));
        Path output=Path.of("target/evaluation/"+(real?"retrieval-bge-m3-v2":"retrieval-hash-v2"));Files.createDirectories(output);
        Files.writeString(output.resolve("report.json"),new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(report)+"\n");
        Files.writeString(output.resolve("report.md"),markdown(report,modes));
        return report;
    }
    private String markdown(Map<String,Object> report,List<Map<String,Object>> modes) {
        var out=new StringBuilder("# M2-A retrieval quality observation\n\nCondition: "+report.get("condition")+". Formal baseline: false.\n\n")
                .append("Inputs SHA: `").append(report.get("datasetSha256")).append("`\n\nReference SHA: `").append(report.get("referenceSha256")).append("`\n\n")
                .append("| Mode | Recall@3 | MRR | nDCG@3 | Precision@3 | Empty rate | Evidence | S/R accuracy | p50 / p95 ms |\n|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for(var m:modes) out.append(String.format(Locale.ROOT,"| %s | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.4f | %.2f / %.2f |%n",
            m.get("mode"),m.get("recallAt3"),m.get("mrr"),m.get("ndcgAt3"),m.get("precisionAt3"),m.get("correctEmptyRate"),m.get("evidenceAccuracy"),m.get("supportsRefutesAccuracy"),m.get("p50Ms"),m.get("p95Ms")));
        out.append("\nPrecision@3 uses denominator 3, averaged over nonempty-reference queries. Empty queries scored separately. S/R accuracy only counts retrieved annotated direct pairs; end-to-end direct recall and oracle judgement are separate in JSON.\n")
            .append("\nModel requests: ").append(report.get("embeddingRequests")).append(". Privacy violations: ").append(report.get("privacyViolations"))
            .append(". Wall clock unverified; durations are monotonic. Reference is synthetic and pending human review.\n");
        return out.toString();
    }
    private RetrievalCorpus.Answer answer(RetrievalCorpus.Loaded loaded,String id) {
        return loaded.reference().answers().stream().filter(a->a.queryId().equals(id)).findFirst().orElseThrow();
    }
    private static UUID uuid(String key) {return UUID.nameUUIDFromBytes(("m2a-v2:"+key).getBytes(StandardCharsets.UTF_8));}
    private static boolean direct(Relation r) {return r==Relation.SUPPORTS || r==Relation.REFUTES;}
    private static Double ratio(int n,int d) {return d==0?null:(double)n/d;}
    private static double avg(List<Double> values) {return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);}
    private static double percentile(List<Long> values,double p) {
        var sorted=values.stream().sorted().toList();return sorted.get(Math.max(0,(int)Math.ceil(sorted.size()*p)-1))/1e6;
    }
    private static Map<String,Object> map(Object... pairs) {
        var out=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2) out.put((String)pairs[i],pairs[i+1]);return out;
    }
}
