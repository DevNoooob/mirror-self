package com.mirrorself.ingestion;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ChatGptDecoderTest {
    @Test void retainsRoleAndRawContent() throws Exception {
        String payload = """
            [{"id":"c1","title":"test","mapping":{
              "node1":{"message":{"id":"m1","author":{"role":"user"},
                 "create_time":1791867600,"content":{"parts":["我正在学习 Java。"]}}},
              "node2":{"message":{"id":"m2","author":{"role":"assistant"},
                 "content":{"parts":["模型生成的说法"]}}}
            }}]
            """;
        var inputs = new ArrayList<SourceInput>();
        new ChatGptDecoder().decode(new ByteArrayInputStream(
                payload.getBytes(StandardCharsets.UTF_8)), inputs::add);
        assertEquals(2, inputs.size());
        assertEquals("c1:m1", inputs.get(0).locator());
        assertEquals("user", inputs.get(0).role());
        assertEquals("assistant", inputs.get(1).role());
        assertTrue(inputs.get(0).rawJson().contains("create_time"));
        assertTrue(inputs.get(0).rawJson().contains("message"));
    }
}
