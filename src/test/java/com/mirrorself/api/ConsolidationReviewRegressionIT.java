package com.mirrorself.api;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.mirrorself.consolidation.*;
import com.mirrorself.embedding.*;
import com.mirrorself.ingestion.*;
import com.mirrorself.memory.*;
import com.mirrorself.retrieval.RetrievalRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.extraction.mode=RULE",
        "mirror.model-egress-enabled=false","mirror.embedding.mode=DISABLED"})
@Import(ConsolidationReviewRegressionIT.Configuration.class)
class ConsolidationReviewRegressionIT extends ExtractionPostgresTestSupport {
    @TestConfiguration static class Configuration {
        @Bean @Primary TestVectors regressionVectors() {return new TestVectors();}
    }
    static class TestVectors implements EmbeddingProvider {
        int calls;
        public Model model() {return new Model("consolidation-review-fictional","v1");}
        public Location location() {return Location.LOCAL;}
        public float[] embed(String text) {calls++;float[] v=new float[1024];v[0]=1;return v;}
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired SourceRepository sources;
    @Autowired MemoryRepository memories;
    @Autowired ConsolidationRepository repository;
    @Autowired ConsolidationService service;
    @Autowired ConsolidationRecall recall;
    @Autowired ConsolidationScanService scans;
    @Autowired RetrievalRepository retrieval;
    @Autowired EmbeddingService indexing;
    @Autowired TestVectors vectors;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    static final Instant TIME=Instant.parse("2024-01-01T00:00:00Z");
    UUID source(UUID owner,String text) {
        return sources.save(owner,new SourceInput("CHATGPT","review-regression:"+UUID.randomUUID(),"fictional","user","{}",text,TIME)).id();
    }
    UUID seed(UUID owner,String text) {
        UUID id=UUID.randomUUID();jdbc.update("""
            INSERT INTO memory_item(id,owner_id,memory_type,content,assertion_origin,status,sensitivity,dedupe_key,occurred_at,temporal_scope)
            VALUES(?,?,'OPINION',?,'USER_EXPLICIT','ACTIVE','NORMAL',?,?::timestamptz,'SOURCE_TIME')
            """,id,owner,text,id.toString(),TIME.toString());
        jdbc.update("INSERT INTO memory_evidence(memory_id,source_id,excerpt) VALUES(?,?,?)",id,source(owner,text),text);return id;
    }
    UUID sourceId(UUID id) {return jdbc.queryForObject("SELECT source_id FROM memory_evidence WHERE memory_id=?",UUID.class,id);}
    @Test void highRankingReviewedInferencesCannotExhaustKeywordTop100() {
        UUID owner=UUID.randomUUID(),anchor=seed(owner,"I do not drink coffee."),target=seed(owner,"I avoid coffee.");
        for(int i=0;i<110;i++) {
            UUID distractor=seed(owner,"I do not drink coffee.");
            jdbc.update("UPDATE memory_item SET assertion_origin='MODEL_INFERRED' WHERE id=?",distractor);
        }
        var generic=retrieval.keyword(owner,"I do not drink coffee.",100);
        assertEquals(100,generic.size());assertFalse(generic.stream().anyMatch(x->x.id().equals(target))); // reproduces old pre-filter starvation
        assertEquals(List.of(target),retrieval.consolidationKeyword(owner,anchor,"I do not drink coffee.",1).stream().map(RetrievalRepository.Ranked::id).toList());
        assertEquals(List.of(target),recall.recall(owner,anchor,1,ConsolidationRecall.Mode.KEYWORD).neighbors().stream().map(x->x.target().memory().id()).toList());
        assertTrue(repository.targets(owner,generic.stream().map(RetrievalRepository.Ranked::id).toList()).stream()
            .allMatch(x->x.memory().origin()==AssertionOrigin.USER_EXPLICIT));
    }
    @Test void ineligibleHighScoresCannotEnterEitherRrfInput() {
        for(String kind:List.of("inferred","retracted","sensitive","secret","foreignOwner","restrictedSource","assistant","foreignSource","badQuote","noEvidence")) {
            UUID owner=UUID.randomUUID(),anchor=seed(owner,"I do not drink coffee."),target=seed(owner,"I avoid coffee.");
            UUID badOwner="foreignOwner".equals(kind)?UUID.randomUUID():owner;
            UUID bad=seed(badOwner,"I do not drink coffee.");
            switch(kind) {
                case "inferred" -> jdbc.update("UPDATE memory_item SET assertion_origin='MODEL_INFERRED' WHERE id=?",bad);
                case "retracted" -> jdbc.update("UPDATE memory_item SET status='RETRACTED' WHERE id=?",bad);
                case "sensitive" -> jdbc.update("UPDATE memory_item SET sensitivity='SENSITIVE' WHERE id=?",bad);
                case "secret" -> jdbc.update("UPDATE memory_item SET sensitivity='SECRET' WHERE id=?",bad);
                case "restrictedSource" -> jdbc.update("UPDATE source_record SET sensitivity='RESTRICTED' WHERE id=?",sourceId(bad));
                case "assistant" -> jdbc.update("UPDATE source_record SET role='assistant' WHERE id=?",sourceId(bad));
                case "foreignSource" -> jdbc.update("UPDATE source_record SET owner_id=? WHERE id=?",UUID.randomUUID(),sourceId(bad));
                case "badQuote" -> jdbc.update("UPDATE memory_evidence SET excerpt='fabricated quotation' WHERE memory_id=?",bad);
                case "noEvidence" -> jdbc.update("DELETE FROM memory_evidence WHERE memory_id=?",bad);
                default -> { }
            }
            indexing.run(owner,100);if(!badOwner.equals(owner)) indexing.run(badOwner,100);int calls=vectors.calls;
            assertEquals(List.of(target),retrieval.consolidationKeyword(owner,anchor,"I do not drink coffee.",1).stream().map(RetrievalRepository.Ranked::id).toList(),kind);
            assertEquals(List.of(target),retrieval.storedNeighbors(owner,anchor,vectors.model(),100).stream().map(RetrievalRepository.Ranked::id).toList(),kind);
            var batch=recall.recall(owner,anchor,1,ConsolidationRecall.Mode.HYBRID);
            assertEquals(List.of(target),batch.neighbors().stream().map(x->x.target().memory().id()).toList(),kind);
            assertEquals(1,batch.neighbors().getFirst().score().keywordRank());assertEquals(1,batch.neighbors().getFirst().score().vectorRank());
            assertEquals(calls,vectors.calls);assertTrue(repository.targets(owner,List.of(bad)).isEmpty(),kind);
        }
    }
    record Pair(UUID owner,UUID a,UUID b) { }
    Pair pair(boolean additionalEvidence) {return pair(UUID.randomUUID(),additionalEvidence);}
    Pair pair(UUID owner,boolean additionalEvidence) {
        UUID x=seed(owner,"我不喝咖啡。"),y=seed(owner,"我不喝咖啡。");
        if(additionalEvidence) {
            jdbc.update("UPDATE memory_evidence SET source_id=? WHERE memory_id=?",sourceId(x),y);
            jdbc.update("INSERT INTO memory_evidence(memory_id,source_id,excerpt) VALUES(?,?,?)",x,source(owner,"我不喝咖啡。"),"我不喝咖啡。");
        }
        return new Pair(owner,x.toString().compareTo(y.toString())<0?x:y,x.toString().compareTo(y.toString())<0?y:x);
    }
    UUID oldCandidate(Pair pair,ConsolidationRelation type) {
        return new TransactionTemplate(transactions).execute(tx->{
            repository.lockMemories(pair.owner(),pair.a(),pair.b());
            var targets=repository.targets(pair.owner(),List.of(pair.a(),pair.b()));
            var byId=targets.stream().collect(java.util.stream.Collectors.toMap(t->t.memory().id(),t->t));
            var proposal=new ConsolidationClassifier.Proposal(type,type==ConsolidationRelation.SAME_AS?"SYMMETRIC":"A_TO_B",List.of("FICTIONAL_OLD_ENGINE"),List.of("CANDIDATE_ONLY_NO_MEMORY_CHANGE"));
            var discovery=new JsonObject();discovery.addProperty("modelRequests",0);
            UUID id=repository.save(pair.owner(),byId.get(pair.a()),byId.get(pair.b()),proposal,"fictional-old","v0",discovery).id();
            jdbc.update("UPDATE consolidation_candidate SET engine_version='consolidation-rules-0' WHERE id=?",id);return id;
        });
    }
    void upgrade(Pair pair) {
        indexing.run(pair.owner(),100);int calls=vectors.calls;
        var job=scans.run(pair.owner(),10,10,ConsolidationRecall.Mode.HYBRID);
        assertEquals("COMPLETE",job.status());assertEquals(ConsolidationWriter.ENGINE_VERSION,job.engineVersion());
        assertEquals(1,job.created());assertEquals(calls,vectors.calls);
    }
    @Test void upgradedEngineCannotIndependentlyApproveSameAsAndSupports() {
        for(boolean additional:List.of(false,true)) {
            var pair=pair(additional);UUID old=oldCandidate(pair,additional?ConsolidationRelation.SAME_AS:ConsolidationRelation.SUPPORTS);upgrade(pair);
            var all=service.list(pair.owner(),null,10,0);assertEquals(2,all.size());
            for(var c:all) {
                assertEquals("PENDING_REVIEW",c.status());assertTrue(c.approvalBlocked());assertEquals(1,c.relationConflicts().size());
                assertTrue(c.reasonCodes().contains("RELATION_TYPE_CONFLICT"));
                assertThrows(IllegalStateException.class,()->service.review(pair.owner(),c.id(),ReviewDecision.APPROVE,c.reviewVersion()));
            }
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE owner_id=? AND action='CONSOLIDATION_RELATION_CONFLICT'",Integer.class,pair.owner()));
            service.review(pair.owner(),old,ReviewDecision.REJECT,1);
            var remaining=service.list(pair.owner(),"PENDING_REVIEW",10,0).getFirst();assertFalse(remaining.approvalBlocked());assertTrue(remaining.relationConflicts().isEmpty());
            assertEquals("APPROVED",service.review(pair.owner(),remaining.id(),ReviewDecision.APPROVE,1).status());
            assertEquals(0,scans.run(pair.owner(),10,20,ConsolidationRecall.Mode.HYBRID).created());
            assertEquals("REJECTED",service.get(pair.owner(),old).status());
        }
    }
    @Test void priorApprovalIsPreservedAndBlocksConflictingUpgrade() {
        var pair=pair(true);UUID old=oldCandidate(pair,ConsolidationRelation.SAME_AS);
        service.review(pair.owner(),old,ReviewDecision.APPROVE,1);
        var before=jdbc.queryForList("SELECT to_jsonb(m)::text FROM memory_item m WHERE owner_id=? ORDER BY id",pair.owner());upgrade(pair);
        var pending=service.list(pair.owner(),"PENDING_REVIEW",10,0).getFirst();assertTrue(pending.approvalBlocked());
        assertEquals("APPROVED",service.get(pair.owner(),old).status());
        assertThrows(IllegalStateException.class,()->service.review(pair.owner(),pending.id(),ReviewDecision.APPROVE,1));
        service.review(pair.owner(),pending.id(),ReviewDecision.REJECT,1);
        assertEquals("APPROVED",service.get(pair.owner(),old).status());assertFalse(service.get(pair.owner(),old).approvalBlocked());
        assertEquals(before,jdbc.queryForList("SELECT to_jsonb(m)::text FROM memory_item m WHERE owner_id=? ORDER BY id",pair.owner()));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM memory_relation WHERE owner_id=?",Integer.class,pair.owner()));
    }
    @Test void parallelApprovalsCannotApproveCompetingRelations() throws Exception {
        var pair=pair(true);oldCandidate(pair,ConsolidationRelation.SAME_AS);upgrade(pair);
        var all=service.list(pair.owner(),null,10,0);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var tasks=all.stream().map(c->pool.submit(()->assertThrows(IllegalStateException.class,()->service.review(pair.owner(),c.id(),ReviewDecision.APPROVE,1)))).toList();
            for(var task:tasks) task.get();
        } finally {pool.shutdownNow();}
        assertEquals(0,service.list(pair.owner(),"APPROVED",10,0).size());
    }
    @Test void conflictVisibilityRespectsOwnerAndInvalidation() {
        var pair=pair(true);oldCandidate(pair,ConsolidationRelation.SAME_AS);upgrade(pair);UUID other=UUID.randomUUID();
        var current=service.list(pair.owner(),null,10,0);assertEquals(2,current.size());assertTrue(service.list(other,null,10,0).isEmpty());
        assertThrows(IllegalArgumentException.class,()->service.get(other,current.getFirst().id()));
        jdbc.update("UPDATE memory_item SET status='RETRACTED' WHERE id=?",pair.a());
        assertTrue(service.list(pair.owner(),null,10,0).isEmpty());
        assertThrows(IllegalArgumentException.class,()->service.review(pair.owner(),current.getFirst().id(),ReviewDecision.APPROVE,1));
    }
    @Test void conflictIsVisibleThroughApiAndApprovalReturns409() throws Exception {
        UUID owner=UUID.fromString("11111111-1111-4111-8111-111111111111");var pair=pair(owner,true);
        oldCandidate(pair,ConsolidationRelation.SAME_AS);upgrade(pair);
        var pending=service.list(owner,null,10,0).getFirst();var client=java.net.http.HttpClient.newHttpClient();
        String url="http://127.0.0.1:"+port+"/api/v1/consolidation/candidates/"+pending.id();
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).header("X-Mirror-Token","0123456789abcdef0123456789abcdef").build();
        var response=client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(200,response.statusCode());
        var body=com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();assertTrue(body.get("approvalBlocked").getAsBoolean());
        assertEquals(1,body.getAsJsonArray("relationConflicts").size());assertTrue(response.body().contains("RELATION_TYPE_CONFLICT"));
        request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(url+"/review")).header("X-Mirror-Token","0123456789abcdef0123456789abcdef")
            .header("Content-Type","application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"decision\":\"APPROVE\",\"expectedVersion\":1}")).build();
        response=client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(409,response.statusCode());
        assertTrue(response.body().contains("RELATION_TYPE_CONFLICT"));
    }
    @Test void upgradeRevisitsPreviousCursorWithoutChangingMemoryRevision() {
        UUID owner=UUID.randomUUID(),anchor=seed(owner,"I do not drink coffee.");
        String oldKey=com.mirrorself.common.Hashing.sha256(ConsolidationClassifier.VERSION+"\n"+recall.modelKey()+"\nKEYWORD\n10");
        var target=repository.targets(owner,List.of(anchor)).getFirst();repository.markProcessed(owner,new ConsolidationRepository.Anchor(anchor,target.revision()),oldKey,0);
        var job=scans.run(owner,10,10,ConsolidationRecall.Mode.KEYWORD);assertEquals(1,job.selected());assertEquals(1,job.processed());
        assertEquals(target.revision(),repository.targets(owner,List.of(anchor)).getFirst().revision());
    }
}
