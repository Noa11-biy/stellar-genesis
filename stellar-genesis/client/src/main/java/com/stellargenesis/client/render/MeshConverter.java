package com.stellargenesis.client.render;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.stellargenesis.core.world.meshing.ChunkMesh;

/**
 * Convertit un ChunkMesh (data brute du module core) en Mesh jMonkeyEngine
 * prêt à être affiché sur le GPU.
 *
 * Calcule les normales lisses (smooth shading) en deux passes :
 *  1. Pour chaque triangle, ajouter sa normale (non-normalisée) à ses 3 sommets
 *  2. Normaliser toutes les normales de sommets à la fin
 */
public class MeshConverter {

    /**
     * Convertit un ChunkMesh en Mesh jME.
     *
     * @param chunkMesh le mesh source (issu de ChunkMesher)
     * @return un Mesh jME prêt à être attaché à une Geometry
     */
    public static Mesh toJmeMesh(ChunkMesh chunkMesh) {
        float[] vertices = chunkMesh.getVertices();
        int[] indices = chunkMesh.getIndices();

        // Tableau des normales : un Vector3f par sommet
        int vertexCount = vertices.length / 3;
        float[] normals = computeSmoothNormals(vertices, indices, vertexCount);

        // Construction du Mesh jME
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Normal,   3, BufferUtils.createFloatBuffer(normals));
        mesh.setBuffer(VertexBuffer.Type.Index,    3, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();  // calcule la bounding box pour le frustum culling

        return mesh;
    }

    /**
     * Calcule les normales lisses par sommet.
     *
     * Algorithme :
     *  - Passe 1 : pour chaque triangle, calculer (B-A) × (C-A) et ajouter
     *              ce vecteur (non normalisé) aux 3 sommets du triangle.
     *  - Passe 2 : normaliser chaque normale de sommet.
     *
     * On NE normalise PAS la normale du triangle avant de l'ajouter, pour que
     * les grands triangles contribuent davantage que les petits (pondération
     * naturelle par l'aire).
     */
    private static float[] computeSmoothNormals(float[] vertices, int[] indices, int vertexCount) {
        float[] normals = new float[vertexCount * 3];

        // PASSE 1 : accumulation
        for (int t = 0; t < indices.length; t += 3) {
            int iA = indices[t];
            int iB = indices[t + 1];
            int iC = indices[t + 2];

            // Récupérer les 3 sommets du triangle
            float ax = vertices[iA * 3],     ay = vertices[iA * 3 + 1], az = vertices[iA * 3 + 2];
            float bx = vertices[iB * 3],     by = vertices[iB * 3 + 1], bz = vertices[iB * 3 + 2];
            float cx = vertices[iC * 3],     cy = vertices[iC * 3 + 1], cz = vertices[iC * 3 + 2];

            // Vecteurs B-A et C-A
            float abx = bx - ax, aby = by - ay, abz = bz - az;
            float acx = cx - ax, acy = cy - ay, acz = cz - az;

            // Produit vectoriel (B-A) × (C-A) → normale du triangle (non normalisée)
            float nx = aby * acz - abz * acy;
            float ny = abz * acx - abx * acz;
            float nz = abx * acy - aby * acx;

            // Ajouter aux 3 sommets
            normals[iA * 3]     += nx;  normals[iA * 3 + 1] += ny;  normals[iA * 3 + 2] += nz;
            normals[iB * 3]     += nx;  normals[iB * 3 + 1] += ny;  normals[iB * 3 + 2] += nz;
            normals[iC * 3]     += nx;  normals[iC * 3 + 1] += ny;  normals[iC * 3 + 2] += nz;
        }

        // PASSE 2 : normalisation
        for (int v = 0; v < vertexCount; v++) {
            float nx = normals[v * 3];
            float ny = normals[v * 3 + 1];
            float nz = normals[v * 3 + 2];

            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 1e-6f) {
                normals[v * 3]     = nx / len;
                normals[v * 3 + 1] = ny / len;
                normals[v * 3 + 2] = nz / len;
            } else {
                // Sommet dégénéré (jamais référencé ou triangles invalides)
                normals[v * 3]     = 0;
                normals[v * 3 + 1] = 1;  // fallback vers le haut
                normals[v * 3 + 2] = 0;
            }
        }

        return normals;
    }
}
