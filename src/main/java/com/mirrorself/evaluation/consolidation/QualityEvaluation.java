package com.mirrorself.evaluation.consolidation;

import com.mirrorself.consolidation.*;
import com.mirrorself.embedding.*;
import com.mirrorself.evaluation.EvaluationJson;
import com.mirrorself.evaluation.retrieval.CountedEmbeddingProvider;
import com.mirrorself.ingestion.*;
import com.mirrorself.memory.*;
import com.mirrorself.retrieval.RetrievalRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Isolated evaluation driver. Never create this against business data; requires an empty DB. */
@Service
public class QualityEvaluation {
    private final JdbcTemplate jdbc;private final SourceRepository sources;private final EmbeddingService index;
    private final ConsolidationRecall recall;private final ConsolidationWriter writer;private final ConsolidationRepository repository;
    private final ConsolidationService service;private final RetrievalRepository retrieval;private final org.springframework.transaction.PlatformTransactionManager transactions;
    public QualityEvaluation(JdbcTemplate jdbc,SourceRepository sources,EmbeddingService index,ConsolidationRecall recall,ConsolidationWriter writer,
            ConsolidationRepository repository,ConsolidationService service,RetrievalRepository retrieval,org.springframework.transaction.PlatformTransactionManager transactions) {
        this.jdbc=jdbc;this.sources=sources;this.index=index;this.recall=recall;this.writer=writer;this.repository=repository;this.service=service;this.retrieval=retrieval;this.transactions=transactions;
    }
    private record Fixture(UUID owner,Map<String,UUID> ids,Map<UUID,QualityCorpus.Memory> definitions) { }
    public Map<String,Object> run(boolean real,boolean includeHoldout,CountedEmbeddingProvider provider) throws Exception {
        var loaded=QualityCorpus.load();
        QualityCorpus.require(jdbc.queryForObject("SELECT current_database()",String.class).startsWith("mirror_self_evaluation_qg_")&&jdbc.queryForObject("SELECT count(*) FROM identity_claim",Long.class)==0
            &&jdbc.queryForObject("SELECT count(*) FROM source_record",Long.class)==0&&jdbc.queryForObject("SELECT count(*) FROM memory_item",Long.class)==0);
        var observations=new ArrayList<QualityScorer.Observation>();Fixture probe=null;long started=System.nanoTime();
        for(var c:loaded.inputs().cases()) {
            if(c.split().equals("HOLDOUT")&&!includeHoldout) continue;
            var fixture=seed(c);if(c.family().equals("F06")&&probe==null) probe=fixture;
            for(UUID owner:fixture.definitions().values().stream().map(m->owner(c.id(),m.owner())).distinct().toList()) {
                var job=index.run(owner,100);if(!"COMPLETE".equals(job.status())) throw new IllegalStateException("QG_INDEX_INCOMPLETE");
            }
            for(var mode:ConsolidationRecall.Mode.values()) observations.add(observe(c,fixture,mode,provider.model()));
        }
        // Independent guard probes use only already pinned fictional input, no additional embeddings.
        double conflictAccuracy=reviewProbe(probe);QualityCorpus.require(jdbc.queryForObject("SELECT count(*) FROM identity_claim",Long.class)==0&&jdbc.queryForObject("SELECT count(*) FROM memory_relation",Long.class)==0);
        var refs=QualityCorpus.references(loaded).answers().stream().collect(java.util.stream.Collectors.toMap(QualityCorpus.Answer::caseId,a->a));
        var results=new LinkedHashMap<String,Object>();
        for(String split:includeHoldout?List.of("DEV","HOLDOUT"):List.of("DEV")) for(var mode:ConsolidationRecall.Mode.values()) {
            var rows=observations.stream().filter(o->o.split().equals(split)&&o.mode().equals(mode.name())).toList();results.put(split+"_"+mode,QualityScorer.score(rows,refs,loaded.gates(),conflictAccuracy));
        }
        var report=new LinkedHashMap<String,Object>();report.put("reportVersion","m2-qg-v1");report.put("condition",real?"REAL_LOCAL_BGE_M3":"ADVERSARIAL_IDENTICAL_1024D_FIXTURE");
        report.put("datasetSha256",loaded.plan().datasetSha256());report.put("referenceSha256",loaded.plan().referenceSha256());report.put("gateSha256",loaded.plan().gateSha256());report.put("planSha256",loaded.planSha256());
        report.put("codeRevision",QualityLiveAuthorization.currentCode());report.put("wallClockTrusted",false);report.put("pipelineVersion",ConsolidationWriter.ENGINE_VERSION);report.put("classifierVersion",ConsolidationClassifier.VERSION);report.put("modelId",provider.model().id());report.put("modelVersion",provider.model().version());
        report.put("modelDigest",real?loaded.plan().modelDigest():"NOT_A_REAL_MODEL");report.put("providerRequests",provider.calls());report.put("providerFailures",provider.failures());report.put("realModelRequests",real?provider.calls():0);
        report.put("queryEmbeddingRequests",0);report.put("cloudCalls",0);report.put("holdoutEvaluated",includeHoldout);report.put("humanReferenceReviewComplete",false);report.put("formalBaseline",false);report.put("v03Ready",false);
        report.put("readinessBlockers",real&&includeHoldout?List.of("INDEPENDENT_HUMAN_REVIEW_PENDING","CHECK_QUANTITATIVE_GATES"):List.of("REAL_LOCAL_ACCEPTANCE_PENDING","HOLDOUT_NOT_ACCEPTED","INDEPENDENT_HUMAN_REVIEW_PENDING"));
        report.put("elapsedMsMonotonic",(System.nanoTime()-started)/1_000_000);report.put("results",results);
        String runId=UUID.randomUUID().toString();report.put("runId",runId);
        var output=Path.of("target/evaluation/m2-qg-v1/"+(real?"local":"mock-dev")+(includeHoldout?"-with-holdout":"")+"/"+runId);Files.createDirectories(output);
        // Refuse overwriting an observation in this checkout. Use a fresh isolated checkout for a new approved run.
        Files.writeString(output.resolve("report.json"),EvaluationJson.GSON.toJson(report)+"\n",StandardOpenOption.CREATE_NEW);
        StringBuilder md=new StringBuilder("# M2-QG quality observation\n\nCondition: "+report.get("condition")+"; human reference review pending; formal baseline=false; V0.3 ready=false.\n\n| Split/mode | Candidate R@10 | P | R | F1 | False SAME_AS | Failed gates |\n|---|---:|---:|---:|---:|---:|---|\n");
        results.forEach((key,value)->{var m=(Map<?,?>)value;md.append(String.format(java.util.Locale.ROOT,"| %s | %.4f | %.4f | %.4f | %.4f | %s | %s |%n",key,m.get("candidateRecallAt10"),m.get("precision"),m.get("recall"),m.get("f1"),m.get("falseSameAs"),m.get("failedGates")));});
        md.append("\nEach JSON detail separates CANDIDATE_RECALL from RELATION_CLASSIFICATION, with raw keyword/vector/fused fixture IDs and abstention codes. Vector similarity is not an entailment label.\n");
        Files.writeString(output.resolve("report.md"),md.toString(),StandardOpenOption.CREATE_NEW);
        if(Boolean.getBoolean("mirror.qg.enforce")&&results.values().stream().anyMatch(v->!Boolean.TRUE.equals(((Map<?,?>)v).get("quantitativeGatePass")))) throw new IllegalStateException("QG_QUANTITATIVE_GATE_FAILED");
        return report;
    }
    private Fixture seed(QualityCorpus.Case c) {
        var ids=new HashMap<String,UUID>();var definitions=new HashMap<UUID,QualityCorpus.Memory>();var sourceIds=new HashMap<String,UUID>();
        for(var m:c.memories()) {
            UUID owner=owner(c.id(),m.owner()),id=uuid(c.id()+"/memory/"+m.key());ids.put(m.key(),id);definitions.put(id,m);
            jdbc.update("INSERT INTO memory_item(id,owner_id,memory_type,content,assertion_origin,status,sensitivity,dedupe_key,temporal_scope,occurred_at) VALUES(?,?,?,?,?,?,?,?,?,?::timestamptz)",id,owner,m.type(),m.content(),m.origin(),m.status(),m.sensitivity(),id.toString(),m.scope(),m.time());
            for(var s:m.sources()) {
                String key=s.owner()+":"+s.key();UUID sid=sourceIds.get(key);
                if(sid==null) {
                    sid=sources.save(owner(c.id(),s.owner()),new SourceInput("CHATGPT","quality/"+c.id()+"/"+key,"fictional qg",s.role(),"{}",s.text(),s.time()==null?null:Instant.parse(s.time()))).id();sourceIds.put(key,sid);
                    jdbc.update("UPDATE source_record SET sensitivity=? WHERE id=?",s.privacy(),sid);
                }
                jdbc.update("INSERT INTO memory_evidence(memory_id,source_id,excerpt) VALUES(?,?,?)",id,sid,s.quote());
            }
        }
        return new Fixture(owner(c.id(),"A"),ids,definitions);
    }
    private QualityScorer.Observation observe(QualityCorpus.Case c,Fixture f,ConsolidationRecall.Mode mode,EmbeddingProvider.Model model) {
        var attempts=new ArrayList<QualityScorer.Attempt>();var queries=new ArrayList<QualityScorer.Query>();var seen=new HashSet<String>();boolean atK=false,at3=false,vectorAtK=false;int ownerErrors=0,privacyErrors=0;
        for(String anchor:List.of("left","right")) {
            UUID anchorId=f.ids().get(anchor),partner=f.ids().get(anchor.equals("left")?"right":"left");var batch=recall.recall(f.owner(),anchorId,10,mode);
            if(batch==null) {queries.add(new QualityScorer.Query(anchor,List.of(),List.of(),List.of()));continue;}
            var keywords=retrieval.consolidationKeyword(f.owner(),anchorId,batch.anchor().memory().content(),100);var vector=retrieval.storedNeighbors(f.owner(),anchorId,model,100);
            var rawIds=new HashSet<UUID>();keywords.forEach(v->rawIds.add(v.id()));vector.forEach(v->rawIds.add(v.id()));batch.neighbors().forEach(v->rawIds.add(v.target().memory().id()));
            for(UUID id:rawIds) {
                var definition=f.definitions().get(id);if(definition==null||!definition.owner().equals("A")) ownerErrors++;
                if(definition==null||!eligible(definition)) privacyErrors++;
            }
            if(vector.stream().limit(10).anyMatch(v->v.id().equals(partner))) vectorAtK=true;
            if(batch.neighbors().stream().anyMatch(v->v.target().memory().id().equals(partner))) atK=true;
            if(batch.neighbors().stream().limit(3).anyMatch(v->v.target().memory().id().equals(partner))) at3=true;
            queries.add(new QualityScorer.Query(anchor,keywords.stream().map(v->key(f,v.id())).toList(),vector.stream().map(v->key(f,v.id())).toList(),batch.neighbors().stream().map(v->key(f,v.target().memory().id())).toList()));
            for(var n:batch.neighbors()) {
                String b=key(f,n.target().memory().id());String pair=java.util.stream.Stream.of(anchor,b).sorted().collect(java.util.stream.Collectors.joining("/"));if(!seen.add(pair)) continue;
                var outcome=writer.propose(f.owner(),batch,n);
                if(outcome.candidateId()==null) {attempts.add(new QualityScorer.Attempt(anchor,b,null,null,outcome.reason(),0,0,true));continue;}
                var candidate=service.get(f.owner(),outcome.candidateId());String from=candidate.direction().equals("SYMMETRIC")?null:key(f,candidate.direction().equals("A_TO_B")?candidate.memoryA().id():candidate.memoryB().id());
                int total=0,errors=0;for(var side:List.of(candidate.memoryA(),candidate.memoryB())) for(var e:side.evidence()) {
                    total++;try {var source=sources.get(f.owner(),e.sourceId());if(!e.role().equals("user")||!source.text().contains(e.excerpt())||!Objects.equals(source.occurredAt(),e.occurredAt())) errors++;}catch(RuntimeException ex) {errors++;}
                }
                attempts.add(new QualityScorer.Attempt(anchor,b,candidate.relation(),from,"PROPOSED",total,errors,candidate.status().equals("PENDING_REVIEW")&&candidate.approvedEffect().equals("RELATION_REVIEW_ONLY")));
            }
        }
        return new QualityScorer.Observation(c.id(),c.split(),mode.name(),atK,at3,vectorAtK,queries,attempts,ownerErrors,privacyErrors);
    }
    private double reviewProbe(Fixture f) {
        int correct=0,total=0;
        for(var type:List.of(ConsolidationRelation.SUPPORTS,ConsolidationRelation.EVOLVED_FROM)) {
            var created=new TransactionTemplate(transactions).execute(tx->{
                var targets=repository.targets(f.owner(),List.of(f.ids().get("left"),f.ids().get("right"))).stream().sorted(Comparator.comparing(t->t.memory().id().toString())).toList();
                repository.lockMemories(f.owner(),targets.get(0).memory().id(),targets.get(1).memory().id());var ids=new ArrayList<UUID>();
                for(String direction:List.of("A_TO_B","B_TO_A")) ids.add(repository.save(f.owner(),targets.get(0),targets.get(1),new ConsolidationClassifier.Proposal(type,direction,List.of("ISOLATED_REVIEW_GUARD_PROBE"),List.of("CANDIDATE_ONLY_NO_MEMORY_CHANGE")),"probe","v1",new com.google.gson.JsonObject()).id());return ids;
            });
            for(UUID id:created) {total++;try {service.review(f.owner(),id,ReviewDecision.APPROVE,1);}catch(IllegalStateException ex) {if(ex.getMessage().startsWith("RELATION_DIRECTION_CONFLICT")) correct++;}}
            total++;service.review(f.owner(),created.get(0),ReviewDecision.REJECT,1);if(service.review(f.owner(),created.get(1),ReviewDecision.APPROVE,1).status().equals("APPROVED")) correct++;
            for(UUID id:created) jdbc.update("DELETE FROM consolidation_candidate WHERE owner_id=? AND id=?",f.owner(),id); // isolated injected probes only
        }
        return (double)correct/total;
    }
    private static boolean eligible(QualityCorpus.Memory m) {return m.status().equals("ACTIVE")&&m.sensitivity().equals("NORMAL")&&m.origin().equals("USER_EXPLICIT")&&!m.sources().isEmpty()&&m.sources().stream().allMatch(s->s.owner().equals(m.owner())&&s.role().equals("user")&&s.privacy().equals("PRIVATE")&&!s.quote().isEmpty()&&s.text().contains(s.quote()));}
    private static String key(Fixture f,UUID id) {return f.definitions().containsKey(id)?f.definitions().get(id).key():"UNAUTHORIZED_UNKNOWN_ID";}
    private static UUID owner(String caseId,String owner) {return uuid(caseId+"/owner/"+owner);}
    private static UUID uuid(String text) {return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8));}
}
