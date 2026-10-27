package com.mirrorself.evaluation;

import com.mirrorself.memory.*;
import java.time.Instant;
import java.util.List;

/** Versioned reference annotations are curated independently of evaluated model outputs. */
public record EvaluationDataset(String schemaVersion, String datasetVersion, boolean fictional,
        String goldProvenance, String humanReviewStatus, List<Case> cases) {
    public enum Egress { NORMAL_GRANT, SENSITIVE_GRANT, NO_GRANT, RESTRICTED_SOURCE }
    public enum EvidencePolicy { ANY, ALL }
    public record Message(String messageId, String role, String text, Instant statedAt, String parentId) { }
    public record Evidence(String messageId, String role, String quote, int start, int end) { }
    public record Gold(String memoryId, MemoryType type, AssertionOrigin origin, String semantic,
            List<String> acceptedContents, List<List<String>> requiredConceptGroups, List<String> excludedConcepts,
            List<Evidence> expectedEvidence, EvidencePolicy evidencePolicy,
            ExtractedMemory.TemporalScope expectedTemporalScope, Sensitivity sensitivity,
            MemoryStatus expectedReviewStatus) { }
    public record Forbidden(String claimId, EvaluationError kind, List<String> phrases) { }
    public record Case(String caseId, List<String> categories, List<Message> messages, String selectedLeaf,
            List<Gold> expectedMemories, List<Forbidden> forbiddenClaims, Egress egress,
            boolean expectBlocked, boolean expectConflict, int repeatExtractions) { }
}
