package com.mirrorself.memory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mirrorself.ingestion.SourceInput;
import com.mirrorself.ingestion.SourceRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemoryService {
    private final MemoryRepository memories;
    private final SourceRepository sources;
    private final JdbcTemplate jdbc;
    private final Gson gson = new Gson();

    public MemoryService(MemoryRepository memories, SourceRepository sources, JdbcTemplate jdbc) {
        this.memories = memories;
        this.sources = sources;
        this.jdbc = jdbc;
    }

    /** Return count of NEW memory items; candidate evidence always links to a saved L0 record. */
    @Transactional
    public int addCandidates(UUID owner, UUID source, List<MemoryCandidate> candidates) {
        int count = 0;
        for (MemoryCandidate original : candidates) {
            Sensitivity sensitivity = original.sensitivity() == Sensitivity.NORMAL
                    ? SensitiveDetector.classify(original.content()) : original.sensitivity();
            MemoryCandidate checked = new MemoryCandidate(original.type(), original.content(),
                    original.origin(), sensitivity, original.occurredAt(), original.exactEvidence());
            MemoryRepository.Stored stored = memories.save(owner, source, checked);
            if (stored.inserted()) {
                count++;
                audit(owner, "MEMORY_CREATED", "memory_item", stored.id());
            }
        }
        return count;
    }

    /** Owner-asserted memory still gets an L0 evidence record (it is never source-less). */
    @Transactional
    public MemoryRepository.Stored addManual(UUID owner, MemoryType type, String text,
                                             Sensitivity sensitivity, Instant occurredAt) {
        if (text == null || text.isBlank() || text.length() > 3000) {
            throw new IllegalArgumentException("text must contain 1-3000 characters");
        }
        JsonObject raw = new JsonObject();
        raw.addProperty("role", "user");
        raw.addProperty("text", text);
        SourceInput input = new SourceInput("MANUAL", "manual:" + UUID.randomUUID(),
                "User-authored memory", "user", gson.toJson(raw), text, occurredAt);
        SourceRepository.Stored source = sources.save(owner, input);
        MemoryCandidate candidate = new MemoryCandidate(type, text, AssertionOrigin.USER_EXPLICIT,
                sensitivity, occurredAt, text);
        MemoryRepository.Stored memory = memories.save(owner, source.id(), candidate);
        audit(owner, "MEMORY_SUBMITTED", "memory_item", memory.id());
        return memory;
    }

    @Transactional
    public MemoryStatus review(UUID owner, UUID id, ReviewDecision decision) {
        MemoryStatus next = memories.review(owner, id, decision);
        audit(owner, "MEMORY_" + decision.name(), "memory_item", id);
        return next;
    }

    @Transactional
    public void deleteMemory(UUID owner, UUID id) {
        // Manual submissions have dedicated L0 records; erase them together
        // when they are not evidence for any other memory.
        List<UUID> orphanableManualSources = jdbc.query("""
                SELECT sr.id FROM source_record sr
                JOIN memory_evidence me ON me.source_id=sr.id
                WHERE me.memory_id=? AND sr.owner_id=? AND sr.source_system='MANUAL'
                  AND NOT EXISTS (
                    SELECT 1 FROM memory_evidence other
                    WHERE other.source_id=sr.id AND other.memory_id<>?
                  )
                """, (rs, row) -> rs.getObject(1, UUID.class), id, owner, id);
        if (!memories.delete(owner, id)) throw new IllegalArgumentException("memory not found");
        for (UUID sourceId : orphanableManualSources) sources.delete(owner, sourceId);
        // Claims inferred solely from the removed evidence cannot remain valid.
        jdbc.update("""
                DELETE FROM identity_claim ic WHERE owner_id=? AND assertion_origin='MODEL_INFERRED'
                  AND NOT EXISTS(SELECT 1 FROM identity_evidence ie WHERE ie.identity_claim_id=ic.id)
                """, owner);
        audit(owner, "MEMORY_DELETED", "memory_item", id);
    }

    @Transactional
    public void deleteSource(UUID owner, UUID sourceId) {
        sources.get(owner, sourceId); // owner-scoped existence check
        List<UUID> impacted = sources.linkedMemories(owner, sourceId);
        sources.delete(owner, sourceId);
        for (UUID memoryId : impacted) {
            if (!memories.hasEvidence(memoryId)) memories.delete(owner, memoryId);
        }
        jdbc.update("""
                DELETE FROM identity_claim ic WHERE owner_id=? AND assertion_origin='MODEL_INFERRED'
                  AND NOT EXISTS(SELECT 1 FROM identity_evidence ie WHERE ie.identity_claim_id=ic.id)
                """, owner);
        audit(owner, "SOURCE_DELETED", "source_record", sourceId);
    }

    private void audit(UUID owner, String action, String entityType, UUID id) {
        jdbc.update("""
                INSERT INTO audit_event(id,owner_id,action,entity_type,entity_id)
                VALUES (?,?,?,?,?)
                """, UUID.randomUUID(), owner, action, entityType, id);
    }
}
