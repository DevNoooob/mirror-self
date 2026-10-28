package com.mirrorself.consolidation;

import com.mirrorself.embedding.EmbeddingGateway;
import com.mirrorself.retrieval.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ConsolidationRecall {
    public enum Mode { KEYWORD, HYBRID }
    public record Neighbor(ConsolidationRepository.Target target,RrfRanking.Score score) { }
    public record Batch(ConsolidationRepository.Target anchor,List<Neighbor> neighbors,String modelId,String modelVersion) { }
    private final ConsolidationRepository candidates;
    private final RetrievalRepository retrieval;
    private final EmbeddingGateway embedding;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public ConsolidationRecall(ConsolidationRepository candidates,RetrievalRepository retrieval,EmbeddingGateway embedding,org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.candidates=candidates;this.retrieval=retrieval;this.embedding=embedding;this.jdbc=jdbc;
    }
    public String modelKey() {return embedding.model().id()+"\n"+embedding.model().version();}
    public long vectorEpoch(UUID owner) {
        var rows=jdbc.query("SELECT epoch FROM consolidation_vector_epoch WHERE owner_id=? AND model_id=? AND model_version=?",
            (rs,n)->rs.getLong(1),owner,embedding.model().id(),embedding.model().version());return rows.isEmpty()?0:rows.getFirst();
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Batch recall(UUID owner,UUID anchorId,int limit,Mode mode) {
        var anchors=candidates.targets(owner,List.of(anchorId));
        if(anchors.isEmpty()) return null;
        var anchor=anchors.getFirst();var model=embedding.model();
        String query=anchor.memory().content();if(query.length()>1000) query=query.substring(0,1000); // recall hint only; classifier checks full original.
        var keywords=retrieval.keyword(owner,query,100).stream().filter(r->!r.id().equals(anchorId)).toList();
        var vectors=mode==Mode.HYBRID?retrieval.storedNeighbors(owner,anchorId,model,100):List.<RetrievalRepository.Ranked>of();
        var ranked=RrfRanking.rank(keywords,vectors).stream().limit(limit).toList();
        var targets=candidates.targets(owner,ranked.stream().map(RrfRanking.Score::id).toList());
        var byId=targets.stream().collect(java.util.stream.Collectors.toMap(t->t.memory().id(),t->t));
        var neighbors=ranked.stream().filter(r->byId.containsKey(r.id())).limit(limit).map(r->new Neighbor(byId.get(r.id()),r)).toList();
        return new Batch(anchor,neighbors,model.id(),model.version());
    }
}
