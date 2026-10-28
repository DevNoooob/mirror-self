package com.mirrorself.retrieval;

import static org.junit.jupiter.api.Assertions.*;
import com.mirrorself.evaluation.retrieval.*;
import com.mirrorself.evaluation.RetrievalMetrics;
import com.mirrorself.embedding.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class RetrievalCorpusTest {
    @Test void inputAndIndependentReferenceWerePinnedBeforeModelExecution() {
        var loaded=RetrievalCorpus.load();assertEquals(44,loaded.inputs().queries().size());assertEquals(42,loaded.inputs().memories().size());
        assertEquals("4b0cde4f771356ea86c2bf70cfab27703c7bf7d06bbebb7acd308482526f2ad4",loaded.plan().datasetSha256());
        assertEquals("cd1c9933d61460657eaed2247b490ae502bc7fc7dbccbce8a0ffc1e83f1285b2",loaded.plan().referenceSha256());
        assertEquals("SYNTHETIC_REFERENCE_PENDING_HUMAN_REVIEW",loaded.reference().annotationStatus());
    }
    @Test void referencesCannotBypassOwnerOrPendingMemoryVisibility() {
        var loaded=RetrievalCorpus.load();var answers=new ArrayList<>(loaded.reference().answers());var old=answers.getFirst();
        answers.set(0,new RetrievalCorpus.Answer(old.queryId(),List.of(new RetrievalCorpus.Relevant("R41",com.mirrorself.retrieval.evidence.EvidenceSupportJudge.Relation.SUPPORTS,3)),old.expectedAggregate(),old.currentRecognition()));
        var bad=new RetrievalCorpus.Reference(loaded.reference().version(),loaded.reference().datasetVersion(),loaded.reference().annotationStatus(),true,
                loaded.reference().defaultUnlistedRelation(),answers);
        assertThrows(IllegalArgumentException.class,()->RetrievalCorpus.validate(loaded.inputs(),bad,loaded.plan()));
    }
    @Test void precisionUsesFixedKAndCountsRefutationAsRelevant() {
        assertEquals(1d/3,RetrievalMetrics.precisionAtK(List.of("refutation","extra"),Map.of("refutation",3),3),1e-12);
        assertEquals(0,RetrievalMetrics.precisionAtK(List.of(),Map.of(),3));
    }
    @Test void providerFailureStopsFurtherActualModelRequests() {
        int[] calls={0};var provider=new CountedEmbeddingProvider(new EmbeddingProvider() {
            public Model model() {return new Model("fixture","v1");}public Location location() {return Location.LOCAL;}
            public float[] embed(String text) {calls[0]++;throw new EmbeddingFailure(EmbeddingFailure.Code.UNAVAILABLE);}
        },10);
        assertThrows(EmbeddingFailure.class,()->provider.embed("fictional"));
        assertThrows(EmbeddingFailure.class,()->provider.embed("fictional"));assertEquals(1,calls[0]);
    }
    @Test void requestBudgetStopsBeforeInvokingTheDelegate() {
        int[] calls={0};var provider=new CountedEmbeddingProvider(new EmbeddingProvider() {
            public Model model() {return new Model("fixture","v1");}public Location location() {return Location.LOCAL;}
            public float[] embed(String text) {calls[0]++;var v=new float[1024];v[0]=1;return v;}
        },1);
        provider.embed("fictional");assertThrows(EmbeddingFailure.class,()->provider.embed("fictional"));
        assertEquals(1,calls[0]);assertEquals(1,provider.calls());
    }
}
