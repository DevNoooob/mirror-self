package com.mirrorself.evaluation;

import com.google.gson.*;
import java.time.Instant;

public final class EvaluationJson {
    private EvaluationJson() { }
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls()
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (value, type, context) ->
                    new JsonPrimitive(value.toString()))
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (value, type, context) ->
                    Instant.parse(value.getAsString())).create();
}
