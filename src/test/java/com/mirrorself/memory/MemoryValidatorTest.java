package com.mirrorself.memory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mirrorself.ingestion.SourceRepository;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import com.mirrorself.model.ModelGateway;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class MemoryValidatorTest {
    final SourceRepository sources=mock(SourceRepository.class);
    final ModelGateway gateway=mock(ModelGateway.class);
    final MemoryValidator validator=new MemoryValidator(sources,gateway);
    final UUID owner=UUID.randomUUID(),id=UUID.randomUUID();
    SourceView source(String role,String text) {
        var s=new SourceView(id,"CHATGPT","c:1","test",role,text,"{}",Instant.parse("2020-01-01T00:00:00Z"));
        when(sources.get(owner,id)).thenReturn(s); return s;
    }
    ExtractedMemory proposal(String text,UUID ref,String quote,AssertionOrigin origin) {
        return new ExtractedMemory(origin==AssertionOrigin.MODEL_INFERRED?MemoryType.THOUGHT_PROCESS:MemoryType.FACT,
                text,origin,Sensitivity.NORMAL,ExtractedMemory.TemporalScope.SOURCE_TIME,
                List.of(new ExtractedMemory.EvidenceRef(ref,quote)));
    }
    @Test void verifiesActualOwnerMessageAndExactQuote() {
        var s=source("user","我正在学习 Java。");
        assertTrue(validator.evidenceValid(owner,proposal("我正在学习 Java",id,"我正在学习 Java",AssertionOrigin.USER_EXPLICIT),List.of(s)));
        assertFalse(validator.evidenceValid(owner,proposal("我已获得博士学位",id,"我正在学习 Java",AssertionOrigin.USER_EXPLICIT),List.of(s)));
        assertFalse(validator.evidenceValid(owner,proposal("不存在",id,"不存在",AssertionOrigin.USER_EXPLICIT),List.of(s)));
        assertFalse(validator.evidenceValid(owner,proposal("我正在学习 Java",UUID.randomUUID(),"我正在学习 Java",AssertionOrigin.USER_EXPLICIT),List.of(s)));
    }
    @Test void rejectsAssistantFactsAndCrossOwnerEvidence() {
        var s=source("assistant","你已经搬到北京了");
        assertFalse(validator.evidenceValid(owner,proposal(s.text(),id,s.text(),AssertionOrigin.USER_EXPLICIT),List.of(s)));
        s=source("user","我喜欢咖啡");
        when(sources.get(owner,id)).thenThrow(new IllegalArgumentException("source not found"));
        assertFalse(validator.evidenceValid(owner,proposal(s.text(),id,s.text(),AssertionOrigin.USER_EXPLICIT),List.of(s)));
    }
    @Test void doesNotLetLongMessageSlicesCiteUnsentText() {
        var stored=source("user","我喜欢咖啡。我住在上海。");
        var sent=new SourceView(id,"CHATGPT","c:1","test","user","我喜欢咖啡。","{}",stored.occurredAt());
        assertFalse(validator.evidenceValid(owner,proposal("我住在上海",id,"我住在上海",AssertionOrigin.USER_EXPLICIT),List.of(sent)));
    }
    @Test void rejectsQuestionsAndQuotesThatDropHistoricalQualifiers() {
        var s=source("user","我以前认为咖啡不好。");
        assertFalse(validator.evidenceValid(owner,proposal("咖啡不好",id,"咖啡不好",AssertionOrigin.USER_EXPLICIT),List.of(s)));
        assertFalse(validator.evidenceValid(owner,proposal("我以前认为咖啡不好",id,"我以前认为咖啡不好",AssertionOrigin.USER_EXPLICIT),List.of(s)));
        s=source("user","我应该学习 Java 吗？");
        assertFalse(validator.evidenceValid(owner,proposal("我应该学习 Java 吗",id,"我应该学习 Java 吗",AssertionOrigin.USER_EXPLICIT),List.of(s)));
    }
    @Test void inferenceCannotClaimInventedFactOrBypassReview() {
        var s=source("user","我会比较多个方案再作决定");
        var inferred=proposal("可能倾向于先比较方案",id,s.text(),AssertionOrigin.MODEL_INFERRED);
        assertTrue(validator.evidenceValid(owner,inferred,List.of(s)));
        assertEquals(MemoryStatus.PENDING_REVIEW,MemoryPolicy.initialStatus(inferred.type(),inferred.origin(),inferred.sensitivity()));
        var bad=new ExtractedMemory(MemoryType.FACT,"出生在虚构城市",AssertionOrigin.MODEL_INFERRED,
                Sensitivity.NORMAL,ExtractedMemory.TemporalScope.SOURCE_TIME,inferred.evidence());
        assertFalse(validator.evidenceValid(owner,bad,List.of(s)));
    }
}
