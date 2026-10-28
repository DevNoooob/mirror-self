package com.mirrorself.embedding;

public interface EmbeddingProvider {
    int DIMENSIONS=1024;
    enum Location { DISABLED, LOCAL, CLOUD }
    record Model(String id,String version) { }
    Model model();
    Location location();
    float[] embed(String text);
}
