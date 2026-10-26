package com.mirrorself.model;

import com.mirrorself.ingestion.SourceRepository.SourceView;
import java.util.List;
import java.util.UUID;

/** All invocations, including semantic validation, require the same source-scoped authorization. */
public interface ModelGateway {
    String generate(UUID owner, UUID grantId, String instructions, String input, List<SourceView> sources);
}
