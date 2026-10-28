package com.mirrorself.api;

import com.mirrorself.ingestion.*;
import com.mirrorself.model.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.extraction.mode=LLM",
        "mirror.extraction.local=false","mirror.model-egress-enabled=true","mirror.extraction.model=fictional-clock",
        "mirror.extraction.base-url=https://clock-test.invalid/v1","mirror.extraction.api-key=","server.port=0"})
class AuthorizationClockIT extends ExtractionPostgresTestSupport {
    @Autowired ModelAuthorization authorization;@Autowired SourceRepository sources;@Autowired JdbcTemplate jdbc;
    @Test void relativeExpiryAndExplicitSourceTimeRoundTripInTemporaryDatabaseWithoutModelCalls() {
        UUID owner=UUID.randomUUID();Instant sourceTime=Instant.parse("2005-06-07T08:09:10Z");
        UUID id=sources.save(owner,new SourceInput("CHATGPT","clock:"+UUID.randomUUID(),"fixture","user","{}","虚构时间回归案例",sourceTime)).id();
        Instant expires=Instant.now().plusSeconds(600);
        UUID grant=authorization.grant(owner,List.of(id),expires,false);
        var stored=jdbc.queryForObject("SELECT expires_at FROM model_egress_grant WHERE id=?",(rs,n)->rs.getObject(1,OffsetDateTime.class).toInstant(),grant);
        assertTrue(Math.abs(Duration.between(expires,stored).toNanos())<1000);
        double remaining=jdbc.queryForObject("SELECT extract(epoch from (expires_at-now())) FROM model_egress_grant WHERE id=?",Double.class,grant);
        assertTrue(remaining>590&&remaining<=601,"Same-host relative TTL should be approximately 600 seconds");
        assertDoesNotThrow(()->authorization.check(owner,grant,List.of(sources.get(owner,id))));
        jdbc.update("UPDATE model_egress_grant SET expires_at=now()-interval '1 second' WHERE id=?",grant);
        assertEquals(ModelFailure.Code.EGRESS_DENIED,assertThrows(ModelFailure.class,()->authorization.check(owner,grant,List.of(sources.get(owner,id)))).code());
        assertEquals(sourceTime,sources.get(owner,id).occurredAt());
    }
}
