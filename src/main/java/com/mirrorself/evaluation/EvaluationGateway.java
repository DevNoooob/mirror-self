package com.mirrorself.evaluation;

import com.mirrorself.ingestion.SourceRepository.SourceView;
import com.mirrorself.model.ModelGateway;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Counts calls without retaining prompts, responses, credentials or provider exceptions. */
public final class EvaluationGateway implements ModelGateway {
    private final ModelGateway delegate;
    private final AtomicInteger calls = new AtomicInteger();
    private final int limit;
    private final AtomicInteger denied = new AtomicInteger();
    public EvaluationGateway(ModelGateway delegate) { this(delegate,Integer.MAX_VALUE); }
    public EvaluationGateway(ModelGateway delegate,int limit) {
        if(limit<1)throw new IllegalArgumentException("Invalid evaluation call limit");
        this.delegate=java.util.Objects.requireNonNull(delegate);this.limit=limit;
    }
    @Override public String generate(UUID owner, UUID grant, String instructions, String input, List<SourceView> sources) {
        int previous=calls.getAndUpdate(value->value<limit?value+1:value);
        if(previous>=limit) {
            denied.incrementAndGet();
            // A request beyond the explicitly approved quota is not authorized egress.
            throw new com.mirrorself.model.ModelFailure(com.mirrorself.model.ModelFailure.Code.EGRESS_DENIED);
        }
        return delegate.generate(owner, grant, instructions, input, sources);
    }
    public int calls() { return calls.get(); }
    public int budgetDenied() { return denied.get(); }
    public Integer callLimit() { return limit==Integer.MAX_VALUE?null:limit; }
}
