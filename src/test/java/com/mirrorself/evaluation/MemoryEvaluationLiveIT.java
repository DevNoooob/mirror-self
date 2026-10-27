package com.mirrorself.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import com.mirrorself.model.SpringAiModelGateway;

/** Paid/network evaluation is separately opted in and dataset-hash approved; never a default CI gate. */
@EnabledIfEnvironmentVariable(named="MIRROR_EVALUATION_LIVE_ENABLED",matches="true")
@SpringBootTest(properties={"mirror.owner-token=0123456789abcdef0123456789abcdef",
        "mirror.extraction.mode=LLM","mirror.extraction.local=false","server.port=0"})
@Import(MemoryEvaluationLiveIT.LiveConfiguration.class)
class MemoryEvaluationLiveIT extends EvaluationIntegrationSupport {
    @TestConfiguration(proxyBeanMethods=false)
    static class LiveConfiguration {
        @Bean @Primary EvaluationGateway liveModel(SpringAiModelGateway model) {return new EvaluationGateway(model,EvaluationSafety.approvedCallLimit(
                System.getenv("MIRROR_EVALUATION_MAX_MODEL_CALLS")));}
    }
    @Test void evaluateOnlyExplicitlyApprovedFictionalSources() throws Exception {
        evaluate(MemoryEvaluationService.Mode.LIVE);
    }
}
