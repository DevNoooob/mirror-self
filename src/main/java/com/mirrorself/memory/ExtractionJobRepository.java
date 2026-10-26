package com.mirrorself.memory;

import com.google.gson.Gson;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ExtractionJobRepository {
    public record Job(UUID id, String status, String extractorVersion, String model,
            int sourceCount, int acceptedCount, int rejectedCount, int newMemories,
            List<String> errorCodes, List<UUID> sourceIds, Instant startedAt, Instant finishedAt) { }
    private final JdbcTemplate jdbc;
    public ExtractionJobRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Transactional public UUID start(UUID owner, MemoryExtractor extractor, List<UUID> sources) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO extraction_job(id,owner_id,extractor_version,model_id,status,source_count) VALUES(?,?,?,?,'RUNNING',?)",
                id,owner,extractor.version(),extractor.model(),sources.size());
        for(UUID source:new LinkedHashSet<>(sources)) jdbc.update(
                "INSERT INTO extraction_job_source(job_id,source_id) VALUES(?,?)",id,source);
        return id;
    }
    public void finish(UUID owner, UUID id, String status, int accepted, int rejected, int created, Set<String> errors) {
        jdbc.update("""
            UPDATE extraction_job SET status=?,accepted_count=?,rejected_count=?,new_memory_count=?,
                error_codes=?::jsonb,finished_at=now() WHERE id=? AND owner_id=?
            """,status,accepted,rejected,created,new Gson().toJson(errors),id,owner);
    }
    public Job get(UUID owner,UUID id) {
        List<UUID> sources=jdbc.query("""
            SELECT js.source_id FROM extraction_job_source js JOIN extraction_job j ON j.id=js.job_id
            JOIN source_record s ON s.id=js.source_id AND s.owner_id=j.owner_id
            WHERE j.id=? AND j.owner_id=? ORDER BY s.occurred_at NULLS LAST,s.imported_at,s.id
            """,(rs,row)->rs.getObject(1,UUID.class),id,owner);
        return jdbc.query("SELECT * FROM extraction_job WHERE id=? AND owner_id=?",(rs,row)-> {
            List<String> errors=new ArrayList<>();
            com.google.gson.JsonParser.parseString(rs.getString("error_codes")).getAsJsonArray()
                    .forEach(e->errors.add(e.getAsString()));
            OffsetDateTime finished=rs.getObject("finished_at",OffsetDateTime.class);
            return new Job(id,rs.getString("status"),rs.getString("extractor_version"),rs.getString("model_id"),
                    rs.getInt("source_count"),rs.getInt("accepted_count"),rs.getInt("rejected_count"),
                    rs.getInt("new_memory_count"),errors,sources,
                    rs.getObject("started_at",OffsetDateTime.class).toInstant(),finished==null?null:finished.toInstant());
        },id,owner).stream().findFirst().orElseThrow(()->new IllegalArgumentException("extraction job not found"));
    }
}
