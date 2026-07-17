package com.stellargenesis.core.world.meshing;

import com.stellargenesis.core.math.Vec3;

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

        float[] normalsArray = computeNormals(vertsArray, indsArray);

        return new ChunkMesh(vertsArray, indsArray, normalsArray);
    }

    private float[] computeNormals(float[] verts, int[] inds) {
        float[] normals = new float[verts.length]; // init à 0 automatiquement

        // ÉTAPE 1 : accumuler les normales de face
        for (int i = 0; i < inds.length; i += 3) {
            int i0 = inds[i];
            int i1 = inds[i + 1];
            int i2 = inds[i + 2];

            Vec3 P0 = new Vec3(verts[i0*3], verts[i0*3+1], verts[i0*3+2]);
            Vec3 P1 = new Vec3(verts[i1*3], verts[i1*3+1], verts[i1*3+2]);
            Vec3 P2 = new Vec3(verts[i2*3], verts[i2*3+1], verts[i2*3+2]);

            Vec3 edge1 = P1.sub(P0);
            Vec3 edge2 = P2.sub(P0);
            Vec3 faceNormal = edge1.cross(edge2); // PAS normalisée

            accumulate(normals, i0, faceNormal);
            accumulate(normals, i1, faceNormal);
            accumulate(normals, i2, faceNormal);
        }

        // ÉTAPE 2 : normaliser
        for (int v = 0; v < normals.length; v += 3) {
            float nx = normals[v], ny = normals[v+1], nz = normals[v+2];
            float len = (float) Math.sqrt(nx*nx + ny*ny + nz*nz);
            if (len > 1e-6f) {
                normals[v]   = nx / len;
                normals[v+1] = ny / len;
                normals[v+2] = nz / len;
            } else {
                normals[v] = 0f; normals[v+1] = 1f; normals[v+2] = 0f;
            }
        }

        return normals;
    }

    // petite méthode utilitaire pour éviter la répétition
    private void accumulate(float[] normals, int vertexIndex, Vec3 n) {
        normals[vertexIndex*3]   += (float) n.x;
        normals[vertexIndex*3+1] += (float) n.y;
        normals[vertexIndex*3+2] += (float) n.z;
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