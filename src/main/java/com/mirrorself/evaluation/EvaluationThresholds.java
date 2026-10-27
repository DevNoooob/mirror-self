package com.mirrorself.evaluation;

import java.util.*;

/** Separate from exploration: no default live gate, and null/undefined metrics never silently pass. */
public record EvaluationThresholds(Map<String,Double> minimum,Map<String,Double> maximum,
        int maxPolicyViolations, int maxPendingReview) {
    public EvaluationThresholds {
        minimum=Map.copyOf(Objects.requireNonNull(minimum));maximum=Map.copyOf(Objects.requireNonNull(maximum));
        if(maxPolicyViolations<0||maxPendingReview<0)throw new IllegalArgumentException("invalid evaluation thresholds");
        for(double limit:java.util.stream.Stream.concat(minimum.values().stream(),maximum.values().stream()).toList())
            if(!Double.isFinite(limit)||limit<0||limit>1)throw new IllegalArgumentException("invalid evaluation thresholds");
    }
    public List<String> violations(EvaluationReport report) {
        List<String> errors=new ArrayList<>();
        Map<String,EvaluationReport.Metric> metrics=report.overall().persisted().metrics();
        minimum.forEach((name,limit)->{
            var metric=metrics.get(name);
            if(metric==null||metric.value()==null||metric.value()<limit)errors.add("minimum:"+name);
        });
        maximum.forEach((name,limit)->{
            var metric=metrics.get(name);
            if(metric==null||metric.value()==null||metric.value()>limit)errors.add("maximum:"+name);
        });
        if(report.overall().policyViolations()>maxPolicyViolations)errors.add("policyViolations");
        if(report.overall().persisted().counts().pendingReview()>maxPendingReview)errors.add("pendingReview");
        return errors;
    }
}
