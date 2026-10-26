package com.mirrorself.memory;

import java.time.Instant;

public record MemoryCandidate(MemoryType type, String content,
                              AssertionOrigin origin, Sensitivity sensitivity,
                              Instant occurredAt, String exactEvidence) {
    public MemoryCandidate {
        if (type == null || origin == null || sensitivity == null) {
            throw new IllegalArgumentException("type, origin and sensitivity are required");
        }
        if (content == null || content.isBlank() || content.length() > 3000) {
            throw new IllegalArgumentException("memory text must have 1–3000 characters");
        }
        if (exactEvidence == null || exactEvidence.isBlank()) {
            throw new IllegalArgumentException("evidence cannot be blank");
        }
    }
}
