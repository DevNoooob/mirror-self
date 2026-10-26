package com.mirrorself.memory;

import com.mirrorself.ingestion.SourceRepository.SourceView;
import java.util.List;
import java.util.UUID;

public interface MemoryExtractor {
    List<ExtractedMemory> extract(UUID owner, List<SourceView> messages, UUID grantId);
    String version();
    String model();
}
