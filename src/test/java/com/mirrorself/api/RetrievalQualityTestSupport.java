package com.mirrorself.api;

import static org.junit.jupiter.api.Assertions.*;
import com.mirrorself.evaluation.retrieval.*;
import com.mirrorself.memory.*;
import com.mirrorself.ingestion.*;
import com.mirrorself.embedding.*;
import com.mirrorself.retrieval.*;
import com.mirrorself.retrieval.evidence.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

abstract class RetrievalQualityTestSupport extends M2RetrievalTestSupport {
    @Autowired JdbcTemplate jdbc;
    @Autowired MemoryService memoryService;
    @Autowired MemoryRepository memories;
    @Autowired SourceRepository sources;
    @Autowired EmbeddingService indexing;
    @Autowired RetrievalService retrieval;
    @Autowired EvidenceSupportJudge judge;
    @Autowired CountedEmbeddingProvider counted;
    void evaluate(boolean real) throws Exception {
        var loaded=RetrievalCorpus.load();
        var result=new RetrievalQualityEvaluation(jdbc,memoryService,memories,sources,indexing,retrieval,judge,counted).run(loaded,real);
        assertEquals(0,result.get("privacyViolations"));assertEquals(0,result.get("evidenceErrors"));assertEquals(0,result.get("providerFailures"));
        assertTrue(((List<?>)result.get("modelErrors")).isEmpty());
        assertTrue(counted.calls()<=loaded.plan().maxEmbeddingRequests());
        for(var job:(List<?>)result.get("indexJobs")) assertEquals("COMPLETE",((EmbeddingService.Job)job).status());
        for(var mode:(List<?>)result.get("modes")) assertEquals(0,((Map<?,?>)mode).get("temporalOverclaims"));
        // Scores are observations. No assertion may require a model to match the reference perfectly.
        if(real) assertTrue(counted.calls()>0);
    }
}
