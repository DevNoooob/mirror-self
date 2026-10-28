package com.mirrorself.retrieval;

import static org.junit.jupiter.api.Assertions.*;
import com.mirrorself.evaluation.RetrievalMetrics;
import java.util.*;
import org.junit.jupiter.api.Test;

class RetrievalMetricsTest {
    @Test void scoresRankingGradedRelevanceEmptyQueriesAndDuplicateIds() {
        var score=RetrievalMetrics.score(List.of("x","b","a","a"),Map.of("a",3,"b",1),3);
        assertEquals(1,score.recallAtK());assertEquals(.5,score.reciprocalRank());assertTrue(score.ndcgAtK()<1);
        assertEquals(0,RetrievalMetrics.score(List.of("x"),Map.of("a",1),3).reciprocalRank());
        assertNull(RetrievalMetrics.score(List.of(),Map.of(),3).recallAtK());
        assertTrue(RetrievalMetrics.score(List.of(),Map.of(),3).correctEmpty());
        assertFalse(RetrievalMetrics.score(List.of("a"),Map.of(),3).correctEmpty());
    }
}
