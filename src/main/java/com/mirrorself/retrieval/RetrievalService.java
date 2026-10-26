package com.mirrorself.retrieval;

import com.mirrorself.memory.MemoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RetrievalService {
    private final MemoryRepository repo;
    public RetrievalService(MemoryRepository repo) { this.repo = repo; }

    /** Owner-only scope and ACTIVE filter live in the SQL, not in an LLM prompt. */
    public List<MemoryRepository.MemoryView> retrieve(UUID owner, String question, int limit) {
        if (question.length() > 1000) throw new IllegalArgumentException("query too long");
        return repo.search(owner, question, limit);
    }

    /** Extractive-only mode, never invents answers or sends text to an external model. */
    public ExtractiveAnswer answer(UUID owner, String question) {
        List<MemoryRepository.MemoryView> evidence = retrieve(owner, question, 6);
        if (evidence.isEmpty()) return new ExtractiveAnswer("EXTRACTIVE_NO_LLM",
                "没有找到足够的个人记忆证据，暂时无法根据你的历史资料回答。", List.of());
        StringBuilder answer = new StringBuilder("找到以下可能相关的历史记忆（请根据原始证据核对）：\n");
        for (int i=0; i<evidence.size(); i++) {
            answer.append(i + 1).append(". ").append(evidence.get(i).content()).append('\n');
        }
        return new ExtractiveAnswer("EXTRACTIVE_NO_LLM", answer.toString(), evidence);
    }

    public record ExtractiveAnswer(String mode, String answer,
                                  List<MemoryRepository.MemoryView> evidence) { }
}
