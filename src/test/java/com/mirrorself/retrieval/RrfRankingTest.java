package com.mirrorself.retrieval;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RrfRankingTest {
    @Test void combinesRanksWithoutMixingIncommensurateScoresAndBreaksTiesById() {
        UUID a=new UUID(0,1),b=new UUID(0,2),c=new UUID(0,3);
        var scores=RrfRanking.rank(List.of(new RetrievalRepository.Ranked(a,99),new RetrievalRepository.Ranked(b,1)),
                List.of(new RetrievalRepository.Ranked(b,.9),new RetrievalRepository.Ranked(c,.8)));
        assertEquals(b,scores.getFirst().id());assertEquals(1d/62+1d/61,scores.getFirst().score(),1e-12);
        assertEquals(2,scores.getFirst().keywordRank());assertEquals(1,scores.getFirst().vectorRank());
    }
}
