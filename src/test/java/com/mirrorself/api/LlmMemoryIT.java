package com.mirrorself.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.google.gson.*;
import com.mirrorself.ingestion.*;
import com.mirrorself.memory.*;
import com.mirrorself.model.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties={"mirror.owner-token=0123456789abcdef0123456789abcdef",
        "mirror.extraction.mode=LLM","mirror.extraction.local=true",
        "mirror.extraction.base-url=http://127.0.0.1:9999/v1","mirror.extraction.model=fictional-mock","server.port=0"})
class LlmMemoryIT extends ExtractionPostgresTestSupport {
    @Autowired ChatGptImportService importer;
    @Autowired SourceRepository sources;
    @Autowired MemoryRepository memories;
    @Autowired MemoryService memoryService;
    @Autowired MemoryExtractionService extraction;
    @Autowired ExtractionJobRepository jobs;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean ModelGateway gateway;
    final UUID owner=UUID.fromString("11111111-1111-4111-8111-111111111111");
    final AtomicInteger revision=new AtomicInteger();
    @BeforeEach void model() {
        when(gateway.generate(any(),any(),anyString(),anyString(),anyList())).thenAnswer(inv->{
            String instructions=inv.getArgument(2);
            if(instructions.startsWith("Validate")) {
                JsonArray proposals=JsonParser.parseString(inv.getArgument(3,String.class)).getAsJsonObject().getAsJsonArray("proposals");
                JsonArray decisions=new JsonArray();
                for(int i=0;i<proposals.size();i++) {
                    var row=new JsonObject();row.addProperty("index",i);row.addProperty("supported",true);decisions.add(row);
                }
                var response=new JsonObject();response.add("decisions",decisions);return response.toString();
            }
            List<SourceRepository.SourceView> batch=inv.getArgument(4);
            List<ExtractedMemory> result=new ArrayList<>();
            for(var source:batch) {
                if(!"user".equals(source.role())) {
                    if("assistant".equals(source.role())) result.add(proposal(source,source.text(),MemoryType.FACT,
                            AssertionOrigin.USER_EXPLICIT,source.text()));
                    continue;
                }
                for(String sentence:source.text().split("[。]")) {
                    if(sentence.startsWith("我会")) result.add(proposal(source,"可能倾向于比较方案 "+revision.incrementAndGet(),
                            MemoryType.THOUGHT_PROCESS,AssertionOrigin.MODEL_INFERRED,sentence));
                    else if(!sentence.isBlank()) result.add(proposal(source,sentence,
                            sentence.startsWith("我喜欢")||sentence.startsWith("我不喜欢")||sentence.startsWith("我以前认为")
                                    ?MemoryType.OPINION:sentence.startsWith("我的目标")?MemoryType.GOAL:MemoryType.FACT,
                            AssertionOrigin.USER_EXPLICIT,sentence));
                }
            }
            JsonObject root=new JsonObject();root.add("memories",new Gson().toJsonTree(result));return root.toString();
        });
    }
    static ExtractedMemory proposal(SourceRepository.SourceView source,String content,MemoryType type,
            AssertionOrigin origin,String quote) {
        return new ExtractedMemory(type,content,origin,Sensitivity.NORMAL,
                quote.startsWith("我以前")?ExtractedMemory.TemporalScope.HISTORICAL:ExtractedMemory.TemporalScope.SOURCE_TIME,
                List.of(new ExtractedMemory.EvidenceRef(source.id(),quote)));
    }
    String conversation(String id,String user,String assistant,String last) {
        JsonObject mapping=new JsonObject();
        String[] roles={"user","assistant","user"},texts={user,assistant,last};
        for(int i=0;i<3;i++) {
            JsonObject node=new JsonObject();node.addProperty("id","n"+i);
            if(i>0) node.addProperty("parent","n"+(i-1));
            JsonObject message=new JsonObject();message.addProperty("id","m"+i);message.addProperty("create_time",1600000000+i);
            JsonObject author=new JsonObject();author.addProperty("role",roles[i]);message.add("author",author);
            JsonArray parts=new JsonArray();parts.add(texts[i]);JsonObject content=new JsonObject();content.add("parts",parts);
            message.add("content",content);node.add("message",message);mapping.add("n"+i,node);
        }
        JsonObject root=new JsonObject();root.addProperty("id",id);root.addProperty("current_node","n2");root.add("mapping",mapping);
        JsonArray all=new JsonArray();all.add(root);return all.toString();
    }
    ChatGptImportService.ImportReport load(String json) throws IOException {
        return importer.load(owner,"fictional.json",new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }
    @Test void importsMultiTurnValidatesPersistsRetrievesAndDeduplicates() throws Exception {
        String json=conversation("fictional-"+UUID.randomUUID(),"我住在上海。","建议你完成北京马拉松。",
                "我会先比较多个方案再决定。我的目标是学习 Java。");
        var report=load(json);
        assertEquals(3,report.importedSources());assertEquals(3,report.newMemories());
        var job=jobs.get(owner,report.extractionJobIds().getFirst());
        assertEquals("PARTIAL",job.status());assertEquals(1,job.rejectedCount());
        assertTrue(job.errorCodes().contains("INVALID_EVIDENCE"));
        var hit=memories.search(owner,"上海",30).stream().filter(m->m.evidence().stream()
                .anyMatch(e->job.sourceIds().contains(e.sourceId()))).findFirst().orElseThrow();
        assertEquals("user",hit.evidence().getFirst().role());assertEquals("我住在上海",hit.evidence().getFirst().excerpt());
        assertEquals("fictional-mock",hit.modelId());assertNotNull(hit.extractorVersion());
        var pending=memories.list(owner,MemoryStatus.PENDING_REVIEW,200).stream()
                .filter(m->m.evidence().stream().anyMatch(e->job.sourceIds().contains(e.sourceId()))).findFirst().orElseThrow();
        assertEquals(AssertionOrigin.MODEL_INFERRED,pending.origin());
        memoryService.review(owner,pending.id(),ReviewDecision.REJECT);
        var replay=extraction.extract(owner,job.sourceIds(),null);
        assertEquals(0,replay.newMemories());
        assertEquals(MemoryStatus.REJECTED,memories.list(owner,MemoryStatus.REJECTED,200).stream()
                .filter(m->m.id().equals(pending.id())).findFirst().orElseThrow().status());
        var duplicate=load(json);assertEquals(0,duplicate.importedSources());assertEquals(0,duplicate.newMemories());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM identity_claim",Integer.class));
    }
    @Test void rejectsNonexistentEvidenceWithoutSavingAnything() {
        UUID id=save("fake-"+UUID.randomUUID(),"我喜欢阅读");
        var ref=new ExtractedMemory.EvidenceRef(UUID.randomUUID(),"不存在的经历");
        var memory=new ExtractedMemory(MemoryType.FACT,"不存在的经历",AssertionOrigin.USER_EXPLICIT,Sensitivity.NORMAL,
                ExtractedMemory.TemporalScope.SOURCE_TIME,List.of(ref));
        when(gateway.generate(any(),any(),anyString(),anyString(),anyList()))
                .thenReturn("{\"memories\":"+new Gson().toJson(List.of(memory))+"}");
        var job=extraction.extract(owner,List.of(id),null);
        assertEquals(0,job.newMemories());assertEquals(1,job.rejectedCount());
        assertTrue(sources.get(owner,id).text().contains("阅读"));
    }
    @Test void invalidJsonAndTimeoutLeaveL0AndOldMemoriesIntact() {
        UUID id=save("failure-"+UUID.randomUUID(),"我喜欢虚构课程");
        int before=jdbc.queryForObject("SELECT count(*) FROM memory_item",Integer.class);
        when(gateway.generate(any(),any(),anyString(),anyString(),anyList())).thenReturn("{broken");
        var bad=extraction.extract(owner,List.of(id),null);assertEquals("FAILED",bad.status());
        assertEquals(List.of("INVALID_MODEL_OUTPUT"),bad.errorCodes());
        when(gateway.generate(any(),any(),anyString(),anyString(),anyList()))
                .thenThrow(new ModelFailure(ModelFailure.Code.MODEL_UNAVAILABLE));
        var timeout=extraction.extract(owner,List.of(id),null);assertEquals("FAILED",timeout.status());
        assertEquals(List.of("MODEL_UNAVAILABLE"),timeout.errorCodes());
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM memory_item",Integer.class));
        assertNotNull(sources.get(owner,id));
    }
    @Test void sensitiveCandidatesStayPendingAndConflictsPreserveBothRecords() throws Exception {
        var report=load(conversation("privacy-"+UUID.randomUUID(),"我喜欢咖啡。","", "我不喜欢咖啡。我在医院接受诊断。"));
        var job=jobs.get(owner,report.extractionJobIds().getFirst());
        var mine=memories.list(owner,null,200).stream().filter(m->m.evidence().stream()
                .anyMatch(e->job.sourceIds().contains(e.sourceId()))).toList();
        assertEquals(3,mine.size());assertTrue(mine.stream().anyMatch(m->!m.conflicts().isEmpty()));
        var sensitive=mine.stream().filter(m->m.content().contains("医院")).findFirst().orElseThrow();
        assertEquals(MemoryStatus.PENDING_REVIEW,sensitive.status());
        assertEquals(Sensitivity.SENSITIVE,sensitive.sensitivity());
    }
    @Test void historicalViewsKeepOriginalStatementTime() throws Exception {
        var report=load(conversation("history-"+UUID.randomUUID(),"我以前认为咖啡不好。","",""));
        var job=jobs.get(owner,report.extractionJobIds().getFirst());
        var hit=memories.list(owner,null,200).stream().filter(m->m.evidence().stream()
                .anyMatch(e->job.sourceIds().contains(e.sourceId()))).findFirst().orElseThrow();
        assertEquals("HISTORICAL",hit.temporalScope());assertEquals(Instant.ofEpochSecond(1600000000),hit.occurredAt());
    }
    @Test void validatorFailureOrSemanticRejectionCannotPersistCandidate() {
        UUID id=save("semantic-"+UUID.randomUUID(),"我喜欢阅读");
        when(gateway.generate(any(),any(),startsWith("Validate"),anyString(),anyList()))
                .thenReturn("{\"decisions\":[{\"index\":0,\"supported\":false}]}");
        assertEquals(0,extraction.extract(owner,List.of(id),null).newMemories());
        when(gateway.generate(any(),any(),startsWith("Validate"),anyString(),anyList())).thenReturn("not json");
        assertEquals("FAILED",extraction.extract(owner,List.of(id),null).status());
    }
    @Test void persistenceBatchRollsBackIfAnyEvidenceFails() {
        UUID id=save("atomic-"+UUID.randomUUID(),"我喜欢阅读独特的虚构故事");
        var source=sources.get(owner,id);
        var valid=proposal(source,source.text(),MemoryType.FACT,AssertionOrigin.USER_EXPLICIT,source.text());
        var invalid=new ExtractedMemory(MemoryType.FACT,"虚构事实",AssertionOrigin.USER_EXPLICIT,Sensitivity.NORMAL,
                ExtractedMemory.TemporalScope.SOURCE_TIME,List.of(new ExtractedMemory.EvidenceRef(UUID.randomUUID(),"虚构事实")));
        int before=jdbc.queryForObject("SELECT count(*) FROM memory_item",Integer.class);
        assertThrows(ModelFailure.class,()->memoryService.persistValidated(owner,null,"test","fictional",
                List.of(valid,invalid),List.of(source)));
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM memory_item",Integer.class));
    }
    @Test void concurrentExtractionProducesOnlyOneMemory() throws Exception {
        UUID id=save("concurrent-"+UUID.randomUUID(),"我喜欢并发测试的虚构课程");
        try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var first=executor.submit(()->extraction.extract(owner,List.of(id),null));
            var second=executor.submit(()->extraction.extract(owner,List.of(id),null));
            assertEquals(1,first.get().newMemories()+second.get().newMemories());
        }
    }
    UUID save(String conversation,String text) {
        return sources.save(owner,new SourceInput("CHATGPT",conversation+":m1","fictional","user","{}",text,Instant.now())).id();
    }
}
