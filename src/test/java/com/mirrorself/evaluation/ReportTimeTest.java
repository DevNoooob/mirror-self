package com.mirrorself.evaluation;

import java.time.Instant;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportTimeTest {
    @Test void instantIsUtcRegardlessOfJvmDisplayTimezone() {
        TimeZone old=TimeZone.getDefault();
        try {
            var moment=Instant.parse("2026-03-08T02:15:00.123456789Z");
            for(String zone:new String[]{"UTC","Asia/Shanghai","America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                String json=EvaluationJson.GSON.toJson(moment,Instant.class);
                assertEquals("\"2026-03-08T02:15:00.123456789Z\"",json);
                assertEquals(moment,EvaluationJson.GSON.fromJson(json,Instant.class));
            }
        } finally {TimeZone.setDefault(old);}
    }
}
