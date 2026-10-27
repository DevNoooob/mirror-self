package com.mirrorself.evaluation;

import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EvaluationSelectionTest {
    static final String IDS="MQ001,MQ019,MQ022,MQ025,MQ032,MQ043,MQ045,MQ047,MQ054";
    DatasetLoader.Loaded corpus() throws Exception {return new DatasetLoader().load(Path.of("evaluation/datasets/memory-quality-v1.json"));}
    @Test void selectsOnlyNineReviewedCasesWhilePreservingFullFileDigestAndGoldStatus() throws Exception {
        var full=corpus();var selected=EvaluationSelection.reviewed(full,IDS,IDS,true);
        assertEquals(9,selected.dataset().cases().size());assertEquals(full.sha256(),selected.sha256());
        assertEquals(9,selected.dataset().cases().stream().mapToInt(c->c.expectedMemories().size()).sum());
        assertEquals(66,full.dataset().cases().size());assertEquals("PENDING_HUMAN_REVIEW",selected.dataset().humanReviewStatus());
        var reverse=new ArrayList<>(List.of(IDS.split(",")));Collections.reverse(reverse);
        assertEquals(selected,EvaluationSelection.reviewed(full,IDS,String.join(",",reverse),true));
    }
    @Test void missingUnknownDuplicateOrUnapprovedScopeNeverFallsBackToFullCorpus() throws Exception {
        var full=corpus();
        for(String requested:Arrays.asList(null,"","MQ001,","MQ001,MQ001","MISSING","PRIVATE_SCOPE_SENTINEL")) {
            var ex=assertThrows(IllegalArgumentException.class,()->EvaluationSelection.reviewed(full,requested,IDS,true));
            assertFalse(ex.getMessage().contains("PRIVATE_SCOPE_SENTINEL"));
        }
        assertThrows(IllegalArgumentException.class,()->EvaluationSelection.reviewed(full,IDS,"MQ001",true));
        assertThrows(IllegalStateException.class,()->EvaluationSelection.reviewed(full,IDS,IDS,false));
    }
    @Test void callQuotaMustBeExplicitFiniteAndBounded() {
        assertEquals(36,EvaluationSafety.approvedCallLimit("36"));
        for(String invalid:Arrays.asList(null,"","0","-1","1001","NaN","PRIVATE_LIMIT_SENTINEL")) {
            var ex=assertThrows(IllegalArgumentException.class,()->EvaluationSafety.approvedCallLimit(invalid));
            assertFalse(ex.getMessage().contains("PRIVATE_LIMIT_SENTINEL"));
        }
    }
    @Test void legacyLiveServiceEntryFailsBeforeDatabaseOrGatewayAccess() throws Exception {
        var full=corpus();var gateway=new EvaluationGateway((o,g,i,t,s)->{throw new AssertionError("must not call model");},36);
        var service=new MemoryEvaluationService(null,null,null,null,null,null,gateway);
        assertThrows(IllegalStateException.class,()->service.run(full,MemoryEvaluationService.Mode.LIVE,true,full.sha256(),"abcdef0",false));
        assertEquals(0,gateway.calls());
    }
}
