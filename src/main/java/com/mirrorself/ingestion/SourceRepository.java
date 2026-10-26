package com.mirrorself.ingestion;

import com.mirrorself.common.Hashing;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SourceRepository {
    public record Stored(UUID id, boolean inserted) { }
    public record SourceView(UUID id, String sourceSystem, String locator, String title,
                             String role, String text, String rawJson, Instant occurredAt) { }

    private final JdbcTemplate jdbc;
    public SourceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Stored save(UUID owner, SourceInput input) {
        String hash = Hashing.sha256(input.rawJson());
        List<UUID> inserted = jdbc.query("""
                INSERT INTO source_record
                    (id,owner_id,source_system,source_locator,conversation_title,role,
                     raw_message,text_content,content_hash,occurred_at)
                VALUES (?,?,?,?,?,?,?::jsonb,?,?,?)
                ON CONFLICT(owner_id,source_system,source_locator,content_hash) DO NOTHING
                RETURNING id
                """, (rs, row) -> rs.getObject("id", UUID.class),
                UUID.randomUUID(), owner, input.sourceSystem(), input.locator(), input.title(),
                input.role(), input.rawJson(), input.text(), hash, pgTime(input.occurredAt()));
        if (!inserted.isEmpty()) return new Stored(inserted.getFirst(), true);
        UUID prior = jdbc.queryForObject("""
                SELECT id FROM source_record
                WHERE owner_id=? AND source_system=? AND source_locator=? AND content_hash=?
                """, UUID.class, owner, input.sourceSystem(), input.locator(), hash);
        return new Stored(prior, false);
    }

    public SourceView get(UUID owner, UUID id) {
        List<SourceView> sources = jdbc.query("""
                SELECT id,source_system,source_locator,conversation_title,role,
                       text_content,raw_message::text AS raw_json,occurred_at
                FROM source_record WHERE owner_id=? AND id=?
                """, (rs, row) -> new SourceView(
                rs.getObject("id", UUID.class), rs.getString("source_system"),
                rs.getString("source_locator"), rs.getString("conversation_title"),
                rs.getString("role"), rs.getString("text_content"), rs.getString("raw_json"),
                readTime(rs.getObject("occurred_at", OffsetDateTime.class))), owner, id);
        if (sources.isEmpty()) throw new IllegalArgumentException("source not found");
        return sources.getFirst();
    }

    public record SourceSummary(UUID id,String sourceSystem,String locator,String role,Instant occurredAt,
            String nodeId,String parentNodeId) { }
    public List<SourceSummary> list(UUID owner,int limit,int offset) {
        if(limit<1||limit>500||offset<0) throw new IllegalArgumentException("limit must be 1-500 and offset nonnegative");
        return jdbc.query("""
            SELECT id,source_system,source_locator,role,occurred_at,
                raw_message->>'id' AS node_id,raw_message->>'parent' AS parent_node_id
            FROM source_record WHERE owner_id=? ORDER BY imported_at,id LIMIT ? OFFSET ?
            """,(rs,row)->new SourceSummary(rs.getObject("id",UUID.class),rs.getString("source_system"),
                rs.getString("source_locator"),rs.getString("role"),readTime(rs.getObject("occurred_at",OffsetDateTime.class)),
                rs.getString("node_id"),rs.getString("parent_node_id")),owner,limit,offset);
    }

    public List<UUID> linkedMemories(UUID owner, UUID sourceId) {
        return jdbc.query("""
                SELECT DISTINCT me.memory_id FROM memory_evidence me
                JOIN source_record sr ON sr.id=me.source_id
                JOIN memory_item mi ON mi.id=me.memory_id
                WHERE sr.owner_id=? AND mi.owner_id=? AND sr.id=?
                """, (rs, row) -> rs.getObject(1, UUID.class), owner, owner, sourceId);
    }

    public boolean delete(UUID owner, UUID sourceId) {
        return jdbc.update("DELETE FROM source_record WHERE id=? AND owner_id=?", sourceId, owner) == 1;
    }

    private static OffsetDateTime pgTime(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
    private static Instant readTime(OffsetDateTime time) {
        return time == null ? null : time.toInstant();
    }
}
