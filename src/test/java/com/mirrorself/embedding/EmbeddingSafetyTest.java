package com.mirrorself.embedding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mirrorself.config.EmbeddingProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EmbeddingSafetyTest {
    @Test void validatesDimensionsFiniteValuesAndZeroNormWithoutLoggingInput() {
        assertEquals(EmbeddingFailure.Code.DIMENSION_MISMATCH,assertThrows(EmbeddingFailure.class,
                ()->EmbeddingVectors.sql(new float[768])).code());
        var vector=new float[1024];vector[0]=Float.NaN;
        assertThrows(EmbeddingFailure.class,()->EmbeddingVectors.validate(vector));
        assertThrows(EmbeddingFailure.class,()->EmbeddingVectors.validate(new float[1024]));
        vector[0]=1;assertTrue(EmbeddingVectors.sql(vector).startsWith("[1.0,"));
    }
    @Test void disabledAndCloudProvidersCannotReceiveQueriesOrMemoryText() {
        for(var location:java.util.List.of(EmbeddingProvider.Location.DISABLED,EmbeddingProvider.Location.CLOUD)) {
            var provider=mock(EmbeddingProvider.class);when(provider.location()).thenReturn(location);
            var error=assertThrows(EmbeddingFailure.class,()->new EmbeddingGateway(provider).embed("fictional private input"));
            assertFalse(error.toString().contains("private"));verify(provider,never()).embed(anyString());
        }
    }
    @Test void configurationRejectsCloudCredentialUrlsAndUnversionedLocalModels() {
        for(String url:java.util.List.of("https://example.com/v1","http://127.0.0.1:8090/v1?key=fictional",
                "http://fictional@localhost:8090/v1","http://127.0.0.1.evil.invalid/v1"))
            assertThrows(IllegalArgumentException.class,()->props(url,"v1"));
        assertThrows(IllegalArgumentException.class,()->props("http://localhost:8090/v1","unconfigured"));
        assertFalse(props("http://localhost:8090/v1","v1").toString().contains("fictional-key"));
    }
    @Test void arbitraryProviderErrorsHaveSafeFixedBoundary() {
        var provider=mock(EmbeddingProvider.class);when(provider.location()).thenReturn(EmbeddingProvider.Location.LOCAL);
        when(provider.embed(anyString())).thenThrow(new IllegalStateException("fictional-key / private chat / response body"));
        var failure=assertThrows(EmbeddingFailure.class,()->new EmbeddingGateway(provider).embed("private chat"));
        assertEquals("UNAVAILABLE",failure.getMessage());assertNull(failure.getCause());
    }
    private EmbeddingProperties props(String url,String version) {
        return new EmbeddingProperties(EmbeddingProperties.Mode.LOCAL,url,"fictional-key","bge-m3",version,Duration.ofSeconds(2));
    }
}
