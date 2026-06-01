package com.stellargenesis.core.world.meshing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class MeshBuilder {

    // Buffer de positions : [x0,y0,z0, x1,y1,z1...]
    private final ArrayList<Float> vertices = new ArrayList<>();

    // Buffer d'indices de triangles
    private final ArrayList<Integer> indices = new ArrayList<>();

    // Cache de déduplication : VertexKey -> index dans vertices
    private final HashMap<VertexKey, Integer> vertexCache = new HashMap<>();

    /**
     * Ajoute un sommet et retourne son index.
     * Si le sommet existe déjà (même position quantifiée), retourne l'index existant.
     */
    public int addVertex(float x, float y, float z){
        VertexKey vk = new VertexKey(x, y, z);
        Integer existingIndex = vertexCache.get(vk);

        // Sommet existe déjà
        if(existingIndex != null){
            return existingIndex;
        }

        // Nouveau sommet
        int newIndex = vertices.size() / 3;
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertexCache.put(vk, newIndex);

        return newIndex;
    }

    public void addTriangle(int i0, int i1, int i2){
        indices.add(i0);
        indices.add(i1);
        indices.add(i2);
    }

    /**
     * Finalise et retourne le ChunkMesh prêt pour le rendu.
     */
    public ChunkMesh build() {
        float[] vertsArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertsArray[i] = vertices.get(i);
        }

        int[] indsArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indsArray[i] = indices.get(i);
        }

        return new ChunkMesh(vertsArray, indsArray);
    }

}

class VertexKey {
    private final int qx;
    private final int qy;
    private final int qz;

    private static final float PRECISION = 1000f;

    public VertexKey(float x, float y, float z) {
        this.qx = Math.round(x * PRECISION);
        this.qy = Math.round(y * PRECISION);
        this.qz = Math.round(z * PRECISION);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VertexKey)) return false;
        VertexKey other = (VertexKey) o;
        return this.qx == other.qx
                && this.qy == other.qy
                && this.qz == other.qz;
    }

    @Override
    public int hashCode() {
        return Objects.hash(qx, qy, qz);
    }
}