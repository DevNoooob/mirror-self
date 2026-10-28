package com.mirrorself.retrieval;

import java.util.*;

public final class RrfRanking {
    private RrfRanking() { }
    public static final int CONSTANT=60;
    public record Score(UUID id,double score,Integer keywordRank,Double keywordScore,Integer vectorRank,Double cosineSimilarity) { }
    public static List<Score> rank(List<RetrievalRepository.Ranked> keyword,List<RetrievalRepository.Ranked> vector) {
        Map<UUID,Score> scores=new HashMap<>();
        for(int i=0;i<keyword.size();i++) {
            var m=keyword.get(i);scores.put(m.id(),new Score(m.id(),1d/(CONSTANT+i+1),i+1,m.score(),null,null));
        }
        for(int i=0;i<vector.size();i++) {
            var m=vector.get(i);var old=scores.get(m.id());
            scores.put(m.id(),new Score(m.id(),(old==null?0:old.score())+1d/(CONSTANT+i+1),
                    old==null?null:old.keywordRank(),old==null?null:old.keywordScore(),i+1,m.score()));
        }
        return scores.values().stream().sorted(Comparator.comparingDouble(Score::score).reversed()
                .thenComparing(Score::id)).toList();
    }
}
