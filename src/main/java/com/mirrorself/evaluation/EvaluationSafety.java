package com.mirrorself.evaluation;

import org.springframework.jdbc.core.JdbcTemplate;

public final class EvaluationSafety {
    private EvaluationSafety() { }
    public static void requireIsolatedDatabase(JdbcTemplate jdbc) {
        String name = jdbc.queryForObject("SELECT current_database()", String.class);
        if (name == null || !name.startsWith("mirror_self_evaluation_"))
            throw new IllegalStateException("Evaluation requires a dedicated mirror_self_evaluation_* database");
    }
    public static int approvedCallLimit(String value) {
        if(value==null||!value.matches("[1-9][0-9]{0,3}"))
            throw new IllegalArgumentException("Set an explicit approved evaluation call limit");
        int limit=Integer.parseInt(value);
        if(limit>1000)throw new IllegalArgumentException("Evaluation call limit must be 1-1000");
        return limit;
    }
    public static void requireLiveConsent(DatasetLoader.Loaded loaded, boolean enabled, String approvedSha256) {
        if (!enabled || !loaded.dataset().fictional() || approvedSha256 == null
                || !approvedSha256.matches("[a-f0-9]{64}") || !approvedSha256.equals(loaded.sha256()))
            throw new IllegalStateException("Live evaluation needs explicit approval of this fictional dataset SHA-256");
    }
}
