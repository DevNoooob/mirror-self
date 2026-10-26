package com.mirrorself.ingestion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Supports the common ChatGPT conversations.json array + mapping/message/author/content/parts shape.
 * Streams one conversation at a time; preserves full per-node JSON (including
 * thread linkage when provided), not the original conversation ZIP bytes.
 */
public class ChatGptDecoder {
    public record Conversation(java.util.List<SourceInput> sources, java.util.List<java.util.List<String>> paths) { }
    public void decode(InputStream input, Consumer<SourceInput> sink) throws IOException {
        decodeConversations(input,conversation->conversation.sources().forEach(sink));
    }
    public void decodeConversations(InputStream input, Consumer<Conversation> sink) throws IOException {
        Objects.requireNonNull(input); Objects.requireNonNull(sink);
        try (JsonReader reader=new JsonReader(new InputStreamReader(input,StandardCharsets.UTF_8))) {
            reader.beginArray();
            while(reader.hasNext()) {
                JsonElement element=JsonParser.parseReader(reader);
                if(element==null||!element.isJsonObject()) continue;
                JsonObject conversation=element.getAsJsonObject();
                String conversationId=string(conversation,"id",string(conversation,"conversation_id",null));
                if(conversationId==null||conversationId.isBlank()) continue;
                String title=string(conversation,"title","Untitled");
                JsonObject mapping=object(conversation,"mapping");
                if(mapping==null) continue;
                java.util.Map<String,SourceInput> messages=new java.util.LinkedHashMap<>();
                java.util.Map<String,String> parents=new java.util.HashMap<>();
                for(var entry:mapping.entrySet()) {
                    if(!entry.getValue().isJsonObject()) continue;
                    var node=entry.getValue().getAsJsonObject();
                    String parent=string(node,"parent",null);
                    if(parent!=null) parents.put(entry.getKey(),parent);
                    var message=object(node,"message");
                    if(message==null) continue;
                    String messageId=string(message,"id",entry.getKey());
                    var author=object(message,"author");
                    String role=author==null?"unknown":string(author,"role","unknown");
                    if(!role.matches("user|assistant|system|tool")) role="unknown";
                    messages.put(entry.getKey(),new SourceInput("CHATGPT",conversationId+":"+messageId,
                            title,role,node.toString(),text(object(message,"content")),time(message,"create_time")));
                }
                java.util.List<java.util.List<String>> paths=new java.util.ArrayList<>();
                String current=string(conversation,"current_node",null);
                if(current!=null&&mapping.has(current)) paths.add(path(current,parents,messages));
                else if(parents.isEmpty()) paths.add(messages.values().stream().map(SourceInput::locator).toList());
                else {
                    var nonLeaves=new java.util.HashSet<>(parents.values());
                    for(String node:mapping.keySet()) if(!nonLeaves.contains(node)) paths.add(path(node,parents,messages));
                }
                sink.accept(new Conversation(java.util.List.copyOf(messages.values()),java.util.List.copyOf(paths)));
            }
            reader.endArray();
        }
    }
    private static java.util.List<String> path(String node, java.util.Map<String,String> parents,
            java.util.Map<String,SourceInput> messages) {
        java.util.LinkedList<String> result=new java.util.LinkedList<>();
        java.util.Set<String> visited=new java.util.HashSet<>();
        while(node!=null) {
            if(!visited.add(node)) throw new IllegalArgumentException("cyclic conversation metadata");
            if(messages.containsKey(node)) result.addFirst(messages.get(node).locator());
            node=parents.get(node);
        }
        return java.util.List.copyOf(result);
    }

    private String text(JsonObject content) {
        if (content == null) return "";
        JsonElement parts = content.get("parts");
        if (parts != null && parts.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonElement element : parts.getAsJsonArray()) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    if (!sb.isEmpty()) sb.append('\n');
                    sb.append(element.getAsString());
                }
            }
            return sb.toString();
        }
        return string(content, "text", "");
    }

    private static JsonObject object(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    private static String string(JsonObject obj, String key, String fallback) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()
                ? el.getAsString() : fallback;
    }

    private static Instant time(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive()) return null;
        try {
            double seconds = el.getAsDouble();
            if (!Double.isFinite(seconds) || seconds <= 0) return null;
            return Instant.ofEpochMilli((long) (seconds * 1000));
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }
}
