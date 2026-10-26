package com.mirrorself.memory;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MemoryPolicyTest {
    @Test void onlyOrdinaryExplicitClaimsBecomeActive() {
        assertEquals(MemoryStatus.ACTIVE, MemoryPolicy.initialStatus(
                AssertionOrigin.USER_EXPLICIT, Sensitivity.NORMAL));
        assertEquals(MemoryStatus.PENDING_REVIEW, MemoryPolicy.initialStatus(
                AssertionOrigin.MODEL_INFERRED, Sensitivity.NORMAL));
        assertEquals(MemoryStatus.PENDING_REVIEW, MemoryPolicy.initialStatus(
                AssertionOrigin.USER_EXPLICIT, Sensitivity.SECRET));
    }

    @Test void cognitiveSelfDescriptionsAlsoRequireReview() {
        assertEquals(MemoryStatus.PENDING_REVIEW,MemoryPolicy.initialStatus(MemoryType.THOUGHT_PROCESS,
                AssertionOrigin.USER_EXPLICIT,Sensitivity.NORMAL));
    }

    @Test void reviewIsOneWay() {
        assertEquals(MemoryStatus.REJECTED,
                MemoryPolicy.reviewed(MemoryStatus.PENDING_REVIEW, ReviewDecision.REJECT));
        assertThrows(IllegalStateException.class,
                () -> MemoryPolicy.reviewed(MemoryStatus.ACTIVE, ReviewDecision.APPROVE));
    }
}
