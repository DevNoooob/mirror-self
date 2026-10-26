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

@SpringBootTest(properties = {"mirror.owner-token=0123456789abcdef0123456789abcdef", "server.port=0"})
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

    @Autowired IngestionUnitService ingestion;
    @Autowired MemoryRepository repo;
    @Autowired MemoryService memories;

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
