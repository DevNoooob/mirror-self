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
    public EvaluationGateway(ModelGateway delegate) { this.delegate = delegate; }
    @Override public String generate(UUID owner, UUID grant, String instructions, String input, List<SourceView> sources) {
        calls.incrementAndGet();
        return delegate.generate(owner, grant, instructions, input, sources);
    }
    public int calls() { return calls.get(); }
}
