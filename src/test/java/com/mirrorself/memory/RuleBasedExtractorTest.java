package com.mirrorself.memory;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RuleBasedExtractorTest {
    private final RuleBasedExtractor extractor = new RuleBasedExtractor();
    @Test void excludesAssistantAndQuestions() {
        assertTrue(extractor.extract("assistant", "我认为你很聪明。", Instant.now()).isEmpty());
        assertTrue(extractor.extract("user", "你觉得我懂 Java 吗？", Instant.now()).isEmpty());
    }
    @Test void extractsLiteralClaimsWithoutInventingTraits() {
        var found = extractor.extract("user", "我正在学习 Java。我的目标是成为工程师。", Instant.now());
        assertEquals(2, found.size());
        assertEquals(MemoryType.KNOWLEDGE, found.get(0).type());
        assertEquals(MemoryType.GOAL, found.get(1).type());
        assertEquals(AssertionOrigin.USER_EXPLICIT, found.get(0).origin());
    }
}
