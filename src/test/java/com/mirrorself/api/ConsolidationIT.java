package com.mirrorself.api;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import com.mirrorself.common.Hashing;
import com.mirrorself.consolidation.*;
import com.mirrorself.embedding.*;
import com.mirrorself.ingestion.*;
import com.mirrorself.memory.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.extraction.mode=RULE","mirror.model-egress-enabled=false","mirror.embedding.mode=DISABLED"})
@Import(ConsolidationIT.Configuration.class)
class ConsolidationIT extends ExtractionPostgresTestSupport {
    @TestConfiguration static class Configuration {
        @Bean @Primary FixtureProvider candidateFixture() {return new FixtureProvider();}
    }
    static class FixtureProvider implements EmbeddingProvider {
        int calls;String version="v1";
        public Model model() {return new Model("all-similar-adversarial",version);}public Location location() {return Location.LOCAL;}
        public float[] embed(String text) {calls++;var vector=new float[1024];vector[0]=1;return vector;}
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired SourceRepository sources;
    @Autowired MemoryRepository memories;
    @Autowired MemoryService memoryService;
    @Autowired EmbeddingService embedding;
    @Autowired FixtureProvider fixture;
    @Autowired ConsolidationScanService scans;
    @Autowired ConsolidationService candidates;
    @Autowired ConsolidationRecall recall;
    @Autowired ConsolidationWriter writer;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    record Pair(UUID owner,UUID left,UUID right) { }
    Pair pair(String left,String right) {
        UUID owner=UUID.randomUUID();return new Pair(owner,seed(owner,left,"OPINION","2024-01-01T00:00:00Z"),seed(owner,right,"OPINION","2024-01-01T00:00:00Z"));
    }
    UUID seed(UUID owner,String text,String type,String time) {
        var raw=new JsonObject();raw.addProperty("text",text);var source=sources.save(owner,new SourceInput("CHATGPT","consolidation:"+UUID.randomUUID(),"fictional","user",raw.toString(),text,time==null?null:Instant.parse(time)));
        return insert(owner,text,type,"ACTIVE","NORMAL","USER_EXPLICIT","SOURCE_TIME",time,List.of(source.id()),List.of(text));
    }
    UUID insert(UUID owner,String text,String type,String status,String sensitivity,String origin,String scope,String time,List<UUID> sourceIds,List<String> quotes) {
        UUID id=UUID.randomUUID();jdbc.update("""
            INSERT INTO memory_item(id,owner_id,memory_type,content,assertion_origin,status,sensitivity,dedupe_key,temporal_scope,occurred_at)
            VALUES(?,?,?,?,?,?,?,?,?,?::timestamptz)
            """,id,owner,type,text,origin,status,sensitivity,Hashing.sha256(id.toString()),scope,time);
        for(int i=0;i<sourceIds.size();i++) jdbc.update("INSERT INTO memory_evidence(memory_id,source_id,excerpt) VALUES(?,?,?)",id,sourceIds.get(i),quotes.get(i));
        return id;
    }
    ConsolidationService.View discover(Pair p) {
        embedding.run(p.owner(),100);var job=scans.run(p.owner(),10,10,ConsolidationRecall.Mode.HYBRID);assertEquals("COMPLETE",job.status());
        assertEquals(1,job.created());return candidates.list(p.owner(),null,100,0).getFirst();
    }
    @Test void reviewsAreRelationOnlyVersionedAndIdempotent() {
        var p=pair("我不喝咖啡。","I do not drink coffee.");var c=discover(p);
        var before=jdbc.queryForList("SELECT to_jsonb(m)::text AS original FROM memory_item m WHERE owner_id=? ORDER BY id",p.owner());
        int calls=fixture.calls;var approved=candidates.review(p.owner(),c.id(),ReviewDecision.APPROVE,c.reviewVersion());
        assertEquals("APPROVED",approved.status());assertEquals("RELATION_REVIEW_ONLY",approved.approvedEffect());
        assertEquals(before,jdbc.queryForList("SELECT to_jsonb(m)::text AS original FROM memory_item m WHERE owner_id=? ORDER BY id",p.owner()));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM memory_relation WHERE owner_id=?",Integer.class,p.owner()));
        assertEquals(0,scans.run(p.owner(),10,10,ConsolidationRecall.Mode.HYBRID).selected());assertEquals(calls,fixture.calls);
        assertThrows(IllegalStateException.class,()->candidates.review(p.owner(),c.id(),ReviewDecision.REJECT,1));
        assertEquals(1,candidates.list(p.owner(),null,100,0).size());
    }
    @Test void rejectionIsNotRevivedByOtherRecallSettings() {
        var p=pair("我不喝咖啡。","I do not drink coffee.");var c=discover(p);candidates.review(p.owner(),c.id(),ReviewDecision.REJECT,1);
        assertEquals(0,scans.run(p.owner(),10,20,ConsolidationRecall.Mode.HYBRID).created());
        assertEquals("REJECTED",candidates.get(p.owner(),c.id()).status());
    }
    @Test void newStoredVectorsRevisitKeywordFallbackWithoutModelCallsDuringScan() {
        var p=pair("我不喝咖啡。","I do not drink coffee.");var initial=scans.run(p.owner(),10,10,ConsolidationRecall.Mode.HYBRID);
        assertEquals(0,initial.created());assertEquals(2,initial.processed());
        embedding.run(p.owner(),100);int calls=fixture.calls;var next=scans.run(p.owner(),10,10,ConsolidationRecall.Mode.HYBRID);
        assertEquals(1,next.created());assertTrue(next.vectorEpoch()>initial.vectorEpoch());assertEquals(calls,fixture.calls);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM consolidation_cursor WHERE owner_id=?",Integer.class,p.owner()));
    }
    @Test void statusPrivacyEvidenceTimeAndDeletionInvalidateCandidates() {
        for(String change:List.of("status","privacy","evidence","sourceTime","memoryTime","validityRange","rawMetadata","deleteSource","deleteMemory")) {
            var p=pair("我不喝咖啡。","I do not drink coffee.");var c=discover(p);candidates.review(p.owner(),c.id(),ReviewDecision.APPROVE,1);
            UUID source=memories.list(p.owner(),null,10).stream().filter(m->m.id().equals(p.left())).findFirst().orElseThrow().evidence().getFirst().sourceId();
            switch(change) {
                case "status" -> jdbc.update("UPDATE memory_item SET status='RETRACTED' WHERE id=?",p.left());
                case "privacy" -> jdbc.update("UPDATE source_record SET sensitivity='RESTRICTED' WHERE id=?",source);
                case "evidence" -> jdbc.update("UPDATE memory_evidence SET excerpt='不喝咖啡' WHERE memory_id=?",p.left());
                case "sourceTime" -> jdbc.update("UPDATE source_record SET occurred_at='2020-01-01T00:00:00Z' WHERE id=?",source);
                case "memoryTime" -> jdbc.update("UPDATE memory_item SET occurred_at='2020-01-01T00:00:00Z' WHERE id=?",p.left());
                case "validityRange" -> jdbc.update("UPDATE memory_item SET valid_to='2025-01-01T00:00:00Z' WHERE id=?",p.left());
                case "rawMetadata" -> jdbc.update("UPDATE source_record SET raw_message='{\"changed\":true}' WHERE id=?",source);
                case "deleteSource" -> memoryService.deleteSource(p.owner(),source);
                default -> memoryService.deleteMemory(p.owner(),p.left());
            }
            assertTrue(candidates.list(p.owner(),null,100,0).isEmpty(),change);
            assertThrows(IllegalArgumentException.class,()->candidates.review(p.owner(),c.id(),ReviewDecision.APPROVE,2));
        }
    }
    @Test void changingEvidenceRequiresNewPendingReviewAndStaleRecallCannotWrite() {
        var p=pair("我不喝咖啡。","I do not drink coffee.");var c=discover(p);candidates.review(p.owner(),c.id(),ReviewDecision.APPROVE,1);
        var batch=recall.recall(p.owner(),p.left(),10,ConsolidationRecall.Mode.HYBRID);assertFalse(batch.neighbors().isEmpty());
        jdbc.update("UPDATE memory_item SET occurred_at='2025-01-01T00:00:00Z' WHERE id=?",p.right());
        assertEquals("STALE",writer.propose(p.owner(),batch,batch.neighbors().getFirst()).kind());
        assertTrue(candidates.list(p.owner(),null,100,0).isEmpty());
    }
    @Test void evidenceChangeCreatesFreshPendingCandidateWithoutRevivingApproval() {
        var p=pair("我不喝咖啡。","I do not drink coffee.");var old=discover(p);candidates.review(p.owner(),old.id(),ReviewDecision.APPROVE,1);
        String text="我不喝咖啡。";var source=sources.save(p.owner(),new SourceInput("CHATGPT","extra:"+UUID.randomUUID(),"fictional","user","{}",text,Instant.parse("2024-01-01T00:00:00Z")));
        jdbc.update("INSERT INTO memory_evidence(memory_id,source_id,excerpt) VALUES(?,?,?)",p.left(),source.id(),text);
        embedding.run(p.owner(),100);scans.run(p.owner(),10,10,ConsolidationRecall.Mode.HYBRID);
        var current=candidates.list(p.owner(),null,100,0).getFirst();assertNotEquals(old.id(),current.id());assertEquals("PENDING_REVIEW",current.status());
        memoryService.deleteSource(p.owner(),source.id());assertEquals(2,memories.list(p.owner(),null,10).size());assertTrue(candidates.list(p.owner(),null,100,0).isEmpty());
    }
    @Test void explicitValidityRangesRequireSeparateReview() {
        var p=pair("我不喝咖啡。","I do not drink coffee.");
        jdbc.update("UPDATE memory_item SET valid_to='2025-01-01T00:00:00Z' WHERE id=?",p.left());
        embedding.run(p.owner(),100);var job=scans.run(p.owner(),10,10,ConsolidationRecall.Mode.HYBRID);
        assertEquals("COMPLETE",job.status());assertTrue(job.abstained()>0);assertTrue(candidates.list(p.owner(),null,100,0).isEmpty());
    }
    @Test void modelVersionSwitchDoesNotMixVectorsOrReviveReviewedCandidate() {
        var p=pair("我不喝咖啡。","I do not drink coffee.");var c=discover(p);candidates.review(p.owner(),c.id(),ReviewDecision.REJECT,1);
        String old=fixture.version;
        try {
            fixture.version="v2";assertEquals(0,scans.run(p.owner(),10,10,ConsolidationRecall.Mode.HYBRID).created());
            embedding.run(p.owner(),100);assertEquals(0,scans.run(p.owner(),10,10,ConsolidationRecall.Mode.HYBRID).created());
            assertEquals("REJECTED",candidates.get(p.owner(),c.id()).status());assertEquals("v1",candidates.get(p.owner(),c.id()).discoveryModelVersion());
        } finally {fixture.version=old;}
    }
    @Test void ownerIsolationIsEnforcedByQueriesAndDatabaseGuard() {
        var p=pair("我不喝咖啡。","I do not drink coffee.");var c=discover(p);UUID other=UUID.randomUUID();
        assertTrue(candidates.list(other,null,100,0).isEmpty());assertThrows(IllegalArgumentException.class,()->candidates.get(other,c.id()));
        assertThrows(IllegalArgumentException.class,()->candidates.review(other,c.id(),ReviewDecision.APPROVE,1));
        UUID foreign=seed(other,"我不喝咖啡。","OPINION","2024-01-01T00:00:00Z");
        UUID a=p.left().toString().compareTo(foreign.toString())<0?p.left():foreign,b=a.equals(p.left())?foreign:p.left();
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("""
            INSERT INTO consolidation_candidate(id,owner_id,memory_a_id,memory_b_id,a_revision,b_revision,relation_type,direction,engine_version,discovery_model_id,discovery_model_version,reason_codes,uncertainties,discovery)
            SELECT ?,?,?,?,1,1,'SAME_AS','SYMMETRIC','test','test','v1','[]','[]','{}'
            """,UUID.randomUUID(),p.owner(),a,b));
    }
    @Test void scansAreBoundedAndConcurrentScansDoNotDuplicate() throws Exception {
        var p=pair("我不喝咖啡。","I do not drink coffee.");embedding.run(p.owner(),100);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {var a=pool.submit(()->scans.run(p.owner(),10,10,ConsolidationRecall.Mode.HYBRID));var b=pool.submit(()->scans.run(p.owner(),10,10,ConsolidationRecall.Mode.HYBRID));
            assertEquals("COMPLETE",a.get().status());assertEquals("COMPLETE",b.get().status());
        } finally {pool.shutdownNow();}
        assertEquals(1,candidates.list(p.owner(),null,100,0).size());
        UUID dense=UUID.randomUUID();for(int i=0;i<60;i++) seed(dense,"我喜欢测试项目"+i+"。","OPINION","2024-01-01T00:00:00Z");
        var bounded=scans.run(dense,3,2,ConsolidationRecall.Mode.KEYWORD);assertEquals(3,bounded.selected());assertTrue(bounded.comparedPairs()<=6);
    }
    @Test void endpointsRequireExistingTokenAndReturnEvidence() throws Exception {
        var client=java.net.http.HttpClient.newHttpClient();String base="http://127.0.0.1:"+port;
        for(String path:List.of("/api/v1/consolidation/jobs","/api/v1/consolidation/candidates")) {
            var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+path));
            if(path.endsWith("jobs")) request.POST(java.net.http.HttpRequest.BodyPublishers.noBody());
            assertEquals(401,client.send(request.build(),java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
        }
        UUID owner=UUID.fromString("11111111-1111-4111-8111-111111111111");seed(owner,"我不喝咖啡。","OPINION","2024-01-01T00:00:00Z");seed(owner,"I do not drink coffee.","OPINION","2024-01-01T00:00:00Z");
        embedding.run(owner,100);
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/api/v1/consolidation/jobs"))
            .header("X-Mirror-Token","0123456789abcdef0123456789abcdef").POST(java.net.http.HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(200,client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
        request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/api/v1/consolidation/candidates"))
            .header("X-Mirror-Token","0123456789abcdef0123456789abcdef").build();
        var response=client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(200,response.statusCode());
        var item=JsonParser.parseString(response.body()).getAsJsonArray().get(0).getAsJsonObject();
        assertEquals("PENDING_REVIEW",item.get("status").getAsString());assertTrue(item.getAsJsonObject("memoryA").getAsJsonArray("evidence").size()>0);
    }

    @Test void evaluatesIndependentFictionalPairsThroughStoredVectorRecallAndReviewPolicy() throws Exception {
        var path=java.nio.file.Path.of("evaluation/datasets/consolidation-fictional-v1.json");String input=java.nio.file.Files.readString(path);
        var data=JsonParser.parseString(input).getAsJsonObject();var rows=new ArrayList<Map<String,Object>>();
        int tp=0,fp=0,fn=0,predictedSame=0,falseSame=0,evidenceTotal=0,evidenceCorrect=0,policyTotal=0,policyCorrect=0;
        for(var element:data.getAsJsonArray("cases")) {
            var test=element.getAsJsonObject();UUID owner=UUID.randomUUID(),other=UUID.randomUUID();var sourceMap=new HashMap<String,UUID>();
            var ids=new ArrayList<UUID>();
            for(String side:List.of("left","right")) {
                var f=test.getAsJsonObject(side);UUID who="A".equals(f.get("owner").getAsString())?owner:other;
                String text=f.get("text").getAsString();String time=f.get("time").isJsonNull()?null:f.get("time").getAsString();
                var refs=new ArrayList<UUID>();var quotes=new ArrayList<String>();var definitions=f.getAsJsonArray("sources");
                if(definitions.isEmpty()) {definitions=new JsonArray();var item=new JsonObject();item.addProperty("key",side);item.addProperty("text",text);definitions.add(item);}
                for(var def:definitions) {
                    var source=def.getAsJsonObject();String key=who+":"+source.get("key").getAsString(),quote=source.get("text").getAsString();
                    String sourceTime=source.has("time")?source.get("time").getAsString():time;
                    UUID sourceId=sourceMap.get(key);
                    if(sourceId==null) {
                        var raw=new JsonObject();raw.addProperty("text",quote);sourceId=sources.save(who,new SourceInput("CHATGPT",test.get("id").getAsString()+":"+UUID.randomUUID(),"fictional consolidation",f.get("role").getAsString(),raw.toString(),quote,sourceTime==null?null:Instant.parse(sourceTime))).id();
                        sourceMap.put(key,sourceId);jdbc.update("UPDATE source_record SET sensitivity=? WHERE id=?",f.get("privacy").getAsString(),sourceId);
                    }
                    refs.add(sourceId);quotes.add(quote);
                }
                UUID id=insert(who,text,f.get("type").getAsString(),f.get("status").getAsString(),f.get("sensitivity").getAsString(),f.get("origin").getAsString(),f.get("scope").getAsString(),time,refs,quotes);
                ids.add(id);if(f.has("deleted")&&f.get("deleted").getAsBoolean()) memoryService.deleteMemory(who,id);
            }
            embedding.run(owner,100);embedding.run(other,100);int modelCalls=fixture.calls;
            var job=scans.run(owner,10,10,ConsolidationRecall.Mode.HYBRID);assertEquals("COMPLETE",job.status());assertEquals(modelCalls,fixture.calls);
            var found=candidates.list(owner,null,100,0);String expected=test.get("expectedRelation").isJsonNull()?null:test.get("expectedRelation").getAsString();
            boolean exact=found.size()==1&&found.getFirst().relation().name().equals(expected);
            if(expected!=null) {if(exact) tp++;else fn++;}if(!exact) fp+=found.size();
            for(var c:found) {
                if(c.relation()==ConsolidationRelation.SAME_AS) {predictedSame++;if(!"SAME_AS".equals(expected)) falseSame++;}
                policyTotal++;if("PENDING_REVIEW".equals(c.status())&&"RELATION_REVIEW_ONLY".equals(c.approvedEffect())) policyCorrect++;
                for(var target:List.of(c.memoryA(),c.memoryB())) for(var e:target.evidence()) {
                    evidenceTotal++;if("user".equals(e.role())&&sources.get(owner,e.sourceId()).text().contains(e.excerpt())) evidenceCorrect++;
                }
                if(test.has("expectedFrom")&&exact) {
                    UUID expectedFrom=ids.get("right".equals(test.get("expectedFrom").getAsString())?1:0);
                    assertEquals(expectedFrom,"A_TO_B".equals(c.direction())?c.memoryA().id():c.memoryB().id());
                }
            }
            rows.add(Map.of("caseId",test.get("id").getAsString(),"coverage",test.get("coverage").getAsString(),"expected",expected==null?"NONE":expected,
                "predicted",found.stream().map(ConsolidationService.View::relation).toList(),"job",job,"candidates",found));
        }
        double precision=(double)tp/(tp+fp),recall=(double)tp/(tp+fn),falseRate=predictedSame==0?0:(double)falseSame/predictedSame;
        var report=new LinkedHashMap<String,Object>();report.put("condition","OFFLINE_ADVERSARIAL_ALL_SIMILAR_STORED_VECTORS");report.put("datasetSha256",Hashing.sha256(input));
        report.put("engineVersion",ConsolidationClassifier.VERSION);report.put("tp",tp);report.put("fp",fp);report.put("fn",fn);report.put("precision",precision);report.put("recall",recall);
        report.put("predictedSameAs",predictedSame);report.put("falseSameAs",falseSame);report.put("falseSameAsRate",falseRate);report.put("evidenceCorrect",evidenceCorrect);report.put("evidenceTotal",evidenceTotal);
        report.put("evidenceAccuracy",(double)evidenceCorrect/evidenceTotal);report.put("reviewPolicyCorrect",policyCorrect);report.put("reviewPolicyTotal",policyTotal);report.put("reviewPolicyAccuracy",(double)policyCorrect/policyTotal);
        report.put("realModelRequests",0);report.put("scanModelRequests",0);report.put("humanReferenceReviewComplete",false);report.put("cases",rows);
        var output=java.nio.file.Path.of("target/evaluation/consolidation-offline-v1");java.nio.file.Files.createDirectories(output);
        java.nio.file.Files.writeString(output.resolve("report.json"),com.mirrorself.evaluation.EvaluationJson.GSON.toJson(report)+"\n");
        java.nio.file.Files.writeString(output.resolve("report.md"),String.format(java.util.Locale.ROOT,"# Consolidation offline observation%n%n31 fictional pairs; all eligible stored vectors deliberately identical. No real model calls.%n%nTP=%d FP=%d FN=%d%n%nPrecision=%.4f Recall=%.4f False SAME_AS rate=%.4f%n%nEvidence=%d/%d Review policy=%d/%d%n",tp,fp,fn,precision,recall,falseRate,evidenceCorrect,evidenceTotal,policyCorrect,policyTotal));
        var gates=data.getAsJsonObject("gates");assertTrue(precision>=gates.get("minPrecision").getAsDouble());assertTrue(recall>=gates.get("minRecall").getAsDouble());
        assertEquals(0,falseSame);assertEquals(evidenceTotal,evidenceCorrect);assertEquals(policyTotal,policyCorrect);
    }
}
