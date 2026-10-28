package com.mirrorself.consolidation;

import com.mirrorself.memory.MemoryType;
import java.util.UUID;

/** Future separately authorized command port. No implementation or HTTP route in M2-B. */
public interface ReviewedMemoryChangePort {
    record TypeCorrection(UUID memoryId,long expectedRevision,MemoryType proposedType,UUID approvedReviewId) { }
    record EvolutionEdge(UUID newerMemoryId,UUID olderMemoryId,long newerRevision,long olderRevision,UUID approvedCandidateId) { }
    void proposeTypeCorrection(UUID owner,TypeCorrection request);
    void proposeEvolutionMaintenance(UUID owner,EvolutionEdge request);
}
