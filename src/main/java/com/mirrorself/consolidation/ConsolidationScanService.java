package com.mirrorself.consolidation;

import com.google.gson.Gson;
import com.mirrorself.common.Hashing;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Explicit bounded scans. Existing stored vectors only: zero model requests. */
@Service
public class ConsolidationScanService {
    public record Job(UUID id,String status,int selected,int processed,int comparedPairs,int created,int existing,int abstained,int stale,
                      String engineVersion,String mode,String modelId,String modelVersion,List<String> errorCodes,int modelRequests,long vectorEpoch) { }
    private final ConsolidationRepository repository;
    private final ConsolidationRecall recall;
    private final ConsolidationWriter writer;
    private final JdbcTemplate jdbc;
    public ConsolidationScanService(ConsolidationRepository repository,ConsolidationRecall recall,ConsolidationWriter writer,JdbcTemplate jdbc) {
        this.repository=repository;this.recall=recall;this.writer=writer;this.jdbc=jdbc;
    }
    public Job run(UUID owner,int limit,int neighbors,ConsolidationRecall.Mode mode) {
        if(limit<1||limit>50||neighbors<1||neighbors>20||mode==null)
            throw new IllegalArgumentException("limit must be 1-50, neighbors 1-20 and mode required");
        long epoch=mode==ConsolidationRecall.Mode.HYBRID?recall.vectorEpoch(owner):0;
        UUID id=UUID.randomUUID();String[] model=recall.modelKey().split("\n",-1);
        String key=Hashing.sha256(ConsolidationWriter.ENGINE_VERSION+"\n"+ConsolidationClassifier.VERSION+"\n"+recall.modelKey()+"\n"+mode+"\n"+neighbors);
        jdbc.update("INSERT INTO consolidation_job(id,owner_id,status,scan_key,engine_version,mode,model_id,model_version,vector_epoch) VALUES(?,?,'RUNNING',?,?,?,?,?,?)",
                id,owner,key,ConsolidationWriter.ENGINE_VERSION,mode.name(),model[0],model[1],epoch);
        int selected=0,processed=0,compared=0,created=0,existing=0,abstained=0,stale=0;
        Set<String> errors=new TreeSet<>();
        try {
            var anchors=repository.pending(owner,key,epoch,limit);selected=anchors.size();
            for(var anchor:anchors) {
                try {
                    var batch=recall.recall(owner,anchor.id(),neighbors,mode);
                    if(batch==null || batch.anchor().revision()!=anchor.revision()) {stale++;continue;}
                    for(var neighbor:batch.neighbors()) {
                        compared++;var result=writer.propose(owner,batch,neighbor);
                        switch(result.kind()) {
                            case "CREATED" -> created++;case "EXISTING" -> existing++;case "STALE" -> stale++;default -> abstained++;
                        }
                    }
                    if(repository.markProcessed(owner,anchor,key,epoch)) processed++;else stale++;
                } catch(RuntimeException ex) {errors.add("SCAN_UNIT_FAILED");}
            }
        } catch(RuntimeException ex) {errors.add("SCAN_STORAGE_FAILED");}
        String status=errors.isEmpty()?"COMPLETE":processed>0?"PARTIAL":"FAILED";
        jdbc.update("""
            UPDATE consolidation_job SET status=?,selected_count=?,processed_count=?,compared_pairs=?,created_count=?,existing_count=?,
                abstained_count=?,stale_count=?,error_codes=?::jsonb,finished_at=now() WHERE owner_id=? AND id=?
            """,status,selected,processed,compared,created,existing,abstained,stale,new Gson().toJson(errors),owner,id);
        return get(owner,id);
    }
    public Job get(UUID owner,UUID id) {
        return jdbc.query("SELECT * FROM consolidation_job WHERE owner_id=? AND id=?",(rs,n)->new Job(id,rs.getString("status"),rs.getInt("selected_count"),
            rs.getInt("processed_count"),rs.getInt("compared_pairs"),rs.getInt("created_count"),rs.getInt("existing_count"),rs.getInt("abstained_count"),
            rs.getInt("stale_count"),rs.getString("engine_version"),rs.getString("mode"),rs.getString("model_id"),rs.getString("model_version"),
            List.of(new Gson().fromJson(rs.getString("error_codes"),String[].class)),0,rs.getLong("vector_epoch")),owner,id).stream().findFirst()
                .orElseThrow(()->new IllegalArgumentException("consolidation job not found"));
    }
}
