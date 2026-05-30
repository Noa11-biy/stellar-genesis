package com.stellargenesis.core.world.meshing;

public class ChunkMesh {
    private final float[] vertices;   // [x0,y0,z0, x1,y1,z1, ...]
    private final int[] indices;       // [i0,i1,i2, i3,i4,i5, ...]

    public ChunkMesh(float[] vertices, int[] indices) {
        if (vertices == null) throw new IllegalArgumentException("vertices null");
        if (indices == null) throw new IllegalArgumentException("indices null");
        if (vertices.length % 3 != 0)
            throw new IllegalArgumentException("vertices.length doit être multiple de 3, reçu : " + vertices.length);
        if (indices.length % 3 != 0)
            throw new IllegalArgumentException("indices.length doit être multiple de 3, reçu : " + indices.length);

        this.vertices = vertices;
        this.indices = indices;
    }

    /**
     * Retourne le buffer de sommets brut.
     * <p>
     * <b>ATTENTION :</b> ce tableau est retourné par référence pour
     * des raisons de performance (évite une copie de plusieurs Ko à
     * chaque frame). <b>Ne JAMAIS modifier son contenu</b> — ce mesh
     * est conceptuellement immuable.
     *
     * @return référence directe au tableau interne (lecture seule)
     */
    public float[] getVertices() {
        return vertices;
    }

    public int[] getIndices() {
        return indices;
    }

    public int getVertexCount() { return vertices.length / 3; }
    public int getTriangleCount() { return indices.length / 3; }

    public boolean isEmpty(){
        return indices.length == 0;
    }
}
