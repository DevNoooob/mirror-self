package com.mirrorself.memory;

import java.util.List;
import java.util.UUID;

/** Untrusted model proposal; cannot be saved without MemoryValidator. */
public record ExtractedMemory(MemoryType type, String content, AssertionOrigin origin,
        Sensitivity sensitivity, TemporalScope temporalScope, List<EvidenceRef> evidence) {
    public enum TemporalScope { SOURCE_TIME, HISTORICAL }
    public record EvidenceRef(UUID sourceId, String quote) { }
}
