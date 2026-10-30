package com.mirrorself.evaluation;

import com.mirrorself.retrieval.evidence.EvidenceSupportJudge.Relation;
import java.util.*;

/** v3 metric contract; relevance bands do not supply entailment labels. */
public final class RetrievalRelevanceBands {
    private RetrievalRelevanceBands() { }
    public record Band(RetrievalMetrics.Values ranking,double precisionAtK,int referenceCount) { }
    public record Result(Band primary,Band timeContextPendingReview,Band background,int correctDirect,int totalDirect) { }
    public static Result score(List<String> ranked,Map<String,Integer> grades,Map<String,Relation> expected,
                               Map<String,Relation> predicted,int k) {
        int total=0,correct=0;
        for(var e:expected.entrySet()) if(e.getValue()==Relation.SUPPORTS||e.getValue()==Relation.REFUTES) {
            total++;if(e.getValue()==predicted.get(e.getKey())) correct++;
        }
        return new Result(band(ranked,grades,3,k),band(ranked,grades,2,k),band(ranked,grades,1,k),correct,total);
    }
    private static Band band(List<String> ranked,Map<String,Integer> grades,int level,int k) {
        var refs=new LinkedHashMap<String,Integer>();grades.forEach((id,g)->{if(g==level) refs.put(id,g);});
        return new Band(RetrievalMetrics.score(ranked,refs,k),RetrievalMetrics.precisionAtK(ranked,refs,k),refs.size());
    }
}
