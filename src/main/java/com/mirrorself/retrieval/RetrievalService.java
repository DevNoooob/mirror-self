package com.mirrorself.retrieval;

import com.mirrorself.embedding.*;
import com.mirrorself.memory.MemoryRepository;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class RetrievalService {
    public enum Mode { KEYWORD, VECTOR, HYBRID }
    public record Hit(MemoryRepository.MemoryView memory,double score,List<String> methods,
            Integer keywordRank,Double keywordScore,Integer vectorRank,Double cosineSimilarity) { }
    public record Result(Mode requestedMode,String vectorStatus,String modelId,String modelVersion,
            int candidateLimit,int rrfConstant,long elapsedMs,List<Hit> hits) { }
    private final MemoryRepository memories;
    private final RetrievalRepository repository;
    private final EmbeddingGateway embedding;
    public RetrievalService(MemoryRepository memories,RetrievalRepository repository,EmbeddingGateway embedding) {
        this.memories=memories;this.repository=repository;this.embedding=embedding;
    }
    public List<MemoryRepository.MemoryView> retrieve(UUID owner,String question,int limit) {
        return explain(owner,question,limit,Mode.HYBRID).hits().stream().map(Hit::memory).toList();
    }
    public Result explain(UUID owner,String question,int limit,Mode mode) {
        if(question==null || question.isBlank() || question.length()>1000)
            throw new IllegalArgumentException("query must contain 1-1000 characters");
        if(limit<1 || limit>30) throw new IllegalArgumentException("limit must be between 1 and 30");
        Objects.requireNonNull(mode);
        long start=System.nanoTime(); int pool=100;
        List<RetrievalRepository.Ranked> keyword=List.of(),vector=List.of();String status="NOT_REQUESTED";
        if(mode!=Mode.VECTOR) keyword=repository.keyword(owner,question,pool);
        if(mode!=Mode.KEYWORD) {
            try {
                float[] query=embedding.embed(question);
                vector=repository.vector(owner,query,embedding.model(),pool,0.35);
                status=vector.isEmpty()?"NO_MATCH":"READY";
            } catch(EmbeddingFailure ex) {
                status=ex.code().name();
                // Explicit VECTOR mode also has a visible keyword fallback on provider failure.
                if(mode==Mode.VECTOR) keyword=repository.keyword(owner,question,pool);
            }
        }
        var scores=RrfRanking.rank(keyword,vector);
        var byId=scores.stream().collect(java.util.stream.Collectors.toMap(RrfRanking.Score::id,s->s));
        var hits=memories.retrievable(owner,scores.stream().map(RrfRanking.Score::id).toList()).stream().limit(limit)
                .map(m->{ var s=byId.get(m.id());var methods=new ArrayList<String>();
                    if(s.keywordRank()!=null) methods.add("KEYWORD");if(s.vectorRank()!=null) methods.add("VECTOR");
                    return new Hit(m,s.score(),List.copyOf(methods),s.keywordRank(),s.keywordScore(),s.vectorRank(),s.cosineSimilarity());
                }).toList();
        return new Result(mode,status,embedding.model().id(),embedding.model().version(),pool,RrfRanking.CONSTANT,
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start),hits);
    }
    /** Local extractive answer; history is always labelled as history, never a new current assertion. */
    public ExtractiveAnswer answer(UUID owner,String question) {
        var evidence=retrieve(owner,question,6);
        if(evidence.isEmpty()) return new ExtractiveAnswer("EXTRACTIVE_NO_LLM",
                "没有找到足够的个人记忆证据，暂时无法根据你的历史资料回答。",List.of());
        var answer=new StringBuilder("找到以下可能相关的历史记忆（请根据原始证据核对）：\n");
        for(int i=0;i<evidence.size();i++) answer.append(i+1).append(". ").append(evidence.get(i).content()).append('\n');
        return new ExtractiveAnswer("EXTRACTIVE_NO_LLM",answer.toString(),evidence);
    }
    public record ExtractiveAnswer(String mode,String answer,List<MemoryRepository.MemoryView> evidence) { }
}
