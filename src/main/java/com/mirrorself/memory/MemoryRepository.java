package com.mirrorself.memory;

import com.mirrorself.common.Hashing;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryRepository {
    public record Stored(UUID id, boolean inserted, MemoryStatus status) { }
    public record Evidence(UUID sourceId, String locator, String excerpt,
                           String role, Instant occurredAt) { }
    public record MemoryView(UUID id, MemoryType type, String content, AssertionOrigin origin,
                             MemoryStatus status, Sensitivity sensitivity, Instant occurredAt,
                             List<Evidence> evidence, String temporalScope,
                             String extractorVersion, String modelId, List<UUID> conflicts) { }

    private final JdbcTemplate jdbc;
    public MemoryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Stored save(UUID owner, UUID sourceId, MemoryCandidate candidate) {
        MemoryStatus targetStatus = MemoryPolicy.initialStatus(
                candidate.type(), candidate.origin(), candidate.sensitivity());
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?::text,0))", Object.class, owner.toString());
        String canonical = MemoryNormalization.canonical(candidate.content());
        List<String> previous = jdbc.query("SELECT dedupe_key,content FROM memory_item WHERE owner_id=? AND memory_type=?",
                (rs,row) -> MemoryNormalization.canonical(rs.getString("content")).equals(canonical)
                        ? rs.getString("dedupe_key") : null, owner, candidate.type().name()).stream()
                .filter(java.util.Objects::nonNull).toList();
        String dedupe = previous.isEmpty() ? Hashing.sha256(candidate.type() + "\n" + canonical) : previous.getFirst();
        List<UUID> created = jdbc.query("""
                INSERT INTO memory_item
                    (id,owner_id,memory_type,content,assertion_origin,status,sensitivity,
                     dedupe_key,occurred_at,valid_from)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(owner_id,dedupe_key) DO NOTHING RETURNING id
                """, (rs, row) -> rs.getObject(1, UUID.class), UUID.randomUUID(), owner,
                candidate.type().name(), candidate.content(), candidate.origin().name(),
                targetStatus.name(), candidate.sensitivity().name(), dedupe,
                pgTime(candidate.occurredAt()), pgTime(candidate.occurredAt()));
        UUID id;
        if (!created.isEmpty()) id = created.getFirst();
        else {
            id = jdbc.queryForObject("""
                    SELECT id FROM memory_item WHERE owner_id=? AND dedupe_key=?
                    """, UUID.class, owner, dedupe);
            // A later sensitivity flag may tighten but must NEVER loosen privacy.
            if (candidate.sensitivity() != Sensitivity.NORMAL) {
                jdbc.update("""
                        UPDATE memory_item SET
                          sensitivity=CASE WHEN sensitivity='SECRET' THEN 'SECRET'
                             ELSE ? END,
                          status=CASE WHEN status='ACTIVE' THEN 'PENDING_REVIEW' ELSE status END,
                          updated_at=now()
                        WHERE id=? AND owner_id=?
                        """, candidate.sensitivity().name(), id, owner);
            }
        }
        jdbc.update("""
                INSERT INTO memory_evidence (memory_id,source_id,excerpt)
                VALUES (?,?,?) ON CONFLICT(memory_id,source_id) DO NOTHING
                """, id, sourceId, candidate.exactEvidence());
        MemoryStatus actual = jdbc.queryForObject(
                "SELECT status FROM memory_item WHERE id=? AND owner_id=?",
                (rs, row) -> MemoryStatus.valueOf(rs.getString(1)), id, owner);
        return new Stored(id, !created.isEmpty(), actual);
    }

    /** Called under the owner's write lock; no reclassification, merge or status change. */
    public boolean hasTypeConflict(UUID owner, ExtractedMemory proposal) {
        var key=MemoryIdentity.of(proposal);
        var prior=withEvidence(owner,jdbc.query("""
                SELECT id,memory_type,content,assertion_origin,status,sensitivity,occurred_at,
                       temporal_scope,extractor_version,model_id
                FROM memory_item WHERE owner_id=? AND content=? AND assertion_origin=?
                    AND temporal_scope=? AND memory_type<>?
                """,this::map,owner,proposal.content(),proposal.origin().name(),proposal.temporalScope().name(),proposal.type().name()));
        return prior.stream().anyMatch(m->MemoryIdentity.of(m).equals(key));
    }

    public List<MemoryView> list(UUID owner, MemoryStatus status, int max) {
        if (max < 1 || max > 200) throw new IllegalArgumentException("limit must be between 1 and 200");
        String sql = """
                SELECT id,memory_type,content,assertion_origin,status,sensitivity,occurred_at
                ,temporal_scope,extractor_version,model_id
                FROM memory_item WHERE owner_id=?
                """ + (status == null ? "" : " AND status=? ") +
                " ORDER BY created_at DESC,id LIMIT ?";
        Object[] args = status == null ? new Object[]{owner, max} : new Object[]{owner, status.name(), max};
        return withEvidence(owner, jdbc.query(sql, this::map, args));
    }

    public List<MemoryView> search(UUID owner, String query, int limit) {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("query cannot be blank");
        if (limit < 1 || limit > 30) throw new IllegalArgumentException("limit must be between 1 and 30");
        List<String> terms = SearchTerms.from(query);
        if (terms.isEmpty()) return List.of();
        List<Object> args = new ArrayList<>();
        List<String> cases = new ArrayList<>();
        List<String> filters = new ArrayList<>();
        for (String term : terms) {
            String condition = "content ILIKE ? ESCAPE '!'";
            cases.add("(CASE WHEN " + condition + " THEN 1 ELSE 0 END)");
            args.add("%" + escapeLike(term) + "%");
        }
        args.add(owner);
        for (String term : terms) {
            filters.add("content ILIKE ? ESCAPE '!'");
            args.add("%" + escapeLike(term) + "%");
        }
        args.add(limit);
        String sql = """
                SELECT id,memory_type,content,assertion_origin,status,sensitivity,occurred_at,
                temporal_scope,extractor_version,model_id,
                """ + String.join(" + ", cases) + " AS score " + """
                FROM memory_item WHERE owner_id=? AND status='ACTIVE' AND (
                """ + String.join(" OR ", filters) + ") ORDER BY score DESC, created_at DESC LIMIT ?";
        return withEvidence(owner, jdbc.query(sql, this::map, args.toArray()));
    }

    public MemoryStatus review(UUID owner, UUID id, ReviewDecision decision) {
        MemoryStatus old = jdbc.query("SELECT status FROM memory_item WHERE owner_id=? AND id=?",
                (rs, row) -> MemoryStatus.valueOf(rs.getString(1)), owner, id)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("memory not found"));
        MemoryStatus next = MemoryPolicy.reviewed(old, decision);
        int affected = jdbc.update("""
                UPDATE memory_item SET status=?,updated_at=now()
                WHERE owner_id=? AND id=? AND status='PENDING_REVIEW'
                """, next.name(), owner, id);
        if (affected != 1) throw new IllegalStateException("Concurrent modification; retry review");
        return next;
    }

    public boolean delete(UUID owner, UUID id) {
        return jdbc.update("DELETE FROM memory_item WHERE owner_id=? AND id=?", owner, id) == 1;
    }

    public boolean hasEvidence(UUID id) {
        Boolean value = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM memory_evidence WHERE memory_id=?)", Boolean.class, id);
        return Boolean.TRUE.equals(value);
    }

    private MemoryView map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        OffsetDateTime at = rs.getObject("occurred_at", OffsetDateTime.class);
        return new MemoryView(rs.getObject("id", UUID.class),
                MemoryType.valueOf(rs.getString("memory_type")), rs.getString("content"),
                AssertionOrigin.valueOf(rs.getString("assertion_origin")),
                MemoryStatus.valueOf(rs.getString("status")),
                Sensitivity.valueOf(rs.getString("sensitivity")),
                at == null ? null : at.toInstant(), List.of(), rs.getString("temporal_scope"),
                rs.getString("extractor_version"), rs.getString("model_id"), List.of());
    }

    private List<MemoryView> withEvidence(UUID owner, List<MemoryView> memories) {
        List<MemoryView> populated = new ArrayList<>();
        for (MemoryView memory : memories) {
            List<Evidence> evidence = jdbc.query("""
                    SELECT sr.id,sr.source_locator,me.excerpt,sr.role,sr.occurred_at
                    FROM memory_evidence me
                    JOIN source_record sr ON sr.id=me.source_id
                    WHERE me.memory_id=? AND sr.owner_id=? ORDER BY sr.occurred_at DESC NULLS LAST
                    """, (rs, row) -> {
                OffsetDateTime at = rs.getObject("occurred_at", OffsetDateTime.class);
                return new Evidence(rs.getObject("id", UUID.class), rs.getString("source_locator"),
                        rs.getString("excerpt"), rs.getString("role"),
                        at == null ? null : at.toInstant());
            }, memory.id(), owner);
            populated.add(new MemoryView(memory.id(), memory.type(), memory.content(),
                    memory.origin(), memory.status(), memory.sensitivity(),
                    memory.occurredAt(), evidence, memory.temporalScope(), memory.extractorVersion(), memory.modelId(),
                    jdbc.query("""
                        SELECT CASE WHEN from_memory_id=? THEN to_memory_id ELSE from_memory_id END
                        FROM memory_relation WHERE owner_id=? AND relation_type='CONTRADICTS'
                          AND (from_memory_id=? OR to_memory_id=?)
                        """, (rs,row)->rs.getObject(1,UUID.class),memory.id(),owner,memory.id(),memory.id())));
        }
        return populated;
    }

    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
    private static OffsetDateTime pgTime(Instant time) {
        return time == null ? null : OffsetDateTime.ofInstant(time, ZoneOffset.UTC);
    }
}
