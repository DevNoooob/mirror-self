package com.mirrorself.evaluation;

import com.mirrorself.memory.*;
import java.util.*;
import static com.mirrorself.evaluation.EvaluationReport.*;
import static com.mirrorself.evaluation.EvaluationError.*;

/** Deterministic one-to-one reference matching. Unknown paraphrases are never silently called false. */
public final class MemoryScorer {
    public static final String METRIC_VERSION = "memory-quality-1.0";

    public CaseResult score(EvaluationDataset.Case c, EvaluationObservation observation) {
        List<Finding> findings = new ArrayList<>();
        List<CandidateAudit> audit = new ArrayList<>();
        var raw = scoreStage(c, observation.extracted(), observation.selectedMessageIds(), "EXTRACTION", findings,audit);
        var stored = scoreStage(c, observation.persisted(), observation.selectedMessageIds(), "PERSISTENCE", findings,audit);
        Map<String,Integer> rejected = new TreeMap<>();
        for (var rejection : observation.rejections()) {
            rejected.merge(rejection.stage().name(), 1, Integer::sum);
            findings.add(new Finding(rejection.stage().name(), VALIDATOR_REJECTION, null, rejection.candidateIndex()));
        }
        for (var failure : observation.failures()) {
            EvaluationError kind = switch (failure.code()) {
                case "INVALID_MODEL_OUTPUT" -> failure.stage() == ExtractionObserver.Stage.EXTRACTION
                        ? MODEL_FORMAT_ERROR : VALIDATOR_FORMAT_ERROR;
                case "MODEL_UNAVAILABLE" -> MODEL_UNAVAILABLE;
                case "EGRESS_DENIED" -> c.expectBlocked() ? null : UNEXPECTED_BLOCK;
                default -> PERSISTENCE_ERROR;
            };
            if (kind != null) findings.add(new Finding(failure.stage().name(), kind, null, null));
        }
        if (observation.unauthorizedCalls() > 0) findings.add(new Finding("AUTHORIZATION", SENSITIVE_DATA_VIOLATION, null, null));
        if (c.expectConflict() && observation.conflictEdges() == 0)
            findings.add(new Finding("PERSISTENCE", MISSING_CONFLICT, null, null));
        if (observation.successfulReplays() < observation.replayChecks())
            findings.add(new Finding("PERSISTENCE", DUPLICATE_MEMORY, null, null));
        boolean expectedFailureOnly = observation.failures().stream().allMatch(f ->
                c.expectBlocked() && "EGRESS_DENIED".equals(f.code()));
        boolean correctEmpty = c.expectedMemories().isEmpty() && observation.persisted().isEmpty()
                && expectedFailureOnly && observation.unauthorizedCalls() == 0;
        return new CaseResult(c.caseId(), c.categories(), raw, stored, List.copyOf(findings), rejected,
                observation.replayChecks(), observation.successfulReplays(), observation.modelCalls(),
                observation.unauthorizedCalls(), observation.elapsedMs(), correctEmpty,List.copyOf(audit));
    }

    private StageScore scoreStage(EvaluationDataset.Case c, List<EvaluationObservation.Prediction> predictions,
            Set<String> selected, String stage, List<Finding> findings,List<CandidateAudit> audit) {
        Map<String,EvaluationDataset.Message> messages = new HashMap<>();
        c.messages().forEach(m -> messages.put(m.messageId(), m));
        List<List<Integer>> semantics = new ArrayList<>(), edges = new ArrayList<>();
        List<Set<EvaluationError>> violations = new ArrayList<>();
        boolean[] evidence = new boolean[predictions.size()];
        for (int i=0; i<predictions.size(); i++) {
            var p = predictions.get(i);
            List<Integer> semantic = new ArrayList<>(), compatible = new ArrayList<>();
            for (int g=0; g<c.expectedMemories().size(); g++) if (semanticallyMatches(p.content(), c.expectedMemories().get(g))) semantic.add(g);
            Set<EvaluationError> errors = new LinkedHashSet<>();
            for (var forbidden : c.forbiddenClaims()) if (forbidden.phrases().stream()
                    .anyMatch(phrase -> canonical(p.content()).contains(canonical(phrase)))) errors.add(forbidden.kind());
            boolean located = p.evidence() != null && !p.evidence().isEmpty();
            if (p.evidence() != null) for (var e : p.evidence()) {
                var source = messages.get(e.messageId());
                if (source != null && !selected.contains(e.messageId())) errors.add(WRONG_BRANCH);
                if (source != null && (!"user".equals(source.role()) || !"user".equals(e.role()))) errors.add(WRONG_ATTRIBUTION);
                located &= source != null && selected.contains(e.messageId()) && "user".equals(source.role())
                        && "user".equals(e.role()) && e.quote() != null && !e.quote().isBlank() && source.text().contains(e.quote());
            }
            boolean expectedAnchor = semantic.isEmpty() || semantic.stream().anyMatch(g -> expectedEvidence(p, c.expectedMemories().get(g)));
            evidence[i] = located && expectedAnchor;
            if (!evidence[i]) errors.add(INVALID_EVIDENCE);
            if (located && errors.isEmpty()) for (int g : semantic) {
                var gold = c.expectedMemories().get(g);
                if (p.type() == gold.type() && p.origin() == gold.origin()
                        && p.temporalScope() == gold.expectedTemporalScope() && expectedEvidence(p,gold)) compatible.add(g);
            }
            semantics.add(semantic); edges.add(compatible); violations.add(errors);
        }
        int[] match = new int[c.expectedMemories().size()]; Arrays.fill(match,-1);
        for (int i=0;i<predictions.size();i++) augment(i,edges,match,new boolean[match.length]);
        int[] goldForPrediction = new int[predictions.size()];Arrays.fill(goldForPrediction,-1);
        for (int g=0;g<match.length;g++) if(match[g]>=0) goldForPrediction[match[g]]=g;
        int tp=0,fp=0,pending=0,validEvidence=0,attribution=0,temporalCorrect=0,temporalAssessed=0,
                reviewCorrect=0,reviewAssessed=0,unsupported=0,sensitive=0;
        Set<Integer> possiblePendingGold = new HashSet<>();
        for(int i=0;i<predictions.size();i++) {
            var p=predictions.get(i);var errors=violations.get(i);var semantic=semantics.get(i);
            int g=goldForPrediction[i]>=0?goldForPrediction[i]:bestReference(p,c,semantic);
            var gold=g<0?null:c.expectedMemories().get(g);
            if(evidence[i]) validEvidence++;
            boolean unsupportedClaim=!evidence[i] || errors.contains(UNSUPPORTED_CLAIM)
                    || errors.contains(WRONG_ATTRIBUTION) || errors.contains(WRONG_TEMPORAL_SCOPE);
            if(gold!=null) {
                if(p.type()!=gold.type()) errors.add(WRONG_MEMORY_TYPE);
                if(p.origin()!=gold.origin()) errors.add(WRONG_ASSERTION_ORIGIN);
                temporalAssessed++;
                if(p.temporalScope()==gold.expectedTemporalScope() && !errors.contains(WRONG_TEMPORAL_SCOPE)) temporalCorrect++;
                else errors.add(WRONG_TEMPORAL_SCOPE);
                if(p.status()!=null) {
                    reviewAssessed++;
                    if(p.status()==gold.expectedReviewStatus()) reviewCorrect++;
                    else errors.add(REVIEW_POLICY_VIOLATION);
                }
                if(p.status()==MemoryStatus.ACTIVE && gold.sensitivity()!=Sensitivity.NORMAL) errors.add(SENSITIVE_DATA_VIOLATION);
            } else {
                if(errors.contains(WRONG_TEMPORAL_SCOPE)) temporalAssessed++;
                if(p.status()!=null && (p.origin()==AssertionOrigin.MODEL_INFERRED
                        ||p.type()==MemoryType.THOUGHT_PROCESS||p.sensitivity()!=Sensitivity.NORMAL)) {
                    reviewAssessed++;
                    if(p.status()==MemoryStatus.PENDING_REVIEW)reviewCorrect++;else errors.add(REVIEW_POLICY_VIOLATION);
                }
            }
            if(p.status()==MemoryStatus.ACTIVE && p.sensitivity()!=Sensitivity.NORMAL) errors.add(SENSITIVE_DATA_VIOLATION);
            CandidateOutcome outcome;
            if(goldForPrediction[i]>=0) { tp++;outcome=CandidateOutcome.CONFIRMED_TP; }
            else if(!edges.get(i).isEmpty()) { fp++;errors.add(DUPLICATE_MEMORY);outcome=CandidateOutcome.CONFIRMED_FP; }
            else if(errors.isEmpty() && semantic.isEmpty()) {
                pending++;errors.add(PENDING_HUMAN_REVIEW);outcome=CandidateOutcome.PENDING_HUMAN_REVIEW;
                for(int target=0;target<c.expectedMemories().size();target++)
                    if(expectedEvidence(p,c.expectedMemories().get(target)))possiblePendingGold.add(target);
            } else { fp++;outcome=CandidateOutcome.CONFIRMED_FP; }
            if(unsupportedClaim)unsupported++;
            if(errors.contains(WRONG_ATTRIBUTION))attribution++;
            if(errors.contains(SENSITIVE_DATA_VIOLATION))sensitive++;
            var pointers=p.evidence()==null?List.<EvidencePointer>of():p.evidence().stream().map(e->{
                var source=messages.get(e.messageId());
                int start=source==null||e.quote()==null||e.quote().isEmpty()?-1:source.text().indexOf(e.quote());
                return new EvidencePointer(source==null?"unknown":source.messageId(),source==null?"unknown":source.role(),
                        start,start<0?-1:start+e.quote().length(),source!=null&&selected.contains(source.messageId()));
            }).toList();
            audit.add(new CandidateAudit(stage,i,outcome,gold==null?null:gold.memoryId(),p.type(),p.origin(),
                    p.temporalScope(),p.status(),pointers,List.copyOf(errors)));
            for(var error:errors)findings.add(new Finding(stage,error,gold==null?null:gold.memoryId(),i));
        }
        int fn=0;
        for(int g=0;g<match.length;g++)if(match[g]<0) {
            fn++;
            if(!possiblePendingGold.contains(g))findings.add(new Finding(stage,MISSING_MEMORY,c.expectedMemories().get(g).memoryId(),null));
        }
        return stage(new Counts(tp,fp,fn,pending,predictions.size(),c.expectedMemories().size(),validEvidence,
                attribution,temporalCorrect,temporalAssessed,reviewCorrect,reviewAssessed,unsupported,sensitive));
    }
    private static boolean augment(int prediction,List<List<Integer>> edges,int[] match,boolean[] visited) {
        for(int gold:edges.get(prediction))if(!visited[gold]) {
            visited[gold]=true;
            if(match[gold]<0||augment(match[gold],edges,match,visited)) {match[gold]=prediction;return true;}
        }
        return false;
    }
    private static int bestReference(EvaluationObservation.Prediction p,EvaluationDataset.Case c,List<Integer> matches) {
        return matches.stream().max(Comparator.comparingInt(i->{var g=c.expectedMemories().get(i);
            return (p.type()==g.type()?4:0)+(p.origin()==g.origin()?2:0)+(p.temporalScope()==g.expectedTemporalScope()?1:0);
        })).orElse(-1);
    }
    private static boolean expectedEvidence(EvaluationObservation.Prediction p,EvaluationDataset.Gold g) {
        if(p.evidence()==null)return false;
        var matches=g.expectedEvidence().stream().map(expected->p.evidence().stream().anyMatch(actual->
                expected.messageId().equals(actual.messageId())&&expected.role().equals(actual.role())
                        &&actual.quote()!=null&&actual.quote().contains(expected.quote()))).toList();
        return g.evidencePolicy()==EvaluationDataset.EvidencePolicy.ALL?matches.stream().allMatch(Boolean::booleanValue)
                :matches.stream().anyMatch(Boolean::booleanValue);
    }
    public static boolean semanticallyMatches(String content,EvaluationDataset.Gold gold) {
        String value=canonical(content);
        if(gold.excludedConcepts().stream().anyMatch(s->value.contains(canonical(s))))return false;
        if(gold.acceptedContents().stream().anyMatch(s->value.equals(canonical(s))))return true;
        return !gold.requiredConceptGroups().isEmpty() && gold.requiredConceptGroups().stream()
                .allMatch(group->group.stream().anyMatch(s->value.contains(canonical(s))));
    }
    private static String canonical(String text) { return text==null?"":MemoryNormalization.canonical(text); }
    public static StageScore stage(Counts c) {
        Map<String,Metric> metrics=new LinkedHashMap<>();
        metrics.put("precision",Metric.of(c.tp(),c.predictions()));
        metrics.put("recall",Metric.of(c.tp(),c.gold()));
        metrics.put("f1",Metric.of(2L*c.tp(),(long)c.predictions()+c.gold()));
        metrics.put("evidenceAccuracy",Metric.of(c.validEvidence(),c.predictions()));
        metrics.put("attributionErrorRate",Metric.of(c.attributionErrors(),c.predictions()));
        metrics.put("temporalAccuracy",Metric.of(c.temporalCorrect(),c.temporalAssessed()));
        metrics.put("reviewPolicyAccuracy",Metric.of(c.reviewCorrect(),c.reviewAssessed()));
        metrics.put("unsupportedMemoryRate",Metric.of(c.unsupported(),c.predictions()));
        metrics.put("resolvedCoverage",Metric.of(c.predictions()-c.pendingReview(),c.predictions()));
        return new StageScore(c,Collections.unmodifiableMap(metrics));
    }
    public Aggregate aggregate(List<CaseResult> cases) {
        Counts raw=Counts.zero(),stored=Counts.zero();int replay=0,good=0,unauthorized=0,empty=0,policy=0,violatingCases=0;
        Map<EvaluationError,Integer> errors=new EnumMap<>(EvaluationError.class);
        for(var c:cases) {
            raw=raw.plus(c.extracted().counts());stored=stored.plus(c.persisted().counts());
            replay+=c.replayChecks();good+=c.successfulReplays();unauthorized+=c.unauthorizedCalls();
            if(c.correctEmptyResult())empty++;
            if(c.findings().stream().anyMatch(f->Set.of(REVIEW_POLICY_VIOLATION,SENSITIVE_DATA_VIOLATION).contains(f.kind())))violatingCases++;
            for(var f:c.findings()) {
                errors.merge(f.kind(),1,Integer::sum);
                if(Set.of(REVIEW_POLICY_VIOLATION,SENSITIVE_DATA_VIOLATION).contains(f.kind()))policy++;
            }
        }
        return new Aggregate(stage(raw),stage(stored),Metric.of(good,replay),Metric.of(violatingCases,cases.size()),unauthorized,policy,empty,cases.size(),errors);
    }
}
