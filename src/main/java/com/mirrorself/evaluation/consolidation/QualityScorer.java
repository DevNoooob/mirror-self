package com.mirrorself.evaluation.consolidation;

import com.google.gson.JsonObject;
import com.mirrorself.consolidation.ConsolidationRelation;
import java.util.*;

/** Exact type AND direction scoring, after discovery is finished. No reference is passed to the engine. */
public final class QualityScorer {
    private QualityScorer() { }
    public record Attempt(String a,String b,ConsolidationRelation relation,String fromKey,String reason,int evidenceTotal,int evidenceErrors,boolean pendingOnly) { }
    public record Query(String anchor,List<String> keyword,List<String> vector,List<String> fused) { }
    public record Observation(String caseId,String split,String mode,boolean atK,boolean at3,boolean vectorAtK,
        List<Query> queries,List<Attempt> attempts,int ownerViolations,int privacyViolations) { }
    public static Map<String,Object> score(List<Observation> rows,Map<String,QualityCorpus.Answer> refs,JsonObject gates,double conflictAccuracy) {
        int positive=0,recalled=0,at3=0,vector=0,tp=0,fp=0,fn=0,conditionalFn=0,sameErrors=0,unrelated=0,evidence=0,errors=0,policy=0,policyCorrect=0,owner=0,privacy=0,unrelatedOpportunities=0,queryHit=0,queryHit3=0,vectorQueryHit=0;
        var details=new ArrayList<Map<String,Object>>();var abstentions=new TreeMap<String,List<String>>();
        for(var row:rows) {
            var expected=refs.get(row.caseId());boolean pos=expected.relation()!=null,matched=false;
            if(pos) {positive++;if(row.atK()) recalled++;if(row.at3()) at3++;if(row.vectorAtK()) vector++;}
            if(pos) for(var query:row.queries()) {
                String partner=query.anchor().equals("left")?"right":"left";
                if(query.fused().contains(partner)) queryHit++;
                if(query.fused().stream().limit(3).anyMatch(partner::equals)) queryHit3++;
                if(query.vector().stream().limit(10).anyMatch(partner::equals)) vectorQueryHit++;
            }
            var wrong=new ArrayList<String>();
            for(var a:row.attempts()) {
                boolean target=Set.of(a.a(),a.b()).equals(Set.of("left","right"));if(!target||!pos) unrelatedOpportunities++;
                if(a.relation()==null) {abstentions.computeIfAbsent(a.reason(),k->new ArrayList<>()).add(row.caseId()+":"+a.a()+"/"+a.b());continue;}
                boolean correct=target&&pos&&expected.relation()==a.relation()&&Objects.equals(expected.fromKey(),a.fromKey());
                if(correct&&!matched) {tp++;matched=true;} else {fp++;wrong.add(a.a()+"/"+a.b()+":"+a.relation()+":"+a.fromKey());}
                if(a.relation()==ConsolidationRelation.SAME_AS&&!correct) sameErrors++;
                if(!target||!pos) unrelated++;
                evidence+=a.evidenceTotal();errors+=a.evidenceErrors();policy++;if(a.pendingOnly()) policyCorrect++;
            }
            if(pos&&!matched) {fn++;if(row.atK()) conditionalFn++;}
            owner+=row.ownerViolations();privacy+=row.privacyViolations();
            var entry=new LinkedHashMap<String,Object>();entry.put("caseId",row.caseId());entry.put("expectedRelation",expected.relation());entry.put("expectedFrom",expected.fromKey());
            entry.put("recallAtK",row.atK());entry.put("vectorRecallAtK",row.vectorAtK());entry.put("correctTypeAndDirection",matched);entry.put("wrongLinks",wrong);
            entry.put("failureStage",pos&&!matched?(row.atK()?"RELATION_CLASSIFICATION":"CANDIDATE_RECALL"):wrong.isEmpty()?"NONE":"FALSE_RELATION");
            entry.put("observation",row);details.add(entry);
        }
        double p=ratio(tp,tp+fp),rec=ratio(tp,tp+fn),f1=p+rec==0?0:2*p*rec/(p+rec),evidenceAccuracy=ratio(evidence-errors,evidence),review=ratio(policyCorrect,policy);
        var failed=new ArrayList<String>();
        if(ratio(recalled,positive)<gates.get("minimumCandidateRecallAtK").getAsDouble()) failed.add("CANDIDATE_RECALL");
        if(p<gates.get("minimumPrecision").getAsDouble()) failed.add("RELATION_PRECISION");if(rec<gates.get("minimumRecall").getAsDouble()) failed.add("RELATION_RECALL");if(f1<gates.get("minimumF1").getAsDouble()) failed.add("RELATION_F1");
        if(sameErrors>0) failed.add("FALSE_SAME_AS");if(unrelated>0) failed.add("UNRELATED_LINK");if(owner>0) failed.add("OWNER_VIOLATION");if(privacy>0) failed.add("PRIVACY_VIOLATION");if(errors>0||evidence==0) failed.add("EVIDENCE_ERROR_OR_UNMEASURED");
        if(review<1||conflictAccuracy<gates.get("minimumConflictReviewAccuracy").getAsDouble()) failed.add("REVIEW_POLICY");
        var result=new LinkedHashMap<String,Object>();result.put("cases",rows.size());result.put("positiveReferencePairs",positive);result.put("recalledPositivePairs",recalled);
        result.put("candidateRecallAt10",ratio(recalled,positive));result.put("candidateRecallAt3",ratio(at3,positive));result.put("vectorOnlyRecallAt10Diagnostic",ratio(vector,positive));
        result.put("queryRecallAt10",ratio(queryHit,positive*2L));result.put("queryRecallAt3",ratio(queryHit3,positive*2L));result.put("vectorQueryRecallAt10Diagnostic",ratio(vectorQueryHit,positive*2L));
        double conditionalRecall=ratio(tp,tp+conditionalFn);
        result.put("relationClassification",Map.of("precision",p,"recall",conditionalRecall,"f1",p+conditionalRecall==0?0:2*p*conditionalRecall/(p+conditionalRecall)));
        result.put("tp",tp);result.put("fp",fp);result.put("fn",fn);result.put("precision",p);result.put("recall",rec);result.put("f1",f1);
        result.put("conditionalClassificationRecall",ratio(tp,tp+conditionalFn));result.put("conditionalClassificationFn",conditionalFn);
        result.put("falseSameAs",sameErrors);result.put("unrelatedLinks",unrelated);result.put("unrelatedOpportunities",unrelatedOpportunities);result.put("unrelatedLinkRate",ratio(unrelated,unrelatedOpportunities));
        result.put("evidenceTotal",evidence);result.put("evidenceErrors",errors);result.put("evidenceAccuracy",evidenceAccuracy);result.put("ownerViolations",owner);result.put("privacyViolations",privacy);
        result.put("reviewPolicyAccuracy",review);result.put("conflictReviewAccuracy",conflictAccuracy);result.put("quantitativeGatePass",failed.isEmpty());result.put("failedGates",failed);result.put("abstentions",abstentions);result.put("details",details);return result;
    }
    private static double ratio(long n,long d) {return d==0?0:(double)n/d;}
}
