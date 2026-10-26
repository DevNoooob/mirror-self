package com.mirrorself.memory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic V0.1 tokenizer. Indexed semantic search is intentionally deferred. */
public final class SearchTerms {
    private static final Pattern TERMS = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.#-]*|[\\p{IsHan}]{2,}");
    private SearchTerms() { }

    public static List<String> from(String query) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = TERMS.matcher(query);
        while (matcher.find() && tokens.size() < 12) {
            String term = matcher.group();
            if (term.codePoints().allMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)) {
                if (term.length() <= 3) tokens.add(term);
                else {
                    // Character bigrams allow some Chinese substring recall without a segmenter.
                    for (int i = 0; i + 2 <= term.length() && tokens.size() < 12; i++) {
                        tokens.add(term.substring(i, i + 2));
                    }
                }
            } else if (term.length() >= 2) tokens.add(term);
        }
        return new ArrayList<>(tokens);
    }
}
