package com.mirrorself.evaluation;

import com.google.gson.*;
import com.mirrorself.ingestion.SourceRepository;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import com.mirrorself.model.*;
import java.nio.file.*;
import java.util.*;

/** Recorded, intentionally fallible Mock outputs; never derives predictions from gold annotations. */
final class FixtureModelGateway implements ModelGateway {
    private final SourceRepository sources;
    private final JsonObject cases;
    FixtureModelGateway(SourceRepository sources,Path fixture) throws java.io.IOException {
        this.sources=sources;
        JsonObject root=JsonParser.parseString(Files.readString(fixture)).getAsJsonObject();
        if(!root.get("fictional").getAsBoolean())throw new IllegalArgumentException("fictional fixtures only");
        this.cases=root.getAsJsonObject("cases");
    }
    @Override public String generate(UUID owner,UUID grant,String instructions,String input,List<SourceView> batch) {
        if(batch.isEmpty())throw new IllegalArgumentException("fixture needs source context");
        String locator=batch.getFirst().locator();String id=locator.substring(0,locator.lastIndexOf(':'));
        var fixture=cases.getAsJsonObject(id);
        if(fixture==null)throw new IllegalArgumentException("unknown fixture case");
        if(instructions.startsWith("Validate")) {
            var proposals=JsonParser.parseString(input).getAsJsonObject().getAsJsonArray("proposals");
            JsonArray decisions=new JsonArray();
            for(int i=0;i<proposals.size();i++) {
                String content=proposals.get(i).getAsJsonObject().get("content").getAsString();
                boolean supported=false;
                for(var prediction:fixture.getAsJsonArray("predictions")) {
                    var p=prediction.getAsJsonObject();
                    if(p.get("content").getAsString().equals(content))supported=p.get("supported").getAsBoolean();
                }
                JsonObject row=new JsonObject();row.addProperty("index",i);row.addProperty("supported",supported);decisions.add(row);
            }
            JsonObject response=new JsonObject();response.add("decisions",decisions);return response.toString();
        }
        String responseMode=fixture.get("responseMode").getAsString();
        if(responseMode.equals("MALFORMED"))return "not-json-fictional-fixture";
        if(responseMode.equals("UNAVAILABLE"))throw new ModelFailure(ModelFailure.Code.MODEL_UNAVAILABLE);
        Map<String,UUID> ids=new HashMap<>();
        for(var source:sources.list(owner,500,0))ids.put(source.locator().substring(source.locator().lastIndexOf(':')+1),source.id());
        JsonArray predictions=new JsonArray();
        for(var element:fixture.getAsJsonArray("predictions")) {
            var original=element.getAsJsonObject();JsonObject prediction=original.deepCopy();prediction.remove("supported");
            JsonArray refs=new JsonArray();
            for(var ref:original.getAsJsonArray("evidence")) {
                var e=ref.getAsJsonObject();String mid=e.get("messageId").getAsString();
                JsonObject value=new JsonObject();value.addProperty("sourceId",ids.getOrDefault(mid,
                        UUID.nameUUIDFromBytes((id+":"+mid).getBytes(java.nio.charset.StandardCharsets.UTF_8))).toString());
                value.addProperty("quote",e.get("quote").getAsString());refs.add(value);
            }
            prediction.add("evidence",refs);predictions.add(prediction);
        }
        JsonObject result=new JsonObject();result.add("memories",predictions);return result.toString();
    }
}
