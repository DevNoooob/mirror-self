package com.mirrorself.embedding;

public final class EmbeddingVectors {
    private EmbeddingVectors() { }
    public static float[] validate(float[] vector) {
        if(vector==null || vector.length!=EmbeddingProvider.DIMENSIONS)
            throw new EmbeddingFailure(EmbeddingFailure.Code.DIMENSION_MISMATCH);
        double norm=0;
        for(float v:vector) {
            if(!Float.isFinite(v)) throw new EmbeddingFailure(EmbeddingFailure.Code.INVALID_VECTOR);
            norm+=(double)v*v;
        }
        if(norm==0) throw new EmbeddingFailure(EmbeddingFailure.Code.INVALID_VECTOR);
        return vector;
    }
    public static String sql(float[] vector) {
        validate(vector);
        var out=new StringBuilder("[");
        for(int i=0;i<vector.length;i++) { if(i>0) out.append(','); out.append(vector[i]); }
        return out.append(']').toString();
    }
}
