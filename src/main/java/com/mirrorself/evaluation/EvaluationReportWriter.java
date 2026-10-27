package com.mirrorself.evaluation;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static com.mirrorself.evaluation.EvaluationReport.*;

/** Deliberately does not serialize observations, source messages, generated text or provider responses. */
public final class EvaluationReportWriter {
    public void write(EvaluationReport report,Path directory) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("report.json"),EvaluationJson.GSON.toJson(report));
        Files.writeString(directory.resolve("report.md"),markdown(report));
    }
    public EvaluationReport read(Path path) throws IOException {
        return EvaluationJson.GSON.fromJson(Files.readString(path),EvaluationReport.class);
    }
    public String markdown(EvaluationReport r) {
        StringBuilder out=new StringBuilder("# Mirror Self Memory Quality Report\n\n");
        out.append("Mode: `").append(r.run().mode()).append("`\n\n");
        if(r.run().mode().equals("OFFLINE_MOCK"))out.append("**Fixed Mock outputs exercise the evaluator and real pipeline; these scores are NOT live LLM quality.**\n\n");
        if(r.run().mode().equals("OFFLINE_RULE"))out.append("**Measured rule-extractor behavior; these scores are NOT LLM quality.**\n\n");
        out.append("Gold review: `").append(r.run().humanReviewStatus()).append("`. Unknown paraphrases require human review.\n\n")
                .append("Dataset: `").append(r.run().datasetVersion()).append("` / `").append(r.run().datasetSha256()).append("`\n\n")
                .append("Model: `").append(r.run().model()).append("`; extractor: `").append(r.run().extractorVersion()).append("`\n\n")
                .append("Code: `").append(r.run().codeVersion()).append("`; dirty: ").append(r.run().workingTreeDirty()).append("\n\n")
                .append("Started: ").append(r.run().startedAt()).append("; elapsed: ").append(r.run().elapsedMs()).append("ms; calls: ")
                .append(r.run().modelCalls()).append("; tokens: N/A (").append(r.run().usageAvailability()).append(")\n\n");
        out.append("Precision/recall/F1 are conservative confirmed-match scores; pending matches are reported, not silently labeled FP. Zero denominators are N/A.\n\n");
        out.append("| Metric | Extracted numerator/denominator | Extracted | Persisted numerator/denominator | Persisted |\n|---|---:|---:|---:|---:|\n");
        r.overall().extracted().metrics().forEach((name,m)->{
            var stored=r.overall().persisted().metrics().get(name);
            out.append("| ").append(name).append(" | ").append(m.numerator()).append('/').append(m.denominator()).append(" | ")
                    .append(value(m)).append(" | ").append(stored.numerator()).append('/').append(stored.denominator()).append(" | ")
                    .append(value(stored)).append(" |\n");
        });
        out.append("\nDeduplication: ").append(r.overall().deduplicationAccuracy().numerator()).append('/')
                .append(r.overall().deduplicationAccuracy().denominator()).append("; unauthorized gateway invocations: ")
                .append(r.overall().unauthorizedCalls()).append("; policy violation events: ").append(r.overall().policyViolations()).append("; cases violating privacy/review policy: ")
                .append(r.overall().privacyViolationRate().numerator()).append('/').append(r.overall().privacyViolationRate().denominator()).append(".\n\n");
        out.append("## Category metrics\n\n| Category | Cases | Extracted P/R/F1 | Persisted P/R/F1 | Attribution errors | Temporal accuracy | Review accuracy | Unsupported rate |\n|---|---:|---|---|---|---|---|---|\n");
        r.categories().forEach((name,a)->out.append("| ").append(name).append(" | ").append(a.caseCount()).append(" | ")
                .append(prf(a.extracted())).append(" | ").append(prf(a.persisted())).append(" | ")
                .append(value(a.extracted().metrics().get("attributionErrorRate"))).append(" | ")
                .append(value(a.persisted().metrics().get("temporalAccuracy"))).append(" | ")
                .append(value(a.persisted().metrics().get("reviewPolicyAccuracy"))).append(" | ")
                .append(value(a.persisted().metrics().get("unsupportedMemoryRate"))).append(" |\n"));
        out.append("\n## Stage differences and failures\n\n| Case | Raw | Stored unique | Rejected by stage | Findings |\n|---|---:|---:|---|---|\n");
        for(var c:r.cases())out.append("| ").append(c.caseId()).append(" | ").append(c.extracted().counts().predictions()).append(" | ")
                .append(c.persisted().counts().predictions()).append(" | ").append(c.rejectedByStage()).append(" | ")
                .append(c.findings().stream().map(f->f.stage()+":"+f.kind()).distinct().toList()).append(" |\n");
        out.append("\n## Baseline comparison\n\nComparable: ").append(r.comparison().comparable()).append("; reason: `")
                .append(r.comparison().reason()).append("`\n\n");
        out.append("| Metric | Delta |\n|---|---:|\n");
        r.comparison().metricDeltas().forEach((name,delta)->out.append("| ").append(name).append(" | ").append(String.format(Locale.ROOT,"%+.6f",delta)).append(" |\n"));
        out.append("\nCategory deltas: `").append(r.comparison().categoryDeltas()).append("`\n\nNew failures: `")
                .append(r.comparison().newFailures()).append("`\n\nResolved failures: `").append(r.comparison().resolvedFailures()).append("`\n\n");
        out.append("## Optimization priorities\n\n");
        for(var priority:r.optimizationPriorities())out.append("- ").append(priority).append('\n');
        return out.toString();
    }
    private String value(Metric metric) {return metric.value()==null?"N/A":String.format(Locale.ROOT,"%.4f",metric.value());}
    private String prf(StageScore score) {return value(score.metrics().get("precision"))+" / "+value(score.metrics().get("recall"))+" / "+value(score.metrics().get("f1"));}
}
