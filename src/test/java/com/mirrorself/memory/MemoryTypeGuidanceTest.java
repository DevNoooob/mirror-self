package com.mirrorself.memory;

import com.google.gson.Gson;
import com.mirrorself.config.ExtractionProperties;
import com.mirrorself.ingestion.SourceRepository;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import com.mirrorself.model.ModelGateway;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MemoryTypeGuidanceTest {
    record Case(String text,MemoryType type,ExtractedMemory.TemporalScope time,MemoryStatus status) { }
    static Stream<Case> heldOut() {return Stream.of(
        new Case("上个季度我系统学习过虚构的青禾语言。",MemoryType.KNOWLEDGE,ExtractedMemory.TemporalScope.HISTORICAL,MemoryStatus.ACTIVE),
        new Case("我现在常用虚构的晨页记录灵感。",MemoryType.FACT,ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE),
        new Case("去年我组织过虚构社团的秋季展览。",MemoryType.EVENT,ExtractedMemory.TemporalScope.HISTORICAL,MemoryStatus.ACTIVE),
        new Case("我更喜欢先画草图再写方案。",MemoryType.OPINION,ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE),
        new Case("若明年有空，我可能考虑完成虚构的木星模型。",MemoryType.GOAL,ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.ACTIVE),
        new Case("我自认为做决定前会列出备选项。",MemoryType.THOUGHT_PROCESS,ExtractedMemory.TemporalScope.SOURCE_TIME,MemoryStatus.PENDING_REVIEW));}
    @ParameterizedTest @MethodSource("heldOut") void sharedTaxonomyReachesBothModelStagesAndKeepsIndependentAxes(Case c) {
        var gateway=mock(ModelGateway.class);var sources=mock(SourceRepository.class);UUID owner=UUID.randomUUID(),id=UUID.randomUUID();
        var source=new SourceView(id,"CHATGPT","TYPE_NEW:u1","fixture","user",c.text(),"{}",Instant.EPOCH);
        var candidate=new ExtractedMemory(c.type(),c.text(),AssertionOrigin.USER_EXPLICIT,Sensitivity.NORMAL,c.time(),List.of(new ExtractedMemory.EvidenceRef(id,c.text())));
        when(sources.get(owner,id)).thenReturn(source);
        when(gateway.generate(eq(owner),isNull(),anyString(),anyString(),anyList())).thenAnswer(inv->{
            String instructions=inv.getArgument(2,String.class);
            assertTrue(instructions.contains(MemoryTypeGuidance.INSTRUCTIONS));assertFalse(MemoryTypeGuidance.INSTRUCTIONS.contains(c.text()));
            assertFalse(instructions.contains("MQ032"));
            return instructions.startsWith("Extract")?"{\"memories\":"+new Gson().toJson(List.of(candidate))+"}":"{\"decisions\":[{\"index\":0,\"supported\":true}]}";
        });
        var properties=new ExtractionProperties(ExtractionProperties.Mode.LLM,"http://127.0.0.1/v1","","offline",Duration.ofSeconds(1),16000,30,true);
        var extracted=new LlmMemoryExtractor(gateway,properties).extract(owner,List.of(source),null);
        assertEquals(List.of(candidate),extracted);
        var validator=new MemoryValidator(sources,gateway);assertTrue(validator.evidenceValid(owner,candidate,List.of(source)));
        assertEquals(Set.of(0),validator.supported(owner,null,List.of(source),extracted));
        assertEquals(c.status(),MemoryPolicy.initialStatus(c.type(),candidate.origin(),candidate.sensitivity()));
        verify(gateway,times(2)).generate(eq(owner),isNull(),anyString(),anyString(),anyList());
    }
}
