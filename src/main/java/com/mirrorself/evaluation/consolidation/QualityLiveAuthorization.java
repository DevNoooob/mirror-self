package com.mirrorself.evaluation.consolidation;

import com.mirrorself.config.EmbeddingProperties;
import java.util.Map;

/** New-round consent, independent of all previous Live flags/grants. No network here. */
public final class QualityLiveAuthorization {
    private QualityLiveAuthorization() { }
    public static void check(QualityCorpus.Loaded l,EmbeddingProperties p,Map<String,String> env,String code,String os,int javaVersion) {
        boolean ok="true".equals(env.get("MIRROR_RUN_LOCAL_QG"))&&"new-testcontainers".equals(env.get("MIRROR_ISOLATED_TEST_ENVIRONMENT"))&&os.equals("Linux")&&javaVersion==21
            &&l.planSha256().equals(env.get("MIRROR_QG_APPROVED_PLAN_SHA"))&&l.plan().datasetSha256().equals(env.get("MIRROR_QG_APPROVED_DATASET_SHA"))
            &&l.plan().referenceSha256().equals(env.get("MIRROR_QG_APPROVED_REFERENCE_SHA"))&&l.plan().gateSha256().equals(env.get("MIRROR_QG_APPROVED_GATE_SHA"))
            &&code.matches("[a-f0-9]{40}")&&code.equals(env.get("MIRROR_QG_APPROVED_CODE_SHA"))
            &&"false".equals(env.get("MIRROR_MODEL_EGRESS_ENABLED"))&&"false".equals(env.get("MIRROR_RUN_LIVE_LLM"))&&"false".equals(env.get("MIRROR_EVALUATION_LIVE_ENABLED"))
            &&p.mode()==EmbeddingProperties.Mode.LOCAL&&p.baseUrl().equals(l.plan().endpoint())&&p.model().equals(l.plan().model())&&p.version().equals(l.plan().modelVersion())&&p.apiKey().isEmpty();
        if(!ok||env.keySet().stream().anyMatch(k->(k.startsWith("SPRING_DATASOURCE_")||k.equals("MIRROR_DB_URL")||k.equals("PGHOST")||k.equals("DATABASE_URL"))&&!env.get(k).isBlank()))
            throw new IllegalStateException("QG_LOCAL_AUTHORIZATION_DENIED");
    }
    public static String currentCode() {
        try {var process=new ProcessBuilder("git","rev-parse","HEAD").redirectErrorStream(true).start();String code=new String(process.getInputStream().readNBytes(100),java.nio.charset.StandardCharsets.UTF_8).strip();
            if(process.waitFor()!=0||!code.matches("[a-f0-9]{40}")) throw new IllegalStateException();return code;
        } catch(Exception ex) {throw new IllegalStateException("QG_CODE_IDENTITY_UNAVAILABLE");}
    }
}
