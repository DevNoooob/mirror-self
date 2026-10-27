package com.mirrorself.evaluation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties={"mirror.owner-token=0123456789abcdef0123456789abcdef","mirror.extraction.mode=RULE",
        "mirror.extraction.local=false","mirror.model-egress-enabled=false","server.port=0"})
@Import(EvaluationIntegrationSupport.OfflineConfiguration.class)
class MemoryEvaluationRuleIT extends EvaluationIntegrationSupport {
    @Test void measuresActualRuleExtractorWithoutAnyModelCall() throws Exception {
        var report=evaluate(MemoryEvaluationService.Mode.OFFLINE_RULE);
        assertTrue(report.overall().caseCount()>=50);
        assertEquals(0,report.run().modelCalls());
        assertEquals("none",report.run().model());
        assertTrue(report.overall().persisted().counts().tp()>0);
    }
}
