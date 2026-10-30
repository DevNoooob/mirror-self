package com.mirrorself.evaluation;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.mirrorself.config.EmbeddingProperties;
import com.mirrorself.consolidation.ConsolidationRelation;
import com.mirrorself.evaluation.consolidation.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class QualityContractTest {
    @Test void frozenInputsHaveDisjointFamiliesAndPendingReferences() {
        var l=QualityCorpus.load();assertEquals(120,l.inputs().cases().size());assertEquals(120,QualityCorpus.references(l).answers().size());
        assertEquals("e2a932f3f5fc04d740b48d65dd93ea8bc6e8935c40e440578e23323ef18a4b3c",l.plan().datasetSha256());
        assertEquals("70a513fbb2d3595d3c587ac1e2ff3457ee69e22f2b0b3a2c2840fe43e2dec47c",l.plan().referenceSha256());
        assertEquals("57ad19a948a7402fd376f75f39f9ab74a73675306641b380f3ce7ce93bc52c74",l.plan().gateSha256());
        assertEquals("a14530ff4c27b9973ad8ab57fcd69adc468ce2e2aa81f5b4d4aebb7357a8de87",l.planSha256());
        assertTrue(QualityCorpus.references(l).annotationStatus().contains("PENDING_HUMAN_REVIEW"));
    }
    QualityScorer.Observation observation(String id,boolean hit,ConsolidationRelation rel,String direction,int evidenceErrors,int owner) {
        var a=new QualityScorer.Attempt("left","right",rel,direction,rel==null?"UNRESOLVED_COMPLETE_PROPOSITION":"PROPOSED",rel==null?0:2,evidenceErrors,true);
        return new QualityScorer.Observation(id,"DEV","HYBRID",hit,hit,hit,List.of(),hit?List.of(a):List.of(),owner,0);
    }
    @Test void wrongDirectionIsClassificationErrorNotVectorRecallMiss() {
        var expected=new QualityCorpus.Answer("x",ConsolidationRelation.SUPPORTS,"left","fictional");
        var m=QualityScorer.score(List.of(observation("x",true,ConsolidationRelation.SUPPORTS,"right",0,0)),Map.of("x",expected),QualityCorpus.load().gates(),1);
        assertEquals(1,m.get("recalledPositivePairs"));assertEquals(0,m.get("tp"));assertEquals(1,m.get("fp"));assertEquals(1,m.get("fn"));
        assertTrue(EvaluationJson.GSON.toJson(m.get("details")).contains("RELATION_CLASSIFICATION"));
    }
    @Test void recallMissAndAbstentionAreSeparateAndNeverBecomeTrueNegatives() {
        var ref=Map.of("x",new QualityCorpus.Answer("x",ConsolidationRelation.SAME_AS,null,"fictional"));
        var missing=QualityScorer.score(List.of(observation("x",false,null,null,0,0)),ref,QualityCorpus.load().gates(),1);
        var refused=QualityScorer.score(List.of(observation("x",true,null,null,0,0)),ref,QualityCorpus.load().gates(),1);
        assertEquals(1,missing.get("fn"));assertEquals(0,missing.get("conditionalClassificationFn"));assertEquals(1,refused.get("conditionalClassificationFn"));
        assertTrue(EvaluationJson.GSON.toJson(refused.get("abstentions")).contains("UNRESOLVED_COMPLETE_PROPOSITION"));
    }
    @Test void perfectMajorityCannotMaskOneUnsafeLinkOrOwnerOrEvidenceFailure() {
        var rows=new ArrayList<QualityScorer.Observation>();var refs=new HashMap<String,QualityCorpus.Answer>();
        for(int i=0;i<100;i++) {String id="x"+i;rows.add(observation(id,true,ConsolidationRelation.SAME_AS,null,0,0));refs.put(id,new QualityCorpus.Answer(id,ConsolidationRelation.SAME_AS,null,"fictional"));}
        rows.add(observation("bad",true,ConsolidationRelation.SAME_AS,null,1,1));refs.put("bad",new QualityCorpus.Answer("bad",null,null,"unrelated"));
        var m=QualityScorer.score(rows,refs,QualityCorpus.load().gates(),1);assertTrue((Double)m.get("precision")>0.98);assertFalse((Boolean)m.get("quantitativeGatePass"));
        assertTrue(((List<?>)m.get("failedGates")).containsAll(List.of("FALSE_SAME_AS","OWNER_VIOLATION","UNRELATED_LINK","EVIDENCE_ERROR_OR_UNMEASURED")));
    }
    @Test void previousLiveConsentCannotAuthorizeNewRoundOrChangedScope() {
        var l=QualityCorpus.load();var p=new EmbeddingProperties(EmbeddingProperties.Mode.LOCAL,l.plan().endpoint(),"",l.plan().model(),l.plan().modelVersion(),java.time.Duration.ofSeconds(30));
        var env=new HashMap<String,String>();env.put("MIRROR_RUN_LOCAL_RETRIEVAL","true");
        assertThrows(IllegalStateException.class,()->QualityLiveAuthorization.check(l,p,env,"f".repeat(40),"Linux",21));
        env.putAll(Map.of("MIRROR_RUN_LOCAL_QG","true","MIRROR_ISOLATED_TEST_ENVIRONMENT","new-testcontainers","MIRROR_QG_APPROVED_PLAN_SHA",l.planSha256(),
            "MIRROR_QG_APPROVED_DATASET_SHA",l.plan().datasetSha256(),"MIRROR_QG_APPROVED_REFERENCE_SHA",l.plan().referenceSha256(),"MIRROR_QG_APPROVED_GATE_SHA",l.plan().gateSha256(),
            "MIRROR_QG_APPROVED_CODE_SHA","f".repeat(40),"MIRROR_MODEL_EGRESS_ENABLED","false","MIRROR_RUN_LIVE_LLM","false","MIRROR_EVALUATION_LIVE_ENABLED","false"));
        env.put("DOCKER_HOST","unix:///var/run/docker.sock");env.put("TESTCONTAINERS_HOST_OVERRIDE","127.0.0.1");
        assertDoesNotThrow(()->QualityLiveAuthorization.check(l,p,env,"f".repeat(40),"Linux",21));
        env.put("MIRROR_QG_APPROVED_PLAN_SHA","changed");assertThrows(IllegalStateException.class,()->QualityLiveAuthorization.check(l,p,env,"f".repeat(40),"Linux",21));
        env.put("MIRROR_QG_APPROVED_PLAN_SHA",l.planSha256());env.put("SPRING_DATASOURCE_URL","jdbc:postgresql://fictional-business-host/db");
        assertThrows(IllegalStateException.class,()->QualityLiveAuthorization.check(l,p,env,"f".repeat(40),"Linux",21));
    }
}
