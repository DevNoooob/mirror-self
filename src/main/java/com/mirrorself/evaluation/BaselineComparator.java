package com.mirrorself.evaluation;

import java.util.*;
import static com.mirrorself.evaluation.EvaluationReport.*;

public final class BaselineComparator {
    public BaselineComparison compare(EvaluationReport baseline,EvaluationReport current) {
        if(!Objects.equals(baseline.metricVersion(),current.metricVersion()))return incompatible("metric_version_mismatch");
        if(!Objects.equals(baseline.run().datasetSha256(),current.run().datasetSha256()))return incompatible("dataset_hash_mismatch");
        if(!Objects.equals(baseline.run().mode(),current.run().mode()))return incompatible("mode_mismatch_mock_is_not_live_quality");
        if(!baseline.cases().stream().map(CaseResult::caseId).sorted().toList().equals(
                current.cases().stream().map(CaseResult::caseId).sorted().toList()))return incompatible("case_selection_mismatch");
        Map<String,Double> delta=deltas(baseline.overall(),current.overall());
        Map<String,Map<String,Double>> categories=new TreeMap<>();
        current.categories().forEach((name,value)->{
            if(baseline.categories().containsKey(name))categories.put(name,deltas(baseline.categories().get(name),value));
        });
        Set<String> before=failures(baseline),after=failures(current);
        List<String> added=after.stream().filter(k->!before.contains(k)).sorted().toList();
        List<String> resolved=before.stream().filter(k->!after.contains(k)).sorted().toList();
        return new BaselineComparison(true,"same_dataset_and_metric_contract",delta,categories,added,resolved);
    }
    public EvaluationReport attach(EvaluationReport current,EvaluationReport baseline) {
        return new EvaluationReport(current.reportVersion(),current.metricVersion(),current.run(),current.overall(),
                current.categories(),current.cases(),compare(baseline,current),current.optimizationPriorities());
    }
    private BaselineComparison incompatible(String reason) {return new BaselineComparison(false,reason,Map.of(),Map.of(),List.of(),List.of());}
    private Map<String,Double> deltas(Aggregate a,Aggregate b) {
        Map<String,Double> result=new TreeMap<>();
        for(String stage:List.of("extracted","persisted")) {
            var old=stage.equals("extracted")?a.extracted():a.persisted();
            var next=stage.equals("extracted")?b.extracted():b.persisted();
            next.metrics().forEach((name,metric)->{
                var previous=old.metrics().get(name);
                if(metric.value()!=null&&previous!=null&&previous.value()!=null)
                    result.put(stage+"."+name,metric.value()-previous.value());
            });
        }
        if(a.deduplicationAccuracy().value()!=null&&b.deduplicationAccuracy().value()!=null)
            result.put("deduplicationAccuracy",b.deduplicationAccuracy().value()-a.deduplicationAccuracy().value());
        if(a.privacyViolationRate().value()!=null&&b.privacyViolationRate().value()!=null)
            result.put("privacyViolationRate",b.privacyViolationRate().value()-a.privacyViolationRate().value());
        result.put("policyViolations",(double)b.policyViolations()-a.policyViolations());
        return result;
    }
    private Set<String> failures(EvaluationReport report) {
        Set<String> result=new TreeSet<>();
        for(var c:report.cases())for(var f:c.findings())
            result.add(c.caseId()+":"+f.stage()+":"+f.kind()+":"+(f.memoryId()==null?"-":f.memoryId()));
        return result;
    }
}
