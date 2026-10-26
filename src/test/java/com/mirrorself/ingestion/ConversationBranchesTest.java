package com.mirrorself.ingestion;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

class ConversationBranchesTest {
    @Test void storesAllBranchesButExtractsOnlySelectedParentChainInOrder() throws Exception {
        String json="""
            [{"id":"c1","current_node":"leaf-b","mapping":{
              "leaf-b":{"id":"leaf-b","parent":"root","message":{"id":"b","author":{"role":"user"},"content":{"parts":["我喜欢阅读"]}}},
              "leaf-a":{"id":"leaf-a","parent":"root","message":{"id":"a","author":{"role":"user"},"content":{"parts":["我喜欢登山"]}}},
              "root":{"id":"root","parent":null,"message":{"id":"r","author":{"role":"assistant"},"content":{"parts":["你喜欢什么？"]}}}
            }}]
            """;
        List<ChatGptDecoder.Conversation> result=new ArrayList<>();
        new ChatGptDecoder().decodeConversations(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),result::add);
        assertEquals(3,result.getFirst().sources().size());
        assertEquals(List.of(List.of("c1:r","c1:b")),result.getFirst().paths());
        json=json.replace("\"current_node\":\"leaf-b\",","");result.clear();
        new ChatGptDecoder().decodeConversations(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),result::add);
        assertEquals(Set.of(List.of("c1:r","c1:a"),List.of("c1:r","c1:b")),new HashSet<>(result.getFirst().paths()));
    }
}
