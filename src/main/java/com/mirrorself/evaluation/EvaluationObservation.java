package com.mirrorself.evaluation;

import com.mirrorself.memory.*;
import java.util.*;

/** Input to the scorer. Content stays in-process; reports contain IDs and fixed error categories only. */
public record EvaluationObservation(Set<String> selectedMessageIds, List<Prediction> extracted,
        List<Prediction> persisted, List<Rejection> rejections, List<Failure> failures,
        int replayChecks, int successfulReplays, int modelCalls, int unauthorizedCalls,
        int conflictEdges, long elapsedMs) {
    public record Evidence(String messageId, String role, String quote) { }
    public record Prediction(MemoryType type, String content, AssertionOrigin origin, Sensitivity sensitivity,
            ExtractedMemory.TemporalScope temporalScope, MemoryStatus status, List<Evidence> evidence) { }
    public record Rejection(ExtractionObserver.Stage stage, int candidateIndex) { }
    public record Failure(ExtractionObserver.Stage stage, String code) { }
}
