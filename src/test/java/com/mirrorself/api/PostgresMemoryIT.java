package com.mirrorself.api;

import static org.junit.jupiter.api.Assertions.*;
import com.mirrorself.ingestion.IngestionUnitService;
import com.mirrorself.ingestion.SourceInput;
import com.mirrorself.memory.MemoryRepository;
import com.mirrorself.memory.MemoryService;
import com.mirrorself.memory.MemoryStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"mirror.owner-token=0123456789abcdef0123456789abcdef", "server.port=0"})
@Testcontainers(disabledWithoutDocker = true)
class PostgresMemoryIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg17")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mirror_self").withUsername("mirror").withPassword("test-password");

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @Test void extractionAndGrantEndpointsRetainOwnerAuthentication() throws Exception {
        var client=java.net.http.HttpClient.newHttpClient();
        for(String path:java.util.List.of("/api/v1/extractions","/api/v1/model-egress-grants","/api/v1/sources")) {
            var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+path))
                    .header("Content-Type","application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}" )).build();
            assertEquals(401,client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
        }
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/v1/extractions/"+UUID.randomUUID()))
                .header("X-Mirror-Token","0123456789abcdef0123456789abcdef").GET().build();
        assertEquals(400,client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
    }
    @Autowired IngestionUnitService ingestion;
    @Autowired MemoryRepository repo;
    @Autowired MemoryService memories;

    @Test void additiveMigrationPreservesV1Data() throws Exception {
        try(var connection=java.sql.DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
            var statement=connection.createStatement()) { statement.execute("CREATE DATABASE legacy_v1_test"); }
        String url=postgres.getJdbcUrl().replace("/mirror_self?","/legacy_v1_test?");
        if(url.equals(postgres.getJdbcUrl())) url=postgres.getJdbcUrl().replace("/mirror_self","/legacy_v1_test");
        var flyway=org.flywaydb.core.Flyway.configure().dataSource(url,postgres.getUsername(),postgres.getPassword()).target("1").load();
        flyway.migrate();
        var dataSource=new org.springframework.jdbc.datasource.DriverManagerDataSource(url,postgres.getUsername(),postgres.getPassword());
        var legacy=new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        UUID source=UUID.randomUUID(), memory=UUID.randomUUID(), owner=UUID.randomUUID();
        legacy.update("""
            INSERT INTO source_record(id,owner_id,source_system,source_locator,role,raw_message,text_content,content_hash)
            VALUES(?,?,'CHATGPT','legacy:m1','user','{"original":true}'::jsonb,'fictional original text',repeat('a',64))
            """,source,owner);
        legacy.update("""
            INSERT INTO memory_item(id,owner_id,memory_type,content,assertion_origin,status,sensitivity,dedupe_key)
            VALUES(?,?,'FACT','fictional legacy memory','USER_EXPLICIT','ACTIVE','NORMAL',repeat('b',64))
            """,memory,owner);
        legacy.update("INSERT INTO memory_evidence(memory_id,source_id,excerpt) VALUES(?,?,'fictional original text')",memory,source);
        org.flywaydb.core.Flyway.configure().dataSource(url,postgres.getUsername(),postgres.getPassword()).load().migrate();
        assertEquals("fictional original text",legacy.queryForObject("SELECT text_content FROM source_record WHERE id=?",String.class,source));
        assertEquals("fictional legacy memory",legacy.queryForObject("SELECT content FROM memory_item WHERE id=?",String.class,memory));
        assertEquals(1,legacy.queryForObject("SELECT count(*) FROM memory_evidence",Integer.class));
        assertEquals(2,legacy.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success",Integer.class));
    }

    @Test void preservesEvidenceAvoidsDuplicatesAndDeletesDerivedMemory() {
        UUID owner = UUID.fromString("11111111-1111-4111-8111-111111111111");
        SourceInput input = new SourceInput("CHATGPT", "test-c:" + UUID.randomUUID(),
                "test", "user", "{\"content\":\"test\"}",
                "我正在学习 Java 并发编程。", Instant.parse("2026-10-13T00:00:00Z"));
        assertEquals(1, ingestion.ingest(owner, input).newMemories());
        assertFalse(ingestion.ingest(owner, input).newSource());
        var hits = repo.search(owner, "Java", 10);
        assertTrue(hits.stream().anyMatch(m -> m.content().contains("Java")));
        var matched = hits.stream().filter(m -> m.content().contains("Java"))
                .findFirst().orElseThrow();
        assertFalse(matched.evidence().isEmpty());
        memories.deleteSource(owner, matched.evidence().getFirst().sourceId());
        assertTrue(repo.search(owner, "Java", 10).stream()
                .noneMatch(m -> m.id().equals(matched.id())));
    }
}
