package com.mirrorself.evaluation;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvaluationSafetyTest {
    @Test void evaluationRefusesBusinessDatabaseBeforeWriting() {
        var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForObject("SELECT current_database()",String.class)).thenReturn("mirror_self");
        assertThrows(IllegalStateException.class,()->EvaluationSafety.requireIsolatedDatabase(jdbc));
        verify(jdbc).queryForObject("SELECT current_database()",String.class);verifyNoMoreInteractions(jdbc);
    }
    @Test void liveRequiresBothExplicitOptInAndExactReviewedDatasetDigest() throws Exception {
        var loaded=new DatasetLoader().load(Path.of("evaluation/datasets/memory-quality-v1.json"));
        assertThrows(IllegalStateException.class,()->EvaluationSafety.requireLiveConsent(loaded,false,loaded.sha256()));
        assertThrows(IllegalStateException.class,()->EvaluationSafety.requireLiveConsent(loaded,true,null));
        assertThrows(IllegalStateException.class,()->EvaluationSafety.requireLiveConsent(loaded,true,"0".repeat(64)));
        assertDoesNotThrow(()->EvaluationSafety.requireLiveConsent(loaded,true,loaded.sha256()));
    }
}
