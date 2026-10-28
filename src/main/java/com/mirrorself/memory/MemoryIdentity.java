package com.mirrorself.memory;

import java.util.*;

/** Conservative in-process identity; never serialize its text into diagnostics.
 * Exact text, origin, time scope and ALL evidence must agree. Type is deliberately separate.
 * No punctuation/semantic normalization: two facts sharing a source must not be merged.
 */
public final class MemoryIdentity {
    private MemoryIdentity() { }
    public record Anchor(UUID sourceId, String quote) { }
    public record Key(String content, AssertionOrigin origin, ExtractedMemory.TemporalScope scope, List<Anchor> anchors) { }
    public static List<Anchor> anchors(List<Anchor> refs) {
        return refs.stream().distinct().sorted(Comparator.comparing((Anchor a)->a.sourceId().toString()).thenComparing(Anchor::quote)).toList();
    }
    public static Key of(ExtractedMemory m) {
        return new Key(m.content(),m.origin(),m.temporalScope(),anchors(m.evidence().stream().map(e->new Anchor(e.sourceId(),e.quote())).toList()));
    }
    public static Key of(MemoryRepository.MemoryView m) {
        return new Key(m.content(),m.origin(),ExtractedMemory.TemporalScope.valueOf(m.temporalScope()),
                anchors(m.evidence().stream().map(e->new Anchor(e.sourceId(),e.excerpt())).toList()));
    }
}
