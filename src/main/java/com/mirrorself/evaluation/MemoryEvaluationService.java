package com.mirrorself.evaluation;

import com.google.gson.*;
import com.mirrorself.config.ExtractionProperties;
import com.mirrorself.ingestion.*;
import com.mirrorself.memory.*;
import com.mirrorself.model.ModelAuthorization;
import com.mirrorself.model.ModelFailure;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.jdbc.core.JdbcTemplate;
import static com.mirrorself.evaluation.EvaluationReport.*;

/** Explicit evaluation harness, deliberately NOT a production Spring bean or HTTP endpoint. */
public final class MemoryEvaluationService {
    public enum Mode { OFFLINE_MOCK, OFFLINE_RULE, LIVE }
    private final SourceRepository sources;
    private final MemoryRepository memories;
    private final MemoryExtractionService extraction;
    private final ModelAuthorization authorization;
    private final JdbcTemplate jdbc;
    private final ExtractionProperties properties;
    private final EvaluationGateway calls;
    private final MemoryScorer scorer = new MemoryScorer();
    public MemoryEvaluationService(SourceRepository sources, MemoryRepository memories,
            MemoryExtractionService extraction, ModelAuthorization authorization, JdbcTemplate jdbc,
            ExtractionProperties properties, EvaluationGateway calls) {
        this.sources=sources; this.memories=memories; this.extraction=extraction;
        this.authorization=authorization; this.jdbc=jdbc; this.properties=properties; this.calls=calls;
    }
    public EvaluationReport run(DatasetLoader.Loaded loaded, Mode mode, boolean liveEnabled,
            String approvedDatasetSha, String codeVersion, boolean dirty) throws IOException {
        return run(loaded,mode,liveEnabled,approvedDatasetSha,codeVersion,dirty,false);
    }
    public EvaluationReport run(DatasetLoader.Loaded loaded, Mode mode, boolean liveEnabled,
            String approvedDatasetSha, String codeVersion, boolean dirty,boolean reviewAttested) throws IOException {
        if(mode==Mode.LIVE) {
            EvaluationSafety.requireLiveConsent(loaded,liveEnabled,approvedDatasetSha);
            if(!reviewAttested)throw new IllegalStateException("Live evaluation needs explicit Gold review attestation");
            if(calls.callLimit()==null)throw new IllegalStateException("Live evaluation needs an approved call limit");
        }
        EvaluationSafety.requireIsolatedDatabase(jdbc);
        if((mode==Mode.OFFLINE_RULE)!=(properties.mode()==ExtractionProperties.Mode.RULE))
            throw new IllegalArgumentException("evaluation mode and configured extractor differ");
        Instant started=Instant.now();long clock=System.nanoTime();int initialCalls=calls.calls();
        List<CaseResult> results=new ArrayList<>();List<ReplayCase> replayCases=new ArrayList<>();String extractorVersion=null,model=null;
        for(var c:loaded.dataset().cases()) {
            long caseClock=System.nanoTime();int beforeCalls=calls.calls();UUID owner=UUID.randomUUID();
            List<ChatGptDecoder.Conversation> decoded=new ArrayList<>();
            new ChatGptDecoder().decodeConversations(new ByteArrayInputStream(conversation(c).getBytes(StandardCharsets.UTF_8)),decoded::add);
            if(decoded.size()!=1)throw new IllegalStateException("evaluation conversation decoding failed");
            Map<String,UUID> ids=new LinkedHashMap<>();Map<UUID,String> messageIds=new HashMap<>();
            for(var source:decoded.getFirst().sources()) {
                var saved=sources.save(owner,source);ids.put(source.locator(),saved.id());
                messageIds.put(saved.id(),source.locator().substring(source.locator().lastIndexOf(':')+1));
            }
            if(c.egress()==EvaluationDataset.Egress.RESTRICTED_SOURCE) {
                var user=c.messages().stream().filter(m->"user".equals(m.role())).findFirst().orElseThrow();
                jdbc.update("UPDATE source_record SET sensitivity='RESTRICTED' WHERE id=? AND owner_id=?",
                        ids.get(c.caseId()+":"+user.messageId()),owner);
            }
            Set<String> selected=new LinkedHashSet<>();
            for(var path:decoded.getFirst().paths())for(var locator:path)selected.add(locator.substring(locator.lastIndexOf(':')+1));
            UUID grant=null;
            if(properties.mode()==ExtractionProperties.Mode.LLM && !properties.local() && c.egress()!=EvaluationDataset.Egress.NO_GRANT) {
                List<UUID> selectedIds=selected.stream().map(id->ids.get(c.caseId()+":"+id)).toList();
                grant=authorization.grant(owner,selectedIds,Instant.now().plusSeconds(600),c.egress()==EvaluationDataset.Egress.SENSITIVE_GRANT);
            }
            Trace trace=new Trace(messageIds,c.messages());List<ExtractionJobRepository.Job> firstJobs=new ArrayList<>();
            for(var path:decoded.getFirst().paths()) {
                if(path.isEmpty())continue;
                var job=extraction.extract(owner,path.stream().map(ids::get).toList(),grant,trace);
                firstJobs.add(job);extractorVersion=job.extractorVersion();model=job.model();
            }
            List<MemoryRepository.MemoryView> first=memories.list(owner,null,200);
            Set<UUID> originalIds=new HashSet<>();first.forEach(m->originalIds.add(m.id()));
            var firstRound=ReplayDiagnostics.round(0,firstJobs,trace.proposals,trace.rejectedCounts(),first);
            List<ReplayAttempt> replayDetails=new ArrayList<>();
            Map<UUID,SourceRepository.SourceView> sourceViews=new HashMap<>();
            ids.values().forEach(id->sourceViews.put(id,sources.get(owner,id)));
            int replayChecks=0,successfulReplays=0;
            for(int replay=1;replay<c.repeatExtractions();replay++) {
                int inserted=0,accepted=0;
                var before=memories.list(owner,null,200);
                Trace replayTrace=new Trace(messageIds,c.messages());List<ExtractionJobRepository.Job> replayJobs=new ArrayList<>();
                for(var path:decoded.getFirst().paths())if(!path.isEmpty()) {
                    var replayJob=extraction.extract(owner,path.stream().map(ids::get).toList(),grant,replayTrace);
                    replayJobs.add(replayJob);
                    inserted+=replayJob.newMemories();accepted+=replayJob.acceptedCount();
                }
                var after=memories.list(owner,null,200);
                var round=ReplayDiagnostics.round(replay,replayJobs,replayTrace.proposals,replayTrace.rejectedCounts(),after);
                replayDetails.add(ReplayDiagnostics.assess(firstRound,round,ReplayDiagnostics.ids(before),trace.proposals,replayTrace.proposals,
                        trace.extractionComparable()&&replayTrace.extractionComparable(),after,sourceViews,messageIds));
                if(!first.isEmpty()) {
                    replayChecks++;
                    if(accepted>0 && inserted==0 && after.size()==first.size() && after.stream().map(MemoryRepository.MemoryView::id)
                            .collect(java.util.stream.Collectors.toSet()).equals(originalIds))successfulReplays++;
                }
            }
            replayCases.add(new ReplayCase(c.caseId(),firstRound,List.copyOf(replayDetails)));
            var stored=first.stream().map(m->new EvaluationObservation.Prediction(m.type(),m.content(),m.origin(),m.sensitivity(),
                    ExtractedMemory.TemporalScope.valueOf(m.temporalScope()),m.status(),m.evidence().stream()
                    .map(e->new EvaluationObservation.Evidence(messageIds.getOrDefault(e.sourceId(),"unknown"),e.role(),e.excerpt())).toList())).toList();
            int modelCalls=calls.calls()-beforeCalls;
            var observation=new EvaluationObservation(Set.copyOf(selected),List.copyOf(trace.raw),stored,
                    List.copyOf(trace.rejections),List.copyOf(trace.failures),replayChecks,successfulReplays,modelCalls,
                    c.expectBlocked()&&mode!=Mode.OFFLINE_RULE?modelCalls:0,
                    first.stream().mapToInt(m->m.conflicts().size()).sum(),elapsed(caseClock));
            results.add(scorer.score(c,observation));
        }
        Map<String,Aggregate> categories=new TreeMap<>();
        loaded.dataset().cases().stream().flatMap(c->c.categories().stream()).distinct().sorted().forEach(category->
                categories.put(category,scorer.aggregate(results.stream().filter(c->c.categories().contains(category)).toList())));
        var dataset=loaded.dataset();
        var run=new Run(mode.name(),dataset.datasetVersion(),loaded.sha256(),dataset.goldProvenance(),dataset.humanReviewStatus(),
                model,extractorVersion,safeCodeVersion(codeVersion),dirty,started,elapsed(clock),calls.calls()-initialCalls,
                null,null,"not_exposed_by_ModelGateway",calls.callLimit(),calls.budgetDenied(),mode==Mode.LIVE&&reviewAttested);
        var overall=scorer.aggregate(results);
        return new EvaluationReport("1.2",MemoryScorer.METRIC_VERSION,run,overall,categories,List.copyOf(results),
                new BaselineComparison(false,"baseline_not_supplied",Map.of(),Map.of(),List.of(),List.of()),priorities(overall),ReplayDiagnostics.aggregate(replayCases),replayCases);
    }
    private static long elapsed(long started) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started); }
    private static String safeCodeVersion(String code) { return code!=null&&code.matches("[a-f0-9]{7,40}")?code:"unknown"; }
    private static List<String> priorities(Aggregate a) {
        List<String> result=new ArrayList<>();
        if(a.unauthorizedCalls()>0||a.policyViolations()>0) result.add("P0: review authorization and pending-review enforcement before model-quality tuning");
        if(a.errorCounts().containsKey(EvaluationError.WRONG_ATTRIBUTION))result.add("P1: inspect attribution prompt and semantic-validator rejections");
        if(a.errorCounts().containsKey(EvaluationError.WRONG_TEMPORAL_SCOPE))result.add("P1: inspect temporal qualifiers and reference scope mapping");
        if(a.errorCounts().containsKey(EvaluationError.MISSING_MEMORY))result.add("P2: inspect extraction recall by category, retaining evidence and privacy gates");
        if(a.errorCounts().containsKey(EvaluationError.PENDING_HUMAN_REVIEW))result.add("P2: human-review unresolved paraphrases before changing aliases or baseline");
        if(result.isEmpty())result.add("No deterministic regression found; model quality still requires reviewed live evaluation");
        return List.copyOf(result);
    }
    static String conversation(EvaluationDataset.Case c) {
        JsonObject root=new JsonObject();root.addProperty("id",c.caseId());root.addProperty("title","fictional evaluation");
        if(c.selectedLeaf()!=null)root.addProperty("current_node",c.selectedLeaf());
        JsonObject mapping=new JsonObject();
        for(var m:c.messages()) {
            JsonObject node=new JsonObject();node.addProperty("id",m.messageId());node.addProperty("parent",m.parentId());
            JsonObject message=new JsonObject();message.addProperty("id",m.messageId());
            if(m.statedAt()!=null)message.addProperty("create_time",m.statedAt().getEpochSecond());
            JsonObject author=new JsonObject();author.addProperty("role",m.role());message.add("author",author);
            JsonObject content=new JsonObject();JsonArray parts=new JsonArray();parts.add(m.text());content.add("parts",parts);
            message.add("content",content);node.add("message",message);mapping.add(m.messageId(),node);
        }
        root.add("mapping",mapping);JsonArray all=new JsonArray();all.add(root);return all.toString();
    }
    private static final class Trace implements ExtractionObserver {
        final Map<UUID,String> ids;
        final List<EvaluationObservation.Prediction> raw=new ArrayList<>();
        final List<ExtractedMemory> proposals=new ArrayList<>();
        boolean extractionComparable() {return failures.stream().noneMatch(f->f.stage()==Stage.AUTHORIZATION||f.stage()==Stage.EXTRACTION);}
        Map<String,Integer> rejectedCounts() {
            Map<String,Integer> counts=new TreeMap<>();rejections.forEach(r->counts.merge(r.stage().name(),1,Integer::sum));return counts;
        }
        final List<EvaluationObservation.Rejection> rejections=new ArrayList<>();
        final List<EvaluationObservation.Failure> failures=new ArrayList<>();
        final Map<ExtractedMemory,Integer> indexes=new IdentityHashMap<>();
        final Map<String,String> roles=new HashMap<>();
        Trace(Map<UUID,String> ids,List<EvaluationDataset.Message> messages) {
            this.ids=ids;messages.forEach(m->roles.put(m.messageId(),m.role()));
        }
        @Override public void extracted(List<SourceRepository.SourceView> batch,List<ExtractedMemory> candidates) {
            proposals.addAll(candidates);
            for(var p:candidates) {
                indexes.put(p,raw.size());
                raw.add(new EvaluationObservation.Prediction(p.type(),p.content(),p.origin(),p.sensitivity(),p.temporalScope(),null,
                        p.evidence().stream().map(e->new EvaluationObservation.Evidence(ids.getOrDefault(e.sourceId(),"unknown"),
                                roles.getOrDefault(ids.get(e.sourceId()),"unknown"),e.quote())).toList()));
            }
        }
        @Override public void rejected(Stage stage,ExtractedMemory candidate) {
            rejections.add(new EvaluationObservation.Rejection(stage,indexes.getOrDefault(candidate,-1)));
        }
        @Override public void failed(Stage stage,ModelFailure.Code code) {
            failures.add(new EvaluationObservation.Failure(stage,code.name()));
        }
    }
}
