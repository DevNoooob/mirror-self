package com.mirrorself.evaluation;

import com.mirrorself.config.ExtractionProperties;
import com.mirrorself.ingestion.SourceRepository;
import com.mirrorself.memory.*;
import com.mirrorself.model.ModelAuthorization;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker=true)
abstract class EvaluationIntegrationSupport {
    @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg17").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("mirror_self_evaluation_test").withUsername("mirror").withPassword("fictional-evaluation-password");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",postgres::getJdbcUrl);
        registry.add("spring.datasource.username",postgres::getUsername);
        registry.add("spring.datasource.password",postgres::getPassword);
    }
    @Autowired SourceRepository sources;
    @Autowired MemoryRepository memories;
    @Autowired MemoryExtractionService extraction;
    @Autowired ModelAuthorization authorization;
    @Autowired JdbcTemplate jdbc;
    @Autowired ExtractionProperties properties;
    @Autowired EvaluationGateway gateway;

    @TestConfiguration(proxyBeanMethods=false)
    static class OfflineConfiguration {
        @Bean @Primary EvaluationGateway fixtureModel(SourceRepository sources) throws Exception {
            return new EvaluationGateway(new FixtureModelGateway(sources,Path.of("evaluation/fixtures/mock-v1.json")));
        }
    }
    EvaluationReport evaluate(MemoryEvaluationService.Mode mode) throws Exception {
        Path dataset=Path.of(System.getProperty("evaluation.dataset","evaluation/datasets/memory-quality-v1.json"));
        var loaded=new DatasetLoader().load(dataset);
        if(mode==MemoryEvaluationService.Mode.LIVE) {
            EvaluationSafety.requireLiveConsent(loaded,"true".equals(System.getenv("MIRROR_EVALUATION_LIVE_ENABLED")),
                    System.getenv("MIRROR_EVALUATION_APPROVED_DATASET_SHA256"));
            loaded=EvaluationSelection.reviewed(loaded,System.getenv("MIRROR_EVALUATION_CASE_IDS"),
                    System.getenv("MIRROR_EVALUATION_APPROVED_CASE_IDS"),
                    "true".equals(System.getenv("MIRROR_EVALUATION_GOLD_REVIEWED")));
        }
        var service=new MemoryEvaluationService(sources,memories,extraction,authorization,jdbc,properties,gateway);
        var report=service.run(loaded,mode,"true".equals(System.getenv("MIRROR_EVALUATION_LIVE_ENABLED")),
                System.getenv("MIRROR_EVALUATION_APPROVED_DATASET_SHA256"),git("rev-parse","HEAD"),!git("status","--porcelain","--", ".", ":!.vscode", ":!target").isBlank(),
                "true".equals(System.getenv("MIRROR_EVALUATION_GOLD_REVIEWED")));
        String folder=mode==MemoryEvaluationService.Mode.OFFLINE_MOCK?"offline-mock":mode==MemoryEvaluationService.Mode.OFFLINE_RULE?"offline-rule":"live";
        var writer=new EvaluationReportWriter();
        Path baseline=Path.of(System.getProperty("evaluation.baseline","evaluation/baselines/"+folder+"-v1.json"));
        if(Files.exists(baseline)) {
            report=new BaselineComparator().attach(report,writer.read(baseline));

        }
        Path output=Path.of(System.getProperty("evaluation.output","target/evaluation/"+folder));
        writer.write(report,output);
        assertEquals(0,gateway.budgetDenied(),"Approved call budget exhausted; partial report saved, do not freeze as a complete baseline");
        if(Files.exists(baseline)) {
            var comparison=report.comparison();
            if(mode!=MemoryEvaluationService.Mode.LIVE) {
                assertTrue(comparison.comparable(),comparison.reason());
                var previous=writer.read(baseline);
                assertEquals(previous.overall().extracted().counts(),report.overall().extracted().counts(),"Extracted counts changed; see report");
                assertEquals(previous.overall().persisted().counts(),report.overall().persisted().counts(),"Stored counts changed; see report");
                assertEquals(previous.overall().deduplicationAccuracy(),report.overall().deduplicationAccuracy());
                assertEquals(previous.overall().unauthorizedCalls(),report.overall().unauthorizedCalls());
                assertEquals(previous.overall().errorCounts(),report.overall().errorCounts(),"Failure event counts changed");
                Map<String,EvaluationReport.CaseResult> priorCases=new HashMap<>();
                previous.cases().forEach(c->priorCases.put(c.caseId(),c));
                for(var c:report.cases())assertEquals(priorCases.get(c.caseId()).rejectedByStage(),c.rejectedByStage());
                for(var entry:report.categories().entrySet()) {
                    assertEquals(previous.categories().get(entry.getKey()).extracted().counts(),entry.getValue().extracted().counts());
                    assertEquals(previous.categories().get(entry.getKey()).persisted().counts(),entry.getValue().persisted().counts());
                }
                assertTrue(comparison.metricDeltas().values().stream().allMatch(delta->Math.abs(delta)<1e-12),
                        "Deterministic offline baseline changed; inspect category deltas before explicitly replacing it");
                assertTrue(comparison.categoryDeltas().values().stream().flatMap(values->values.values().stream())
                        .allMatch(delta->Math.abs(delta)<1e-12),"Category baseline changed; see report");
                assertTrue(comparison.newFailures().isEmpty());assertTrue(comparison.resolvedFailures().isEmpty());
            }
        }
        String thresholds=System.getProperty("evaluation.thresholds");
        if(thresholds!=null) {
            var gate=EvaluationJson.GSON.fromJson(Files.readString(Path.of(thresholds)),EvaluationThresholds.class);
            assertEquals(List.of(),gate.violations(report),"Configured quality thresholds not met; see report");
        }
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM identity_claim",Integer.class));
        return report;
    }
    private static String git(String... args) {
        try {
            List<String> command=new ArrayList<>(List.of("git","-c","safe.directory="+Path.of("").toAbsolutePath()));
            command.addAll(List.of(args));var process=new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            String result=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).strip();
            return process.waitFor()==0?result:"unknown";
        }catch(Exception ex){return "unknown";}
    }
}
