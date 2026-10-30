package com.mirrorself.consolidation;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class ConsolidationCompatibilityTest {
    @Test void keepsArchivedMigrationReferencesAndFirstObservationByteIdentical() throws Exception {
        var immutable=Map.ofEntries(
            Map.entry("src/main/resources/db/migration/V4__consolidation_candidates.sql","032b3b55f6070549be8eff1c4dfbfeafe9750b09e73e0f516da19144515f84fc"),
            Map.entry("evaluation/datasets/consolidation-fictional-v1.json","a7a9fadcf08efd859d0ebef5a7c889338eb8b8a47763a1cbed669aacf2c44d55"),
            Map.entry("evaluation/datasets/retrieval-fictional-v2.json","4b0cde4f771356ea86c2bf70cfab27703c7bf7d06bbebb7acd308482526f2ad4"),
            Map.entry("evaluation/references/retrieval-reference-v2.json","cd1c9933d61460657eaed2247b490ae502bc7fc7dbccbce8a0ffc1e83f1285b2"),
            Map.entry("evaluation/datasets/retrieval-fictional-v3.json","aedb0a96d46dbcb06e5bee0481a3566855a55330fed842efd334db840035ab50"),
            Map.entry("evaluation/references/retrieval-reference-v3.json","0b6404d8752737a8eb3477f0d08d47b8738c6d874da29644ec014b2aeb2649dd"),
            Map.entry("evaluation/runs/consolidation-offline-v1/analysis.json","6cecefac5f8a32f8abde079d7b2f822b67e4a3e056a4748c66acf6b4464cecf1"),
            Map.entry("evaluation/runs/consolidation-offline-v1/analysis.md","1668157af871bf51b697983d1bb19928e47e4467e5b4941a0994358451f854e6"),
            Map.entry("evaluation/runs/consolidation-offline-v1/manifest.json","0a687ec9d504fa8d847ca0352b2c9ca28256ab66d3a603f5588d7daf9b943c5b"),
            Map.entry("evaluation/runs/consolidation-offline-v1/report.json","ac4cdf90593887d73d22c71f7688e0959b284869887d81f3de9c3e7eda9a3ed2"),
            Map.entry("evaluation/runs/consolidation-offline-v1/report.md","3ee8c3aba4bf191a168435bdcb780579bb9ba7c8292f885bf42db6667f51a66e"));
        for(var e:immutable.entrySet()) assertEquals(e.getValue(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(e.getKey())))),e.getKey());
    }
}
