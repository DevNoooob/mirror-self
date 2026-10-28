package com.mirrorself.retrieval;

/** SQL predicate shared by indexing, keyword, vector and final evidence hydration.
 * Alias m is always memory_item. Owner is bound separately at every call site. */
public final class RetrievalVisibility {
    private RetrievalVisibility() { }
    public static final String SQL="""
        m.status='ACTIVE' AND m.sensitivity='NORMAL'
        AND EXISTS(SELECT 1 FROM memory_evidence ev WHERE ev.memory_id=m.id)
        AND NOT EXISTS (
            SELECT 1 FROM memory_evidence ev LEFT JOIN source_record sr ON sr.id=ev.source_id
            WHERE ev.memory_id=m.id AND (sr.id IS NULL OR sr.owner_id<>m.owner_id
                OR sr.sensitivity<>'PRIVATE' OR sr.role<>'user'
                OR length(ev.excerpt)=0 OR position(ev.excerpt in sr.text_content)=0))
        """;
}
