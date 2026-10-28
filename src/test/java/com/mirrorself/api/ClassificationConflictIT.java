package com.mirrorself.api;

import com.mirrorself.ingestion.*;
import com.mirrorself.memory.*;
import com.mirrorself.model.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.extraction.mode=RULE","server.port=0"})
class ClassificationConflictIT extends ExtractionPostgresTestSupport {
    @Autowired SourceRepository sources;@Autowired MemoryService service;@Autowired MemoryRepository memories;
    ExtractedMemory proposal(UUID source,String text,MemoryType type) {return new ExtractedMemory(type,text,AssertionOrigin.USER_EXPLICIT,Sensitivity.NORMAL,
        ExtractedMemory.TemporalScope.SOURCE_TIME,List.of(new ExtractedMemory.EvidenceRef(source,text)));}
    @Test void typeAwareStoragePrimitiveAloneCanCreateCrossTypeDuplicates() {
        UUID owner=UUID.randomUUID();String text="我学习过虚构的蓝帆课程。";
        UUID id=sources.save(owner,new SourceInput("CHATGPT","primitive:"+UUID.randomUUID(),"fixture","user","{}",text,Instant.EPOCH)).id();
        for(var type:List.of(MemoryType.FACT,MemoryType.KNOWLEDGE))
            assertTrue(memories.save(owner,id,new MemoryCandidate(type,text,AssertionOrigin.USER_EXPLICIT,Sensitivity.NORMAL,Instant.EPOCH,text)).inserted());
        var rows=memories.list(owner,null,100);assertEquals(2,rows.size());
        assertEquals(MemoryIdentity.of(rows.getFirst()),MemoryIdentity.of(rows.getLast()));
        // This reproduces the old primitive risk, NOT an assertion about the unobserved Live replay.
    }
    @Test void exactTypeDriftCannotOverwriteApprovedOrReviveRejectedMemory() {
        for(var decision:ReviewDecision.values()) {
            UUID owner=UUID.randomUUID();String text="我自认为选择虚构项目时比较谨慎。";
            UUID id=sources.save(owner,new SourceInput("CHATGPT","new-case:"+UUID.randomUUID(),"fixture","user","{}",text,Instant.EPOCH)).id();
            var source=sources.get(owner,id);var initial=proposal(id,text,MemoryType.THOUGHT_PROCESS);
            service.persistValidated(owner,null,"fixture","none",List.of(initial),List.of(source));
            var saved=memories.list(owner,null,100).getFirst();service.review(owner,saved.id(),decision);
            var before=memories.list(owner,null,100);
            var ex=assertThrows(ModelFailure.class,()->service.persistValidated(owner,null,"fixture","none",List.of(proposal(id,text,MemoryType.FACT)),List.of(source)));
            assertEquals(ModelFailure.Code.CLASSIFICATION_CONFLICT,ex.code());assertEquals(before,memories.list(owner,null,100));
        }
    }
    @Test void twoIndependentFactsFromOneSourceRemainSeparateAndConflictBatchRollsBack() {
        UUID owner=UUID.randomUUID();String first="我住在虚构的柳风镇。",second="我主要使用虚构的星页记事本。",third="我在虚构的小舟社团任职。";
        UUID id=sources.save(owner,new SourceInput("CHATGPT","separate:"+UUID.randomUUID(),"fixture","user","{}",first+second+third,Instant.EPOCH)).id();
        var source=sources.get(owner,id);var a=proposal(id,first,MemoryType.FACT);var b=proposal(id,second,MemoryType.FACT);
        assertEquals(2,service.persistValidated(owner,null,"fixture","none",List.of(a,b),List.of(source)));
        var before=memories.list(owner,null,100);
        assertEquals(0,service.persistValidated(owner,null,"fixture","none",List.of(a,b),List.of(source)));
        assertEquals(before,memories.list(owner,null,100));
        var cross=proposal(id,first,MemoryType.KNOWLEDGE);
        assertThrows(ModelFailure.class,()->service.persistValidated(owner,null,"fixture","none",List.of(proposal(id,third,MemoryType.FACT),cross),List.of(source)));
        assertEquals(before,memories.list(owner,null,100));
    }
}
