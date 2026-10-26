package com.mirrorself.memory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Conservative, deterministic sample extractor; NOT a general NLP model.
 * Works only on the user's original message. No inferred traits.
 */
@Component
public class RuleBasedExtractor implements MemoryExtractor {
    @Override public String version() { return "rule-0.2.0"; }
    @Override public String model() { return "none"; }
    @Override public List<ExtractedMemory> extract(java.util.UUID owner,
            List<com.mirrorself.ingestion.SourceRepository.SourceView> messages, java.util.UUID grantId) {
        List<ExtractedMemory> result = new ArrayList<>();
        for (var message : messages) {
            for (var candidate : extract(message.role(), message.text(), message.occurredAt())) {
                result.add(new ExtractedMemory(candidate.type(), candidate.content(), candidate.origin(),
                        candidate.sensitivity(), ExtractedMemory.TemporalScope.SOURCE_TIME,
                        List.of(new ExtractedMemory.EvidenceRef(message.id(), candidate.exactEvidence()))));
            }
        }
        return result;
    }

    private static final Pattern LEARNING = Pattern.compile(
        "^我(?:正在|现在在|最近在|目前在|一直在)(?:学习|学|研究).+");
    private static final Pattern GOAL = Pattern.compile(
        "^(?:我的目标是|我希望|我打算|我计划).+");
    private static final Pattern OPINION = Pattern.compile(
        "^我(?:认为|觉得|更倾向于).+");

    public List<MemoryCandidate> extract(String role, String text, Instant occurredAt) {
        if (!"user".equals(role) || text == null || text.isBlank()) return List.of();
        List<MemoryCandidate> result = new ArrayList<>();
        for (String part : text.split("[。！？!?\\n\\r]+")) {
            String sentence = part.trim();
            if (sentence.length() < 5 || sentence.length() > 220) continue;
            MemoryType type;
            if (LEARNING.matcher(sentence).matches()) type = MemoryType.KNOWLEDGE;
            else if (GOAL.matcher(sentence).matches()) type = MemoryType.GOAL;
            else if (OPINION.matcher(sentence).matches()) type = MemoryType.OPINION;
            else continue;
            result.add(new MemoryCandidate(type, sentence, AssertionOrigin.USER_EXPLICIT,
                    Sensitivity.NORMAL, occurredAt, sentence));
            if (result.size() == 8) break;
        }
        return result;
    }
}
