package com.mirrorself.evaluation;

import java.util.*;

/** Ranked retrieval metrics only; no generated answer or semantic grader. */
public final class RetrievalMetrics {
    private RetrievalMetrics() { }
    public record Values(Double recallAtK,Double reciprocalRank,Double ndcgAtK,boolean correctEmpty) { }
    public static double precisionAtK(List<String> ranked,Map<String,Integer> relevance,int k) {
        if(k<1) throw new IllegalArgumentException("k must be positive");
        return (double)ranked.stream().distinct().limit(k).filter(relevance::containsKey).count()/k;
    }
    public static Values score(List<String> ranked,Map<String,Integer> relevance,int k) {
        if(k<1) throw new IllegalArgumentException("k must be positive");
        var hits=ranked.stream().distinct().limit(k).toList();
        if(relevance.isEmpty()) return new Values(null,null,null,hits.isEmpty());
        long found=hits.stream().filter(relevance::containsKey).count();double rr=0,dcg=0;
        for(int i=0;i<hits.size();i++) {int grade=relevance.getOrDefault(hits.get(i),0);
            if(grade>0 && rr==0) rr=1d/(i+1);
            dcg+=(Math.pow(2,grade)-1)/(Math.log(i+2)/Math.log(2));
        }
        var ideal=relevance.values().stream().sorted(Comparator.reverseOrder()).limit(k).toList();double idcg=0;
        for(int i=0;i<ideal.size();i++) idcg+=(Math.pow(2,ideal.get(i))-1)/(Math.log(i+2)/Math.log(2));
        return new Values((double)found/relevance.size(),rr,idcg==0?0:dcg/idcg,false);
    }
}
