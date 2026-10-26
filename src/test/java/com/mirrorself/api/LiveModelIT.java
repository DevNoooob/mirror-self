package com.mirrorself.api;

import static org.junit.jupiter.api.Assertions.*;
import com.mirrorself.config.ExtractionProperties;
import com.mirrorself.ingestion.*;
import com.mirrorself.memory.*;
import com.mirrorself.model.ModelAuthorization;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Explicitly opt-in: only fictional text, but a real provider may charge for two calls. */
@EnabledIfEnvironmentVariable(named="MIRROR_RUN_LIVE_LLM",matches="true")
@SpringBootTest(properties={"mirror.owner-token=0123456789abcdef0123456789abcdef",
        "mirror.extraction.mode=LLM","mirror.model-egress-enabled=true","server.port=0"})
class LiveModelIT extends ExtractionPostgresTestSupport {
    @Autowired SourceRepository sources;
    @Autowired MemoryExtractionService extraction;
    @Autowired MemoryRepository memories;
    @Autowired ModelAuthorization authorization;
    @Autowired ExtractionProperties properties;
    @Test void extractsFictionalMultiTurnEvidenceWithRealModel() {
        UUID owner=UUID.fromString("11111111-1111-4111-8111-111111111111");
        String conversation="live-fictional-"+UUID.randomUUID();
        List<UUID> ids=new ArrayList<>();
        String[] roles={"user","assistant","user"};
        String[] texts={"我住在虚构的星海城。","你平时喜欢做什么？","我喜欢阅读科幻小说。"};
        for(int i=0;i<texts.length;i++) ids.add(sources.save(owner,new SourceInput("CHATGPT",conversation+":m"+i,
                "fictional smoke",roles[i],"{}",texts[i],Instant.parse("2020-01-01T00:00:00Z").plusSeconds(i))).id());
        UUID grant=properties.local()?null:authorization.grant(owner,ids,Instant.now().plusSeconds(600),false);
        var job=extraction.extract(owner,ids,grant);
        assertTrue(Set.of("COMPLETE","PARTIAL").contains(job.status()),job.errorCodes().toString());
        assertTrue(job.newMemories()>0);
        assertTrue(memories.list(owner,null,200).stream().anyMatch(m->m.evidence().stream()
                .anyMatch(e->ids.contains(e.sourceId())&&"user".equals(e.role())&&e.excerpt().contains("我"))));
    }
}
