package com.mirrorself.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mirrorself.config.*;
import com.mirrorself.ingestion.SourceRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ModelAuthorizationTest {
    @Test void deniesCloudBeforeAnyProviderRequestEvenWithAKey() {
        var jdbc=mock(JdbcTemplate.class);var sources=mock(SourceRepository.class);
        UUID owner=UUID.randomUUID();
        var props=new ExtractionProperties(ExtractionProperties.Mode.LLM,"https://fictional.invalid/v1",
                "test-only-placeholder","fictional",Duration.ofMillis(100),16000,30,false);
        var auth=new ModelAuthorization(jdbc,new MirrorProperties(owner,"0123456789abcdef0123456789abcdef",false),props,sources);
        var gateway=new SpringAiModelGateway(props,auth);
        var source=new SourceRepository.SourceView(UUID.randomUUID(),"CHATGPT","c:1","test","user","test","{}",null);
        var ex=assertThrows(ModelFailure.class,()->gateway.generate(owner,UUID.randomUUID(),"instructions","private",List.of(source)));
        assertEquals(ModelFailure.Code.EGRESS_DENIED,ex.code());
        verifyNoInteractions(jdbc,sources);
    }
    @Test void localFlagCannotDisguiseRemoteEndpointAndPropertiesRedactCredentials() {
        assertThrows(IllegalArgumentException.class,()->new ExtractionProperties(ExtractionProperties.Mode.LLM,
                "http://fictional.invalid/v1","test","fictional",Duration.ofSeconds(1),16000,30,true));
        var props=new ExtractionProperties(ExtractionProperties.Mode.LLM,"https://fictional.invalid/v1",
                "test-only-placeholder","fictional",Duration.ofSeconds(1),16000,30,false);
        assertFalse(props.toString().contains("test-only-placeholder"));
    }
}
