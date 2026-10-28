package com.mirrorself.retrieval;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import com.mirrorself.common.Hashing;
import com.mirrorself.retrieval.evidence.*;
import com.mirrorself.retrieval.evidence.EvidenceSupportJudge.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Reuses captured evidence decisions; zero embeddings, zero DB, no reference edits. */
class EvidenceAggregateReplayTest {
    @Test void replaysImmutableFirstRunWithPropositionScopedConflictVeto() throws Exception {
        String raw=Files.readString(Path.of("evaluation/runs/bge-m3-v2-first/report.json"));
        assertEquals("9e250a8ca96421317bcb20b22ac21470f6b259170b87146c543349fd47b18b5d",Hashing.sha256(raw));
        var source=JsonParser.parseString(raw).getAsJsonObject();var gson=new Gson();var judge=new ConservativeEvidenceJudge();
        var modes=new ArrayList<Map<String,Object>>();
        for(var element:source.getAsJsonArray("modes")) {
            var mode=element.getAsJsonObject();int before=0,after=0;var cases=new ArrayList<Map<String,Object>>();
            for(var item:mode.getAsJsonArray("cases")) {
                var c=item.getAsJsonObject();var decisions=new ArrayList<Decision>();
                for(var evidence:c.getAsJsonArray("evidence")) decisions.add(gson.fromJson(evidence.getAsJsonObject().get("decision"),Decision.class));
                var conclusion=judge.conclude(decisions);String expected=c.get("expectedAggregate").getAsString();
                String old=c.getAsJsonObject("aggregate").get("relation").getAsString();
                if(old.equals(expected)) before++;if(conclusion.relation().name().equals(expected)) after++;
                String id=c.get("caseId").getAsString();
                if("R2Q01".equals(id)) assertEquals(Relation.REFUTES,conclusion.relation());
                if("R2Q02".equals(id)) assertEquals(Relation.SUPPORTS,conclusion.relation());
                if("R2Q24".equals(id)||"R2Q25".equals(id)) assertEquals(Relation.INSUFFICIENT_EVIDENCE,conclusion.relation());
                cases.add(Map.of("caseId",id,"originalAggregate",old,"replayedAggregate",conclusion,"expectedAggregate",expected));
            }
            modes.add(Map.of("mode",mode.get("mode").getAsString(),"originalCorrect",before,"replayCorrect",after,
                "total",cases.size(),"cases",cases));
        }
        var report=new LinkedHashMap<String,Object>();report.put("condition","OFFLINE_AGGREGATE_REPLAY_OF_REAL_RETRIEVAL");
        report.put("sourceReportSha256",Hashing.sha256(raw));report.put("sourceCodeRevision",source.get("codeRevision").getAsString());
        report.put("aggregateVersion",ConservativeEvidenceJudge.VERSION);report.put("perEvidenceJudgements","UNCHANGED_FROM_FIRST_RUN_evidence-rules-1");
        report.put("datasetSha256",source.get("datasetSha256").getAsString());report.put("referenceSha256",source.get("referenceSha256").getAsString());
        report.put("newModelRequests",0);report.put("retrievalMetricsChanged",false);report.put("formalBaseline",false);report.put("modes",modes);
        Path output=Path.of("target/evaluation/retrieval-aggregate-replay-v1.1");Files.createDirectories(output);
        Files.writeString(output.resolve("report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
        var markdown=new StringBuilder("# Offline aggregate replay, zero new model requests\n\nOnly direct evidence for the tested proposition may veto a conclusion via a recorded conflict.\nCaptured per-evidence decisions and all first-run retrieval metrics remain unchanged.\n\n| Mode | Original aggregate correct | Replayed aggregate correct | Total |\n|---|---:|---:|---:|\n");
        for(var m:modes) markdown.append("| ").append(m.get("mode")).append(" | ").append(m.get("originalCorrect")).append(" | ")
            .append(m.get("replayCorrect")).append(" | ").append(m.get("total")).append(" |\n");
        Files.writeString(output.resolve("report.md"),markdown);
    }
}
