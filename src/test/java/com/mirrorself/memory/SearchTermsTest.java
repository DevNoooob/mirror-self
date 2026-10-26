package com.mirrorself.memory;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SearchTermsTest {
    @Test void handlesChineseAndJava() {
        var tokens = SearchTerms.from("我之前学习 Java 并发编程是什么时候？");
        assertTrue(tokens.contains("Java"));
        assertTrue(tokens.contains("并发"));
        assertTrue(tokens.size() <= 12);
    }
    @Test void noSqlWildcardsInTerms() {
        assertTrue(SearchTerms.from("%%%%____").isEmpty());
    }
}
