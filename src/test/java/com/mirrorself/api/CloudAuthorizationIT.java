package com.mirrorself.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mirrorself.ingestion.*;
import com.mirrorself.memory.*;
import com.mirrorself.model.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties={"mirror.owner-token=0123456789abcdef0123456789abcdef", "mirror.extraction.mode=LLM",
        "mirror.model-egress-enabled=true","mirror.extraction.local=false",
        "mirror.extraction.base-url=https://fictional.invalid/v1","mirror.extraction.model=fictional-cloud","server.port=0"})
class CloudAuthorizationIT extends ExtractionPostgresTestSupport {
    @Autowired SourceRepository sources;
    @Autowired MemoryExtractionService extraction;
    @Autowired ModelAuthorization authorization;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean ModelGateway gateway;
    final UUID owner=UUID.fromString("11111111-1111-4111-8111-111111111111");
    UUID source(String text) {
        return sources.save(owner,new SourceInput("CHATGPT","c-"+UUID.randomUUID()+":m1","fictional","user","{}",text,Instant.now())).id();
    }
    @Test void missingExpiredRevokedAndMismatchedGrantsBlockAllProviderCalls() {
        UUID id=source("我喜欢阅读");
        assertEquals("BLOCKED",extraction.extract(owner,List.of(id),null).status());
        UUID other=source("我喜欢咖啡");
        UUID grant=authorization.grant(owner,List.of(other),Instant.now().plusSeconds(60),false);
        assertEquals("BLOCKED",extraction.extract(owner,List.of(id),grant).status());
        grant=authorization.grant(owner,List.of(id),Instant.now().plusSeconds(60),false);
        jdbc.update("UPDATE model_egress_grant SET expires_at=now()-interval '1 second' WHERE id=?",grant);
        assertEquals("BLOCKED",extraction.extract(owner,List.of(id),grant).status());
        grant=authorization.grant(owner,List.of(id),Instant.now().plusSeconds(60),false);authorization.revoke(owner,grant);
        assertEquals("BLOCKED",extraction.extract(owner,List.of(id),grant).status());
        verifyNoInteractions(gateway);
    }
    @Test void grantsAreOwnerProviderAndSensitivityScoped() {
        UUID id=source("我在医院接受诊断");
        UUID grant=authorization.grant(owner,List.of(id),Instant.now().plusSeconds(60),false);
        assertEquals("BLOCKED",extraction.extract(owner,List.of(id),grant).status());
        UUID secret=source("我的密码是 fictional-placeholder");
        UUID secretGrant=authorization.grant(owner,List.of(secret),Instant.now().plusSeconds(60),true);
        assertEquals("BLOCKED",extraction.extract(owner,List.of(secret),secretGrant).status());
        UUID normal=source("我喜欢阅读");
        UUID wrongProvider=authorization.grant(owner,List.of(normal),Instant.now().plusSeconds(60),false);
        jdbc.update("UPDATE model_egress_grant SET provider_key=repeat('0',64) WHERE id=?",wrongProvider);
        assertEquals("BLOCKED",extraction.extract(owner,List.of(normal),wrongProvider).status());
        assertThrows(IllegalArgumentException.class,()->authorization.grant(UUID.randomUUID(),List.of(normal),Instant.now().plusSeconds(60),false));
        verifyNoInteractions(gateway);
    }
    @Test void explicitValidGrantAllowsOnlyItsSource() {
        UUID id=source("我喜欢阅读");
        UUID grant=authorization.grant(owner,List.of(id),Instant.now().plusSeconds(60),false);
        when(gateway.generate(any(),any(),anyString(),anyString(),anyList())).thenReturn("{\"memories\":[]}");
        assertEquals("COMPLETE",extraction.extract(owner,List.of(id),grant).status());
        verify(gateway,times(1)).generate(eq(owner),eq(grant),anyString(),anyString(),anyList());
    }
}
