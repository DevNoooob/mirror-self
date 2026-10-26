package com.mirrorself.ingestion;

import com.mirrorself.memory.MemoryExtractionService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Backward-compatible single-message entry point; full imports use conversation paths. */
@Service
public class IngestionUnitService {
    public record SavedUnit(boolean newSource,int newMemories) { }
    private final SourceRepository sources;
    private final MemoryExtractionService extraction;
    public IngestionUnitService(SourceRepository sources,MemoryExtractionService extraction) {
        this.sources=sources; this.extraction=extraction;
    }
    public SavedUnit ingest(UUID owner,SourceInput input) {
        var stored=sources.save(owner,input);
        if(!stored.inserted()) return new SavedUnit(false,0);
        var job=extraction.extract(owner,List.of(stored.id()),null);
        return new SavedUnit(true,job.newMemories());
    }
}
