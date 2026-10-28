package com.mirrorself.embedding;

import com.google.gson.Gson;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Explicit bounded incremental jobs; no startup backfill or network inside memory-write transactions. */
@Service
public class EmbeddingService {
    public record Job(UUID id,String modelId,String modelVersion,String status,int selected,int inserted,
                      int skipped,int failed,List<String> errorCodes) { }
    private final EmbeddingGateway gateway;
    private final EmbeddingRepository repository;
    private final JdbcTemplate jdbc;
    public EmbeddingService(EmbeddingGateway gateway,EmbeddingRepository repository,JdbcTemplate jdbc) {
        this.gateway=gateway;this.repository=repository;this.jdbc=jdbc;
    }
    public Job run(UUID owner,int limit) {
        if(limit<1 || limit>100) throw new IllegalArgumentException("limit must be 1-100");
        UUID id=UUID.randomUUID(); var model=gateway.model();
        jdbc.update("INSERT INTO embedding_job(id,owner_id,model_id,model_version,status) VALUES(?,?,?,?,'RUNNING')",
                id,owner,model.id(),model.version());
        int selected=0,inserted=0,skipped=0,failed=0;
        Set<String> errors=new TreeSet<>(); String status="COMPLETE";
        try {
            gateway.check();
            var targets=repository.pending(owner,model,limit);selected=targets.size();
            for(var target:targets) {
                try {
                    if(repository.save(owner,target,model,gateway.embed(target.content()))) inserted++;
                    else { skipped++;errors.add("STALE_OR_ALREADY_INDEXED"); }
                } catch(EmbeddingFailure ex) { failed++;errors.add(ex.code().name()); }
            }
            if(failed>0) status=inserted>0?"PARTIAL":"FAILED";
        } catch(EmbeddingFailure ex) { status="BLOCKED";errors.add(ex.code().name()); }
        catch(RuntimeException ex) {
            // No DB/provider exception message may cross the job boundary.
            status="FAILED";errors.add("INDEX_STORAGE_FAILURE");
        }
        jdbc.update("""
            UPDATE embedding_job SET status=?,selected_count=?,inserted_count=?,skipped_count=?,failed_count=?,
                error_codes=?::jsonb,finished_at=now() WHERE id=? AND owner_id=?
            """,status,selected,inserted,skipped,failed,new Gson().toJson(errors),id,owner);
        return get(owner,id);
    }
    public Job get(UUID owner,UUID id) {
        return jdbc.query("""
            SELECT * FROM embedding_job WHERE owner_id=? AND id=?
            """,(rs,n)->new Job(id,rs.getString("model_id"),rs.getString("model_version"),rs.getString("status"),
                rs.getInt("selected_count"),rs.getInt("inserted_count"),rs.getInt("skipped_count"),rs.getInt("failed_count"),
                List.of(new Gson().fromJson(rs.getString("error_codes"),String[].class))),owner,id)
                .stream().findFirst().orElseThrow(()->new IllegalArgumentException("embedding job not found"));
    }
}
