package com.mirrorself.memory;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.mirrorself.model.ModelFailure;
import java.io.StringReader;
import java.util.Set;

final class ModelJson {
    private ModelJson() { }
    static JsonObject parse(String text) {
        try {
            if (text == null || text.length() > 100000) throw new IllegalArgumentException();
            JsonReader reader = new JsonReader(new StringReader(text));
            reader.setStrictness(Strictness.STRICT);
            JsonElement parsed = read(reader,0);
            if (reader.peek() != JsonToken.END_DOCUMENT || !parsed.isJsonObject()) throw new IllegalArgumentException();
            return parsed.getAsJsonObject();
        } catch (Exception ex) { throw new ModelFailure(ModelFailure.Code.INVALID_MODEL_OUTPUT); }
    }
    private static JsonElement read(JsonReader reader,int depth) throws java.io.IOException {
        if(depth>20) throw new IllegalArgumentException();
        return switch(reader.peek()) {
            case BEGIN_OBJECT -> {
                JsonObject object=new JsonObject(); reader.beginObject();
                while(reader.hasNext()) {
                    String name=reader.nextName();
                    if(object.has(name)) throw new IllegalArgumentException();
                    object.add(name,read(reader,depth+1));
                }
                reader.endObject(); yield object;
            }
            case BEGIN_ARRAY -> {
                JsonArray array=new JsonArray(); reader.beginArray();
                while(reader.hasNext()) {
                    if(array.size()>=500) throw new IllegalArgumentException();
                    array.add(read(reader,depth+1));
                }
                reader.endArray(); yield array;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(new java.math.BigDecimal(reader.nextString()));
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw new IllegalArgumentException();
        };
    }
    static void keys(JsonObject object, String... expected) {
        if (!object.keySet().equals(Set.of(expected))) throw new IllegalArgumentException();
    }
    static String text(JsonObject object, String field, int max) {
        var value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException();
        String result = value.getAsString();
        if (result.isBlank() || result.length() > max) throw new IllegalArgumentException();
        return result;
    }
}
