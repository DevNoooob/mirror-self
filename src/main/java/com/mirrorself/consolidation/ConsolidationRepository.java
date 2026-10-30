package com.mirrorself.consolidation;

import com.google.gson.*;
import com.mirrorself.memory.*;
import com.mirrorself.memory.MemoryRepository.MemoryView;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConsolidationRepository {
    public record Target(MemoryView memory,long revision,Instant validFrom,Instant validTo,UUID supersededBy) { }
    private record Revision(long value,Instant validFrom,Instant validTo,UUID supersededBy) { }
    public record Candidate(UUID id,UUID a,UUID b,long aRevision,long bRevision,ConsolidationRelation relation,String direction,
        String status,String validity,long reviewVersion,String engineVersion,String modelId,String modelVersion,
        List<String> reasons,List<String> uncertainties,JsonObject discovery) { }
    public record RelationConflict(UUID candidateId,ConsolidationRelation relation,String status,String engineVersion) { }
    public record Saved(UUID id,boolean inserted) { }
    public record Anchor(UUID id,long revision) { }
    private final JdbcTemplate jdbc;
    private final MemoryRepository memories;
    private final Gson gson=new Gson();
    public ConsolidationRepository(JdbcTemplate jdbc,MemoryRepository memories) {this.jdbc=jdbc;this.memories=memories;}
    public static final String CURRENT="""
        c.validity='VALID' AND consolidation_memory_visible(c.owner_id,c.memory_a_id,c.a_revision)
        AND consolidation_memory_visible(c.owner_id,c.memory_b_id,c.b_revision)
        """;
    public List<Anchor> pending(UUID owner,String key,long epoch,int limit) {
        return jdbc.query("SELECT m.id,m.embedding_revision FROM memory_item m WHERE m.owner_id=? AND "+ConsolidationVisibility.SQL+"""
            AND NOT EXISTS(SELECT 1 FROM consolidation_cursor x WHERE x.owner_id=m.owner_id AND x.memory_id=m.id
                AND x.scan_key=? AND x.memory_revision=m.embedding_revision AND x.vector_epoch=?)
            ORDER BY m.created_at,m.id LIMIT ?
            """,(rs,n)->new Anchor(rs.getObject(1,UUID.class),rs.getLong(2)),owner,key,epoch,limit);
    }
    public List<Target> targets(UUID owner,List<UUID> ids) {
        if(ids.isEmpty()) return List.of();
        var args=new ArrayList<Object>();args.add(owner);args.addAll(ids);
        Map<UUID,Revision> revisions=new HashMap<>();
        jdbc.query("SELECT m.id,m.embedding_revision,m.valid_from,m.valid_to,m.superseded_by FROM memory_item m WHERE m.owner_id=? AND "+ConsolidationVisibility.SQL
            +" AND m.id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")",(rs,n)->{
                revisions.put(rs.getObject(1,UUID.class),new Revision(rs.getLong(2),instant(rs,"valid_from"),instant(rs,"valid_to"),rs.getObject("superseded_by",UUID.class)));return 0;
            },args.toArray());
        return memories.retrievable(owner,ids.stream().filter(revisions::containsKey).toList()).stream().filter(m->revisions.containsKey(m.id()))
            .map(m->{var r=revisions.get(m.id());return new Target(m,r.value(),r.validFrom(),r.validTo(),r.supersededBy());}).toList();
    }
    private static Instant instant(ResultSet rs,String name) throws SQLException {
        var value=rs.getTimestamp(name);return value==null?null:value.toInstant();
    }
    public void lockMemories(UUID owner,UUID a,UUID b) {
        jdbc.query("SELECT id FROM memory_item WHERE owner_id=? AND id IN (?,?) ORDER BY id FOR UPDATE",
            (rs,n)->rs.getObject(1,UUID.class),owner,a,b);
    }
    public boolean markProcessed(UUID owner,Anchor anchor,String key,long epoch) {
        return jdbc.update("""
            INSERT INTO consolidation_cursor(owner_id,memory_id,scan_key,memory_revision,vector_epoch)
            SELECT m.owner_id,m.id,?,m.embedding_revision,? FROM memory_item m
            WHERE m.owner_id=? AND m.id=? AND m.embedding_revision=? AND
            """+ConsolidationVisibility.SQL+"""
            ON CONFLICT(owner_id,memory_id,scan_key) DO UPDATE SET memory_revision=EXCLUDED.memory_revision,vector_epoch=EXCLUDED.vector_epoch,processed_at=now()
            """,key,epoch,owner,anchor.id(),anchor.revision())==1;
    }
    public Saved save(UUID owner,Target a,Target b,ConsolidationClassifier.Proposal p,String model,String version,JsonObject discovery) {
        var inserted=jdbc.query("""
            INSERT INTO consolidation_candidate(id,owner_id,memory_a_id,memory_b_id,a_revision,b_revision,
                relation_type,direction,engine_version,discovery_model_id,discovery_model_version,reason_codes,uncertainties,discovery)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?::jsonb,?::jsonb)
            ON CONFLICT(owner_id,memory_a_id,memory_b_id,relation_type,a_revision,b_revision) DO NOTHING RETURNING id
            """,(rs,n)->rs.getObject(1,UUID.class),UUID.randomUUID(),owner,a.memory().id(),b.memory().id(),a.revision(),b.revision(),
            p.relation().name(),p.direction(),ConsolidationWriter.ENGINE_VERSION,model,version,gson.toJson(p.reasons()),gson.toJson(p.uncertainties()),gson.toJson(discovery));
        if(!inserted.isEmpty()) {
            UUID id=inserted.getFirst();audit(owner,"CONSOLIDATION_PROPOSED",id);
            if(!conflicts(owner,get(owner,id)).isEmpty()) audit(owner,"CONSOLIDATION_RELATION_CONFLICT",id);
            return new Saved(id,true);
        }
        UUID id=jdbc.queryForObject("""
            SELECT id FROM consolidation_candidate WHERE owner_id=? AND memory_a_id=? AND memory_b_id=?
                AND relation_type=? AND a_revision=? AND b_revision=?
            """,UUID.class,owner,a.memory().id(),b.memory().id(),p.relation().name(),a.revision(),b.revision());
        return new Saved(id,false);
    }
    public Candidate get(UUID owner,UUID id) {
        return jdbc.query("SELECT c.* FROM consolidation_candidate c WHERE c.owner_id=? AND c.id=? AND "+CURRENT,
                this::map,owner,id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("candidate not found or no longer eligible"));
    }
    public List<Candidate> list(UUID owner,String status,int limit,int offset) {
        if(limit<1||limit>100||offset<0) throw new IllegalArgumentException("limit must be 1-100 and offset nonnegative");
        if(status!=null&&!Set.of("PENDING_REVIEW","APPROVED","REJECTED").contains(status)) throw new IllegalArgumentException("invalid candidate status");
        var args=new ArrayList<Object>();args.add(owner);if(status!=null) args.add(status);args.add(limit);args.add(offset);
        return jdbc.query("SELECT c.* FROM consolidation_candidate c WHERE c.owner_id=? AND "+CURRENT
                +(status==null?"":" AND c.status=? ")+" ORDER BY c.created_at DESC,c.id LIMIT ? OFFSET ?",this::map,args.toArray());
    }
    // Only current, owner-visible, non-dismissed hypotheses can block approval.
    public List<RelationConflict> conflicts(UUID owner,Candidate candidate) {
        if("REJECTED".equals(candidate.status())) return List.of();
        return jdbc.query("SELECT c.id,c.relation_type,c.status,c.engine_version FROM consolidation_candidate c WHERE c.owner_id=? "
                +"AND c.memory_a_id=? AND c.memory_b_id=? AND c.a_revision=? AND c.b_revision=? "
                +"AND c.id<>? AND c.relation_type<>? AND c.status<>'REJECTED' AND "+CURRENT+" ORDER BY c.id",
            (rs,n)->new RelationConflict(rs.getObject(1,UUID.class),ConsolidationRelation.valueOf(rs.getString(2)),rs.getString(3),rs.getString(4)),
            owner,candidate.a(),candidate.b(),candidate.aRevision(),candidate.bRevision(),candidate.id(),candidate.relation().name());
    }
    public void review(UUID owner,Candidate before,ReviewDecision decision,long version) {
        if(decision==ReviewDecision.APPROVE&&!conflicts(owner,before).isEmpty())
            throw new IllegalStateException("RELATION_TYPE_CONFLICT: resolve conflicting hypotheses before approval");
        int changed=jdbc.update("UPDATE consolidation_candidate c SET status=?,review_version=review_version+1,reviewed_at=now(),updated_at=now() "
            +"WHERE c.owner_id=? AND c.id=? AND c.status='PENDING_REVIEW' AND c.review_version=? AND "+CURRENT+"""
            AND (?='REJECT' OR NOT EXISTS(SELECT 1 FROM consolidation_candidate rival
                WHERE rival.owner_id=c.owner_id AND rival.memory_a_id=c.memory_a_id AND rival.memory_b_id=c.memory_b_id
                  AND rival.a_revision=c.a_revision AND rival.b_revision=c.b_revision
                  AND rival.validity='VALID' AND rival.status<>'REJECTED' AND rival.relation_type<>c.relation_type))
            """,
            decision==ReviewDecision.APPROVE?"APPROVED":"REJECTED",owner,before.id(),version,decision.name());
        if(changed!=1) throw new IllegalStateException("candidate changed; reload before review");
        audit(owner,"CONSOLIDATION_"+decision.name(),before.id());
    }
    private Candidate map(ResultSet rs,int n) throws SQLException {
        return new Candidate(rs.getObject("id",UUID.class),rs.getObject("memory_a_id",UUID.class),rs.getObject("memory_b_id",UUID.class),
            rs.getLong("a_revision"),rs.getLong("b_revision"),ConsolidationRelation.valueOf(rs.getString("relation_type")),rs.getString("direction"),
            rs.getString("status"),rs.getString("validity"),rs.getLong("review_version"),rs.getString("engine_version"),rs.getString("discovery_model_id"),
            rs.getString("discovery_model_version"),List.of(gson.fromJson(rs.getString("reason_codes"),String[].class)),
            List.of(gson.fromJson(rs.getString("uncertainties"),String[].class)),JsonParser.parseString(rs.getString("discovery")).getAsJsonObject());
    }
    private void audit(UUID owner,String action,UUID id) {
        jdbc.update("INSERT INTO audit_event(id,owner_id,action,entity_type,entity_id) VALUES(?,?,?,'consolidation_candidate',?)",UUID.randomUUID(),owner,action,id);
    }
}
