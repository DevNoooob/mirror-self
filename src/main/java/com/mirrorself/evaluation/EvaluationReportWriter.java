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
        out.append("Call limit: ").append(r.run().modelCallLimit()==null?"not recorded / unlimited":r.run().modelCallLimit())
                .append("; budget denials: ").append(r.run().budgetDenied()).append("; operator review attested: ")
                .append(r.run().operatorReviewAttested()).append(". This flag records an operator attestation, not an AI declaration of human approval.\n\n");
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
        if(r.replaySummary()!=null) {
            var summary=r.replaySummary();
            out.append("## Replay diagnostics (separate contract)\n\n")
                .append("Contract: `").append(summary.contractVersion()).append("`. Legacy deduplication/DUPLICATE_MEMORY is a combined replay-success heuristic, NOT proof of duplicate rows. Original scoring is preserved.\n\n")
                .append("Model output consistency: ").append(summary.modelOutputConsistency().numerator()).append('/').append(summary.modelOutputConsistency().denominator())
                .append("; database row-set idempotency: ").append(summary.databaseRowSetIdempotency().numerator()).append('/').append(summary.databaseRowSetIdempotency().denominator())
                .append("; observed exact duplicate groups: ").append(summary.duplicateRowGroups()).append(".\n\n")
                .append("Row-set stability does not prove an accepted write exercised deduplication, nor compare every mutable field. Output comparison uses exact candidate text/evidence and all type/origin/time/sensitivity fields, independent of order.\n\n")
                .append("| Case/round | Extracted/accepted/inserted/rejected | Errors | Before/after/initial IDs | Same model output | DB idempotent | Type drift | Actual duplicate groups | Reasons |\n|---|---|---|---|---|---|---|---|---|\n");
            for(var c:r.replayDiagnostics()) {
                out.append("| ").append(c.caseId()).append("/0 | ").append(c.first().extracted()).append('/').append(c.first().accepted()).append('/').append(c.first().inserted()).append('/').append(c.first().rejected())
                    .append(" | ").append(c.first().errorCodes()).append(" | initial=").append(c.first().memoryIds()).append(" | N/A | N/A | N/A | N/A | first snapshot |\n");
                for(var a:c.replays()) {
                    var v=a.result();out.append("| ").append(c.caseId()).append('/').append(v.round()).append(" | ").append(v.extracted()).append('/').append(v.accepted()).append('/').append(v.inserted()).append('/').append(v.rejected())
                        .append(" | ").append(v.errorCodes()).append(" | ").append(a.beforeIds()).append(" -> ").append(v.memoryIds()).append(" / first=").append(c.first().memoryIds())
                        .append(" | ").append(a.modelOutputConsistent()).append(" | ").append(a.databaseRowSetIdempotent()).append(" | ").append(a.typeDrifts())
                        .append(" | ").append(a.exactDuplicateRows()).append(" | ").append(a.reasons()).append(" |\n");
                }
            }
            out.append("\nFirst-pass persistence metrics use the pre-replay snapshot. Replay diagnostics include every replay round; no source text, candidate body or content hashes are serialized.\n\n");
        }
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
        int evidenceRejected=r.cases().stream().mapToInt(c->c.rejectedByStage().getOrDefault("EVIDENCE_VALIDATION",0)).sum();
        int semanticRejected=r.cases().stream().mapToInt(c->c.rejectedByStage().getOrDefault("SEMANTIC_VALIDATION",0)).sum();
        out.append("\nFirst-extraction validation rejections: evidence=").append(evidenceRejected)
                .append("; semantic=").append(semanticRejected).append(". Replay calls are included only in call/dedup counters.\n\n");
        out.append("## Candidate audit (reference metadata only)\n\n")
                .append("A reference ID on FP means a related Gold, not a confirmed match. Pending content requires protected manual inspection; generated text is deliberately absent.\n\n")
                .append("| Case | Stage/index | Outcome | Reference | Type/origin | Scope/status | Evidence UTF-16 | Rejected at | Errors |\n|---|---|---|---|---|---|---|---|---|\n");
        for(var c:r.cases())if(c.candidates()!=null)for(var a:c.candidates()) {
            var rejected=a.stage().equals("EXTRACTION")?c.findings().stream()
                    .filter(f->f.kind()==EvaluationError.VALIDATOR_REJECTION&&Objects.equals(f.predictionIndex(),a.index()))
                    .map(Finding::stage).distinct().toList():List.<String>of();
            out.append("| ").append(c.caseId()).append(" | ").append(a.stage()).append('/').append(a.index()).append(" | ")
                    .append(a.outcome()).append(" | ").append(a.referenceMemoryId()).append(" | ").append(a.type()).append('/').append(a.origin())
                    .append(" | ").append(a.temporalScope()).append('/').append(a.status()).append(" | ")
                    .append(a.evidence().stream().map(e->e.messageId()+":"+e.role()+"["+e.start()+","+e.end()+") selected="+e.selected()).toList())
                    .append(" | ").append(rejected).append(" | ").append(a.errors()).append(" |\n");
        }
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
