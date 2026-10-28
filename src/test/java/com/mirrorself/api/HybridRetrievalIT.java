package com.mirrorself.api;

import static org.junit.jupiter.api.Assertions.*;
import com.mirrorself.embedding.*;
import com.mirrorself.memory.*;
import com.mirrorself.retrieval.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.model-egress-enabled=false"})
@Import(HybridRetrievalIT.Configuration.class)
class HybridRetrievalIT extends ExtractionPostgresTestSupport {
    @TestConfiguration static class Configuration {
        @Bean @Primary ControlledProvider embeddingFixture() {return new ControlledProvider();}
    }
    static class ControlledProvider implements EmbeddingProvider {
        String version="v1"; Location location=Location.LOCAL;boolean fail=false;int calls;int dimensions=1024;
        Runnable beforeReturn=()->{};
        @Override public Model model() {return new Model("controlled-offline",version);}
        @Override public Location location() {return location;}
        @Override public float[] embed(String text) {
            calls++;if(fail) throw new IllegalStateException("fictional-private-response");
            beforeReturn.run();var v=new float[dimensions];v[text.contains("hiking")||text.contains("trekking")?0:1]=1;return v;
        }
    }
    @Autowired ControlledProvider provider;
    @Autowired EmbeddingService indexing;
    @Autowired EmbeddingRepository embeddings;
    @Autowired MemoryService memories;
    @Autowired MemoryRepository repository;
    @Autowired RetrievalService retrieval;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    UUID owner;
    @BeforeEach void reset() { owner=UUID.randomUUID();provider.version="v1";provider.fail=false;provider.calls=0;
        provider.location=EmbeddingProvider.Location.LOCAL;provider.dimensions=1024;provider.beforeReturn=()->{}; }
    UUID add(String text) {return memories.addManual(owner,MemoryType.FACT,text,Sensitivity.NORMAL,Instant.parse("2024-01-01T00:00:00Z")).id();}
    int vectors() {return jdbc.queryForObject("SELECT count(*) FROM memory_embedding WHERE owner_id=?",Integer.class,owner);}
    @Test void incrementalJobsAreIdempotentAndVersionIsolated() {
        UUID id=add("I enjoy hiking.");assertEquals(1,indexing.run(owner,10).inserted());
        assertEquals(0,indexing.run(owner,10).selected());assertEquals(1,provider.calls);
        var hit=retrieval.explain(owner,"trekking",3,RetrievalService.Mode.HYBRID).hits().getFirst();
        assertEquals(id,hit.memory().id());assertEquals(List.of("VECTOR"),hit.methods());assertEquals("user",hit.memory().evidence().getFirst().role());
        provider.version="v2";assertTrue(retrieval.explain(owner,"trekking",3,RetrievalService.Mode.VECTOR).hits().isEmpty());
        assertEquals(1,indexing.run(owner,10).inserted());assertEquals(2,vectors());
        assertEquals(id,retrieval.explain(owner,"trekking",3,RetrievalService.Mode.VECTOR).hits().getFirst().memory().id());
    }
    @Test void lifecycleInvalidatesAllVersionsAndPendingRequiresApproval() {
        UUID id=memories.addManual(owner,MemoryType.THOUGHT_PROCESS,"I tend to sketch before deciding.",Sensitivity.NORMAL,null).id();
        assertEquals(0,indexing.run(owner,10).selected());assertTrue(repository.search(owner,"sketch",10).isEmpty());
        memories.review(owner,id,ReviewDecision.APPROVE);assertEquals(1,indexing.run(owner,10).inserted());
        provider.version="v2";indexing.run(owner,10);assertEquals(2,vectors());
        jdbc.update("UPDATE memory_item SET status='RETRACTED' WHERE id=?",id);assertEquals(0,vectors());
        assertTrue(retrieval.retrieve(owner,"sketch",3).isEmpty());
        UUID rejected=memories.addManual(owner,MemoryType.THOUGHT_PROCESS,"I weigh every option.",Sensitivity.NORMAL,null).id();
        memories.review(owner,rejected,ReviewDecision.REJECT);assertEquals(0,indexing.run(owner,10).selected());
    }
    @Test void sourcePrivacyChangesAndDeletionInvalidateOldVectors() {
        UUID id=add("I enjoy hiking.");indexing.run(owner,10);var source=repository.list(owner,null,10).getFirst().evidence().getFirst().sourceId();
        jdbc.update("UPDATE source_record SET sensitivity='RESTRICTED' WHERE id=?",source);assertEquals(0,vectors());
        assertTrue(repository.search(owner,"hiking",10).isEmpty());assertEquals(0,indexing.run(owner,10).selected());
        jdbc.update("UPDATE source_record SET sensitivity='PRIVATE' WHERE id=?",source);indexing.run(owner,10);
        memories.deleteSource(owner,source);assertEquals(0,vectors());assertTrue(repository.list(owner,null,10).isEmpty());
        UUID other=add("I enjoy hiking again.");indexing.run(owner,10);memories.deleteMemory(owner,other);assertEquals(0,vectors());
    }
    @Test void inFlightStatusChangeCannotResurrectAnOldVector() {
        UUID id=add("I enjoy hiking.");provider.beforeReturn=()->jdbc.update("UPDATE memory_item SET status='RETRACTED' WHERE id=?",id);
        var job=indexing.run(owner,10);assertEquals(0,job.inserted());assertEquals(1,job.skipped());assertEquals(0,vectors());
    }
    @Test void databaseFiltersOwnerStatePrivacyAndAssistantAttributionBeforeRanking() {
        UUID valid=add("I enjoy hiking.");UUID hidden=add("I enjoy hiking secretly.");
        indexBothVersions();
        jdbc.update("UPDATE memory_item SET sensitivity='SECRET' WHERE id=?",hidden);
        UUID assistant=add("I recommend hiking.");UUID source=repository.list(owner,null,10).stream().filter(m->m.id().equals(assistant)).findFirst().orElseThrow().evidence().getFirst().sourceId();
        jdbc.update("UPDATE source_record SET role='assistant' WHERE id=?",source);
        UUID other=UUID.randomUUID();memories.addManual(other,MemoryType.FACT,"I enjoy hiking elsewhere.",Sensitivity.NORMAL,null);indexing.run(other,10);
        assertEquals(List.of(valid),retrieval.retrieve(owner,"hiking",10).stream().map(MemoryRepository.MemoryView::id).toList());
        assertEquals(List.of(valid),retrieval.explain(owner,"trekking",10,RetrievalService.Mode.VECTOR).hits().stream().map(h->h.memory().id()).toList());
        assertThrows(IllegalArgumentException.class,()->indexing.get(other,indexing.run(owner,10).id()));
    }
    private void indexBothVersions() {indexing.run(owner,10);provider.version="v2";indexing.run(owner,10);}
    @Test void failuresHaveSafeFallbackAndDisabledCloudNeverReceivesText() {
        UUID id=add("I enjoy hiking.");provider.fail=true;
        var job=indexing.run(owner,10);assertEquals("FAILED",job.status());assertEquals(List.of("UNAVAILABLE"),job.errorCodes());
        var result=retrieval.explain(owner,"hiking",10,RetrievalService.Mode.VECTOR);
        assertEquals("UNAVAILABLE",result.vectorStatus());assertEquals(id,result.hits().getFirst().memory().id());
        provider.location=EmbeddingProvider.Location.CLOUD;int before=provider.calls;
        assertEquals("BLOCKED",indexing.run(owner,10).status());
        assertEquals("EGRESS_DENIED",retrieval.explain(owner,"hiking",10,RetrievalService.Mode.HYBRID).vectorStatus());assertEquals(before,provider.calls);
    }
    @Test void rankingAndEvidenceHydrationUseOneConsistentReadSnapshot() {
        UUID id=add("I enjoy hiking.");indexing.run(owner,10);
        provider.beforeReturn=()->{
            provider.beforeReturn=()->{};
            var concurrent=new org.springframework.transaction.support.TransactionTemplate(transactions);
            concurrent.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            concurrent.executeWithoutResult(status->jdbc.update("UPDATE memory_item SET content='I write fiction.' WHERE id=?",id));
        };
        var during=retrieval.explain(owner,"hiking",10,RetrievalService.Mode.HYBRID);
        assertEquals("I enjoy hiking.",during.hits().getFirst().memory().content());
        assertEquals("I enjoy hiking.",during.hits().getFirst().memory().evidence().getFirst().excerpt());
        assertEquals(0,vectors());
        assertTrue(retrieval.explain(owner,"hiking",10,RetrievalService.Mode.HYBRID).hits().isEmpty());
    }
    @Test void dimensionMismatchIsExplicitInJobsAndFallbackWithoutPersistingAnything() {
        UUID id=add("I enjoy hiking.");provider.dimensions=768;
        var job=indexing.run(owner,10);assertEquals("FAILED",job.status());assertEquals(1,job.failed());
        assertEquals(List.of("DIMENSION_MISMATCH"),job.errorCodes());assertEquals(0,vectors());
        var result=retrieval.explain(owner,"hiking",10,RetrievalService.Mode.HYBRID);
        assertEquals("DIMENSION_MISMATCH",result.vectorStatus());assertEquals(id,result.hits().getFirst().memory().id());
    }
    @Test void concurrentSavesHaveOneWinnerAndDoNotDuplicateVectors() throws Exception {
        add("I enjoy hiking.");var model=provider.model();var target=embeddings.pending(owner,model,10).getFirst();
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        var gate=new java.util.concurrent.CountDownLatch(1);float[] v=new float[1024];v[0]=1;
        try {
            var first=executor.submit(()->{gate.await();return embeddings.save(owner,target,model,v);});
            var second=executor.submit(()->{gate.await();return embeddings.save(owner,target,model,v);});gate.countDown();
            assertNotEquals(first.get(),second.get());assertEquals(1,vectors());
        } finally {executor.shutdownNow();}
    }
    @Test void evidenceEditsPartialSourceDeletionAndContentChangesInvalidateVectors() {
        UUID id=add("I enjoy hiking.");var original=repository.list(owner,null,10).getFirst().evidence().getFirst().sourceId();
        var source=jdbc.queryForObject("SELECT text_content FROM source_record WHERE id=?",String.class,original);
        UUID extra=UUID.randomUUID();jdbc.update("""
            INSERT INTO source_record(id,owner_id,source_system,source_locator,role,raw_message,text_content,content_hash)
            VALUES(?,?,'CHATGPT',?,'user','{}',?,repeat('c',64))
            """,extra,owner,"second:"+extra,source);
        jdbc.update("INSERT INTO memory_evidence(memory_id,source_id,excerpt) VALUES(?,?,?)",id,extra,source);
        indexing.run(owner,10);assertEquals(1,vectors());
        jdbc.update("UPDATE memory_evidence SET excerpt='hiking' WHERE memory_id=? AND source_id=?",id,extra);
        assertEquals(0,vectors());indexing.run(owner,10);
        memories.deleteSource(owner,extra);assertEquals(0,vectors());assertEquals(1,repository.list(owner,null,10).size());
        indexing.run(owner,10);assertEquals(1,vectors());
        jdbc.update("UPDATE memory_item SET content='I enjoy hiking outdoors.' WHERE id=?",id);assertEquals(0,vectors());
    }
    @Test void staleRowsCannotBypassDatabaseVisibilityEvenIfInsertedOutsideTheIndexer() {
        UUID id=add("I enjoy hiking.");indexing.run(owner,10);
        jdbc.update("UPDATE memory_item SET status='RETRACTED' WHERE id=?",id);
        float[] v=new float[1024];v[0]=1;
        jdbc.update("""
            INSERT INTO memory_embedding(id,owner_id,memory_id,model_id,model_version,memory_revision,vector_value)
            SELECT ?,owner_id,id,'controlled-offline','v1',embedding_revision,?::vector FROM memory_item WHERE id=?
            """,UUID.randomUUID(),EmbeddingVectors.sql(v),id);
        assertEquals(1,vectors());assertTrue(retrieval.explain(owner,"trekking",10,RetrievalService.Mode.VECTOR).hits().isEmpty());
        assertTrue(repository.search(owner,"hiking",10).isEmpty());
    }
    @Test void endpointsRetainTokenAuthenticationAndLegacyListShape() throws Exception {
        var client=java.net.http.HttpClient.newHttpClient();
        for(String path:List.of("/api/v1/embeddings/jobs","/api/v1/retrieval/explain?query=hiking")) {
            var builder=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+path));
            if(path.endsWith("jobs")) builder.POST(java.net.http.HttpRequest.BodyPublishers.noBody());
            assertEquals(401,client.send(builder.build(),java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
        }
        UUID configured=UUID.fromString("11111111-1111-4111-8111-111111111111");
        memories.addManual(configured,MemoryType.FACT,"I enjoy hiking.",Sensitivity.NORMAL,null);
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/v1/retrieval?query=hiking"))
                .header("X-Mirror-Token","0123456789abcdef0123456789abcdef").build();
        var response=client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(200,response.statusCode());
        var array=com.google.gson.JsonParser.parseString(response.body()).getAsJsonArray();
        assertTrue(array.get(0).getAsJsonObject().has("evidence"));assertTrue(array.get(0).getAsJsonObject().has("id"));
    }
}
