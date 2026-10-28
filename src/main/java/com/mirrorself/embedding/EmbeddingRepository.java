package com.mirrorself.embedding;

import com.mirrorself.retrieval.RetrievalVisibility;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EmbeddingRepository {
    public record Target(UUID id,String content,long revision) { }
    private final JdbcTemplate jdbc;
    public EmbeddingRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<Target> pending(UUID owner,EmbeddingProvider.Model model,int limit) {
        return jdbc.query("""
            SELECT m.id,m.content,m.embedding_revision FROM memory_item m
            WHERE m.owner_id=? AND
            """+RetrievalVisibility.SQL+"""
            AND NOT EXISTS(SELECT 1 FROM memory_embedding e WHERE e.memory_id=m.id AND e.owner_id=m.owner_id
                AND e.model_id=? AND e.model_version=? AND e.memory_revision=m.embedding_revision)
            ORDER BY m.created_at,m.id LIMIT ?
            """,(rs,n)->new Target(rs.getObject(1,UUID.class),rs.getString(2),rs.getLong(3)),owner,model.id(),model.version(),limit);
    }
    @Transactional
    public boolean save(UUID owner,Target target,EmbeddingProvider.Model model,float[] vector) {
        // Serialize with status/evidence/source invalidation before checking freshness.
        jdbc.query("SELECT id FROM memory_item WHERE owner_id=? AND id=? FOR UPDATE",
                (rs,n)->rs.getObject(1,UUID.class),owner,target.id());
        return jdbc.update("""
            INSERT INTO memory_embedding(id,owner_id,memory_id,model_id,model_version,memory_revision,vector_value)
            SELECT ?,m.owner_id,m.id,?,?,m.embedding_revision,?::vector FROM memory_item m
            WHERE m.owner_id=? AND m.id=? AND m.embedding_revision=? AND
            """+RetrievalVisibility.SQL+"""
            ON CONFLICT(owner_id,memory_id,model_id,model_version) WHERE memory_id IS NOT NULL DO NOTHING
            """,UUID.randomUUID(),model.id(),model.version(),EmbeddingVectors.sql(vector),owner,target.id(),target.revision())==1;
    }
}
