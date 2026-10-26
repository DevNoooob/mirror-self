package com.mirrorself.memory;

import java.text.Normalizer;
import java.util.Locale;

public final class MemoryNormalization {
    private MemoryNormalization() { }
    public static String canonical(String content) {
        return Normalizer.normalize(content,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .strip().replaceAll("\\s+"," ").replaceAll("[。.!！?？]+$","");
    }
}
