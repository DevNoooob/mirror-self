package com.mirrorself.ingestion;

import com.mirrorself.memory.MemoryService;
import com.mirrorself.memory.RuleBasedExtractor;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A message and its derived L1 memories commit atomically. */
@Service
public class IngestionUnitService {
    public record SavedUnit(boolean newSource, int newMemories) { }
    private final SourceRepository sourceRepo;
    private final RuleBasedExtractor extractor;
    private final MemoryService memories;

    public IngestionUnitService(SourceRepository sourceRepo, RuleBasedExtractor extractor,
                                MemoryService memories) {
        this.sourceRepo = sourceRepo;
        this.extractor = extractor;
        this.memories = memories;
    }

    @Transactional
    public SavedUnit ingest(UUID owner, SourceInput input) {
        SourceRepository.Stored source = sourceRepo.save(owner, input);
        if (!source.inserted()) return new SavedUnit(false, 0);
        int created = memories.addCandidates(owner, source.id(), extractor.extract(
                input.role(), input.text(), input.occurredAt()));
        return new SavedUnit(true, created);
    }
}
