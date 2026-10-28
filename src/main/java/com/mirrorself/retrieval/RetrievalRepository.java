package com.mirrorself.retrieval;

import com.mirrorself.embedding.*;
import com.mirrorself.memory.SearchTerms;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RetrievalRepository {
    public record Ranked(UUID id,double score) { }
    private final JdbcTemplate jdbc;
    public RetrievalRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<Ranked> keyword(UUID owner,String query,int limit) {
        var terms=SearchTerms.from(query);
        if(terms.isEmpty()) return List.of();
        var args=new ArrayList<Object>(); var cases=new ArrayList<String>(); var filters=new ArrayList<String>();
        for(String term:terms) { cases.add("CASE WHEN m.content ILIKE ? ESCAPE '!' THEN 1 ELSE 0 END");args.add(pattern(term)); }
        args.add(owner);
        for(String term:terms) { filters.add("m.content ILIKE ? ESCAPE '!'");args.add(pattern(term)); }
        args.add(limit);
        return jdbc.query("SELECT m.id,"+String.join(" + ",cases)+" AS score FROM memory_item m WHERE m.owner_id=? AND "
                +RetrievalVisibility.SQL+" AND ("+String.join(" OR ",filters)+") ORDER BY score DESC,m.id LIMIT ?",
                (rs,n)->new Ranked(rs.getObject(1,UUID.class),rs.getDouble(2)),args.toArray());
    }
    public List<Ranked> vector(UUID owner,float[] vector,EmbeddingProvider.Model model,int limit,double minSimilarity) {
        // Exact search over pre-filtered eligible rows. Avoid HNSW post-filter under-recall in M1.
        return jdbc.query("""
            WITH eligible AS MATERIALIZED (
                SELECT m.id,e.vector_value FROM memory_item m JOIN memory_embedding e
                    ON e.memory_id=m.id AND e.owner_id=m.owner_id AND e.memory_revision=m.embedding_revision
                WHERE m.owner_id=? AND e.model_id=? AND e.model_version=? AND
            """+RetrievalVisibility.SQL+"""
            ), scored AS (SELECT id,1-(vector_value <=> ?::vector) AS score FROM eligible)
            SELECT id,score FROM scored WHERE score>=? ORDER BY score DESC,id LIMIT ?
            """,(rs,n)->new Ranked(rs.getObject(1,UUID.class),rs.getDouble(2)),owner,model.id(),model.version(),
                EmbeddingVectors.sql(vector),minSimilarity,limit);
    }
    private static String pattern(String text) {
        return "%"+text.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
    }
}
