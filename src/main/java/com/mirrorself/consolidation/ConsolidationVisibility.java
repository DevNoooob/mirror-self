package com.mirrorself.consolidation;

import com.mirrorself.retrieval.RetrievalVisibility;

public final class ConsolidationVisibility {
    private ConsolidationVisibility() { }
    public static final String SQL=RetrievalVisibility.SQL+" AND m.assertion_origin='USER_EXPLICIT' ";
    public static String forAlias(String alias) {
        if(!java.util.Set.of("m","a","b").contains(alias)) throw new IllegalArgumentException("invalid SQL alias");
        return SQL.replace("m.",alias+".");
    }
}
