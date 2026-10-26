package com.mirrorself.memory;

/** Pure domain policy: derived personality/value claims can never be self-certified. */
public final class MemoryPolicy {
    private MemoryPolicy() { }

    public static MemoryStatus initialStatus(AssertionOrigin origin, Sensitivity sensitivity) {
        if (origin == AssertionOrigin.MODEL_INFERRED || sensitivity != Sensitivity.NORMAL) {
            return MemoryStatus.PENDING_REVIEW;
        }
        return MemoryStatus.ACTIVE;
    }

    public static MemoryStatus initialStatus(MemoryType type, AssertionOrigin origin, Sensitivity sensitivity) {
        return type == MemoryType.THOUGHT_PROCESS ? MemoryStatus.PENDING_REVIEW : initialStatus(origin, sensitivity);
    }

    public static MemoryStatus reviewed(MemoryStatus old, ReviewDecision decision) {
        if (old != MemoryStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only PENDING_REVIEW memories may be reviewed");
        }
        return decision == ReviewDecision.APPROVE ? MemoryStatus.ACTIVE : MemoryStatus.REJECTED;
    }
}
