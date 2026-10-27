package com.mirrorself.evaluation;

import java.util.*;

/** Explicit reviewed case scope; absence never means "send the entire corpus". */
public final class EvaluationSelection {
    private EvaluationSelection() { }
    public static DatasetLoader.Loaded reviewed(DatasetLoader.Loaded loaded,String requested,String approved,boolean reviewed) {
        if(!reviewed)throw new IllegalStateException("Live evaluation needs explicit Gold review attestation");
        Set<String> ids=parse(requested);
        if(!ids.equals(parse(approved)))throw new IllegalArgumentException("Live case selection differs from approved scope");
        var d=loaded.dataset();var cases=d.cases().stream().filter(c->ids.contains(c.caseId())).toList();
        if(cases.size()!=ids.size())throw new IllegalArgumentException("Unknown Live evaluation case ID");
        // Keep the digest of the complete source file; selected IDs are independently approved and reported.
        return new DatasetLoader.Loaded(new EvaluationDataset(d.schemaVersion(),d.datasetVersion(),d.fictional(),
                d.goldProvenance(),d.humanReviewStatus(),cases),loaded.sha256());
    }
    private static Set<String> parse(String csv) {
        if(csv==null||csv.isBlank())throw new IllegalArgumentException("Explicit Live case IDs are required");
        Set<String> ids=new TreeSet<>();
        for(String item:csv.split(",",-1)) {
            String id=item.strip();
            if(!id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")||!ids.add(id))
                throw new IllegalArgumentException("Invalid or duplicate Live case IDs");
        }
        return Set.copyOf(ids);
    }
}
