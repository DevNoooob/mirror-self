package com.mirrorself.memory;

import com.mirrorself.ingestion.SourceRepository.SourceView;
import com.mirrorself.model.ModelFailure;
import java.util.List;

/** Optional, in-process instrumentation. Production API calls use NONE and expose no trace. */
public interface ExtractionObserver {
    enum Stage { AUTHORIZATION, EXTRACTION, EVIDENCE_VALIDATION, SEMANTIC_VALIDATION, PERSISTENCE }
    ExtractionObserver NONE = new ExtractionObserver() { };
    default void extracted(List<SourceView> batch, List<ExtractedMemory> candidates) { }
    default void rejected(Stage stage, ExtractedMemory candidate) { }
    default void persisted(List<ExtractedMemory> candidates, int inserted) { }
    default void failed(Stage stage, ModelFailure.Code code) { }
}
