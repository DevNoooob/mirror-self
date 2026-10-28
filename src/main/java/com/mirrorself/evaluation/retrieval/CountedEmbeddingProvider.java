package com.mirrorself.evaluation.retrieval;

import com.mirrorself.embedding.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Evaluation-only decorator: bounded calls, no text/response logging or caches. */
public class CountedEmbeddingProvider implements EmbeddingProvider {
    private final EmbeddingProvider delegate;
    private final AtomicInteger calls=new AtomicInteger(),failures=new AtomicInteger();
    private final int budget;
    public CountedEmbeddingProvider(EmbeddingProvider delegate,int budget) {this.delegate=delegate;this.budget=budget;}
    public Model model() {return delegate.model();}
    public Location location() {return delegate.location();}
    public synchronized float[] embed(String text) {
        if(failures.get()>0) throw new EmbeddingFailure(EmbeddingFailure.Code.UNAVAILABLE);
        if(calls.get()>=budget) throw new EmbeddingFailure(EmbeddingFailure.Code.UNAVAILABLE);
        calls.incrementAndGet();
        try {return delegate.embed(text);} catch(RuntimeException ex) {failures.incrementAndGet();throw ex;}
    }
    public int calls() {return calls.get();}
    public int failures() {return failures.get();}
}
