package com.stellargenesis.client.render;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.stellargenesis.core.math.Vec3;
import com.stellargenesis.core.world.Chunk;
import com.stellargenesis.core.world.ChunkPos;
import com.stellargenesis.core.world.biome.BiomeRules;
import com.stellargenesis.core.world.biome.ColorPalette;
import com.stellargenesis.core.world.meshing.ChunkMesh;

/**
 * Convertit un ChunkMesh (data brute du module core) en Mesh jMonkeyEngineq
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
    public static Mesh toJmeMesh(ChunkMesh chunkMesh, ChunkPos pos,
                                 float baseHeight, float amplitude,
                                 BiomeRules rules, ColorPalette palette) {
        float[] vertices = chunkMesh.getVertices();
        int[] indices = chunkMesh.getIndices();
        int vertexCount = vertices.length / 3;

        float[] normals = computeSmoothNormals(vertices, indices, vertexCount);
        float[] colors  = computeColors(vertices, normals, pos,
                baseHeight, amplitude, rules, palette);

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Normal,   3, BufferUtils.createFloatBuffer(normals));
        mesh.setBuffer(VertexBuffer.Type.Color,    4, BufferUtils.createFloatBuffer(colors)); // ← NOUVEAU
        mesh.setBuffer(VertexBuffer.Type.Index,    3, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();
        return mesh;
    }

    private static float[] computeColors(float[] vertices, float[] normals, ChunkPos pos,
                                         float baseHeight, float amplitude,
                                         BiomeRules rules, ColorPalette palette) {
        int vertexCount = vertices.length / 3;
        float[] colors = new float[vertexCount * 4]; // RGBA

        for (int v = 0; v < vertexCount; v++) {
            int p = v * 3;

            // Reconversion local → monde sur Y
            float worldY = vertices[p + 1] + pos.y * Chunk.SIZE;

            Vec3 normal = new Vec3(normals[p], normals[p + 1], normals[p + 2]);
            Vec3 color  = rules.colorForFlat(worldY, normal, baseHeight, amplitude, palette);

            int c = v * 4;
            colors[c]     = (float) color.x;
            colors[c + 1] = (float) color.y;
            colors[c + 2] = (float) color.z;
            colors[c + 3] = 1.0f;
        }
        return colors;
    }


    private static float[] computeSmoothNormals(float[] vertices, int[] indices, int vertexCount) {
        float[] normals = new float[vertexCount * 3];

        // ═══════════════════════════════════════════
        // PASSE 1 : Accumuler les normales des triangles
        // ═══════════════════════════════════════════
        for (int i = 0; i < indices.length; i += 3) {
            int iA = indices[i];
            int iB = indices[i + 1];
            int iC = indices[i + 2];

            // Positions des 3 sommets du triangle
            float ax = vertices[iA * 3],     ay = vertices[iA * 3 + 1], az = vertices[iA * 3 + 2];
            float bx = vertices[iB * 3],     by = vertices[iB * 3 + 1], bz = vertices[iB * 3 + 2];
            float cx = vertices[iC * 3],     cy = vertices[iC * 3 + 1], cz = vertices[iC * 3 + 2];

            // Deux arêtes du triangle
            float e1x = bx - ax, e1y = by - ay, e1z = bz - az;
            float e2x = cx - ax, e2y = cy - ay, e2z = cz - az;

            // Produit vectoriel e1 × e2 = normale (non normalisée)
            // Sa longueur est proportionnelle à l'aire → pondération automatique
            float nx = e1y * e2z - e1z * e2y;
            float ny = e1z * e2x - e1x * e2z;
            float nz = e1x * e2y - e1y * e2x;

            // Ajouter aux 3 sommets
            normals[iA * 3]     += nx;  normals[iA * 3 + 1] += ny;  normals[iA * 3 + 2] += nz;
            normals[iB * 3]     += nx;  normals[iB * 3 + 1] += ny;  normals[iB * 3 + 2] += nz;
            normals[iC * 3]     += nx;  normals[iC * 3 + 1] += ny;  normals[iC * 3 + 2] += nz;
        }

        // ═══════════════════════════════════════════
        // PASSE 2 : Normalisation
        // ═══════════════════════════════════════════
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
                // Sommet dégénéré (jamais référencé)
                normals[v * 3]     = 0;
                normals[v * 3 + 1] = 1;  // fallback vers le haut
                normals[v * 3 + 2] = 0;
            }
        }

        return normals;
    }

}
