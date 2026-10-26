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
    private final MemoryValidator validator;

    public MemoryService(MemoryRepository memories, SourceRepository sources, JdbcTemplate jdbc, MemoryValidator validator) {
        this.memories = memories;
        this.sources = sources;
        this.jdbc = jdbc;
        this.validator = validator;
    }

    /** Return count of NEW memory items; candidate evidence always links to a saved L0 record. */
    @Transactional
    public int addCandidates(UUID owner, UUID source, List<MemoryCandidate> candidates) {
        var storedSource=sources.get(owner,source);
        List<ExtractedMemory> extracted=candidates.stream().map(c->new ExtractedMemory(c.type(),c.content(),
                c.origin(),c.sensitivity(),ExtractedMemory.TemporalScope.SOURCE_TIME,
                List.of(new ExtractedMemory.EvidenceRef(source,c.exactEvidence())))).toList();
        return persistValidated(owner,null,"rule-legacy","none",extracted,List.of(storedSource));
    }

    @Transactional
    public int persistValidated(UUID owner, UUID job, String extractorVersion, String model,
            List<ExtractedMemory> valid, List<SourceRepository.SourceView> batch) {
        int created=0;
        // Serialize owner memory writes for canonical deduplication and symmetric conflict detection.
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?::text,0))",Object.class,owner.toString());
        for(var proposal:valid) {
            if(!validator.evidenceValid(owner,proposal,batch))
                throw new com.mirrorself.model.ModelFailure(com.mirrorself.model.ModelFailure.Code.INVALID_EVIDENCE);
            for(var ref:proposal.evidence()) {
                // Keep L0 present until the evidence insert commits; deletion uses the same row lock.
                jdbc.queryForObject("SELECT id FROM source_record WHERE id=? AND owner_id=? FOR KEY SHARE",
                        UUID.class,ref.sourceId(),owner);
            }
            var primary=proposal.evidence().stream().filter(e->e.quote().contains(proposal.content()))
                    .findFirst().orElse(proposal.evidence().getFirst());
            var source=sources.get(owner,primary.sourceId());
            Sensitivity sensitivity=proposal.sensitivity();
            for(var ref:proposal.evidence()) {
                var detected=SensitiveDetector.classify(sources.get(owner,ref.sourceId()).text());
                String sourceSensitivity=jdbc.queryForObject("SELECT sensitivity FROM source_record WHERE id=? AND owner_id=?",
                        String.class,ref.sourceId(),owner);
                if("RESTRICTED".equals(sourceSensitivity)&&detected==Sensitivity.NORMAL) detected=Sensitivity.SENSITIVE;
                if(detected.ordinal()>sensitivity.ordinal()) sensitivity=detected;
            }
            var contentSensitivity=SensitiveDetector.classify(proposal.content());
            if(contentSensitivity.ordinal()>sensitivity.ordinal()) sensitivity=contentSensitivity;
            // Inference deduplication is anchored to exact evidence, independent of model phrasing.
            String anchor=proposal.evidence().stream().map(e->e.sourceId()+":"+e.quote()).sorted()
                    .collect(java.util.stream.Collectors.joining("\n"));
            String fingerprint=com.mirrorself.common.Hashing.sha256(proposal.type()+"\n"+proposal.origin()+"\n"+anchor);
            List<UUID> existing=jdbc.query("SELECT id FROM memory_item WHERE owner_id=? AND extraction_fingerprint=?",
                    (rs,row)->rs.getObject(1,UUID.class),owner,fingerprint);
            MemoryRepository.Stored saved;
            if(!existing.isEmpty()) {
                UUID id=existing.getFirst();
                // Replays may tighten sensitivity but cannot revive rejected memories.
                if(sensitivity!=Sensitivity.NORMAL) jdbc.update("""
                    UPDATE memory_item SET sensitivity=CASE WHEN sensitivity='SECRET' THEN 'SECRET' ELSE ? END,
                        status=CASE WHEN status='ACTIVE' THEN 'PENDING_REVIEW' ELSE status END WHERE id=? AND owner_id=?
                    """,sensitivity.name(),id,owner);
                saved=new MemoryRepository.Stored(id,false,null);
            } else {
                var candidate=new MemoryCandidate(proposal.type(),proposal.content(),proposal.origin(),sensitivity,
                        source.occurredAt(),primary.quote());
                saved=memories.save(owner,primary.sourceId(),candidate);
                jdbc.update("""
                    UPDATE memory_item SET extraction_fingerprint=COALESCE(extraction_fingerprint,?),
                        temporal_scope=CASE WHEN temporal_scope='HISTORICAL' THEN 'HISTORICAL' ELSE ? END,
                        extractor_version=COALESCE(extractor_version,?),model_id=COALESCE(model_id,?),
                        extraction_job_id=COALESCE(extraction_job_id,?) WHERE id=? AND owner_id=?
                    """,fingerprint,proposal.temporalScope().name(),extractorVersion,model,job,saved.id(),owner);
            }
            for(var ref:proposal.evidence()) jdbc.update("""
                INSERT INTO memory_evidence(memory_id,source_id,excerpt) VALUES(?,?,?)
                    ON CONFLICT(memory_id,source_id) DO NOTHING
                """,saved.id(),ref.sourceId(),ref.quote());
            if(saved.inserted()) { created++; audit(owner,"MEMORY_CREATED","memory_item",saved.id()); }
            detectConflicts(owner,saved.id(),proposal.content());
        }
        return created;
    }

    private void detectConflicts(UUID owner,UUID id,String content) {
        jdbc.query("SELECT id,content FROM memory_item WHERE owner_id=? AND id<>? AND status NOT IN ('REJECTED','RETRACTED')",
                (rs,row)-> {
                    UUID other=rs.getObject("id",UUID.class);
                    if(MemoryConflicts.contradicts(content,rs.getString("content"))) {
                        UUID from=id.compareTo(other)<0?id:other, to=from.equals(id)?other:id;
                        jdbc.update("""
                            INSERT INTO memory_relation(owner_id,from_memory_id,to_memory_id,relation_type)
                            VALUES(?,?,?,'CONTRADICTS') ON CONFLICT DO NOTHING
                            """,owner,from,to);
                    }
                    return other;
                },owner,id);
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
        Sensitivity detected=SensitiveDetector.classify(text);
        if(detected.ordinal()>sensitivity.ordinal()) sensitivity=detected;
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
