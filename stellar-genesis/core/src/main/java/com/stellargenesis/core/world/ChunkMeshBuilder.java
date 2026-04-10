package com.stellargenesis.core.world;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Transforme un Chunk en Mesh 3D pour jMonkeyEngine.
 *
 * Principe : pour chaque bloc non-air, on vérifie les 6 voisins.
 * Si le voisin est air (ou hors du chunk), on génère la face correspondante.
 *
 * C'est du "culled meshing" — on ne dessine que les faces visibles.
 */
public class ChunkMeshBuilder {

    // Les 6 directions : +X, -X, +Y, -Y, +Z, -Z
    private static final int[][] DIRS = {
            { 1,  0,  0}, // droite
            {-1,  0,  0}, // gauche
            { 0,  1,  0}, // haut
            { 0, -1,  0}, // bas
            { 0,  0,  1}, // devant
            { 0,  0, -1}  // derrière
    };

    // Les 4 sommets de chaque face selon la direction
    // Chaque face est un quad (2 triangles = 6 indices)
    private static final float[][][] FACE_VERTICES = {
            // +X
            {{1,0,0}, {1,1,0}, {1,1,1}, {1,0,1}},
            // -X
            {{0,0,1}, {0,1,1}, {0,1,0}, {0,0,0}},
            // +Y
            {{0,1,0}, {0,1,1}, {1,1,1}, {1,1,0}},
            // -Y
            {{0,0,1}, {0,0,0}, {1,0,0}, {1,0,1}},
            // +Z
            {{1,0,1}, {1,1,1}, {0,1,1}, {0,0,1}},
            // -Z
            {{0,0,0}, {0,1,0}, {1,1,0}, {1,0,0}}
    };

    // Normales pour chaque direction
    private static final float[][] NORMALS = {
            { 1, 0, 0}, {-1, 0, 0},
            { 0, 1, 0}, { 0,-1, 0},
            { 0, 0, 1}, { 0, 0,-1}
    };

    // UVs pour un quad standard
    private static final float[][] QUAD_UVS = {
            {0, 0}, {1, 0}, {1, 1}, {0, 1}
    };

    /**
     * Construit le mesh 3D d'un chunk.
     *
     * @param chunk        Le chunk à mailler
     * @param chunkManager Le manager pour vérifier les blocs des chunks voisins
     * @return Mesh jME prêt à être attaché à un Geometry, ou null si chunk vide
     */
    public static Mesh buildMesh(Chunk chunk, ChunkManager chunkManager) {
        List<Float> positions = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> uvs = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        int vertexCount = 0;

        int cx = chunk.getPosition().x;
        int cy = chunk.getPosition().y;
        int cz = chunk.getPosition().z;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    short blockId = chunk.getBlock(x, y, z);

                    // Air = 0, on skip
                    if (blockId == 0) continue;

                    // Pour chaque direction, vérifier si le voisin est air
                    for (int d = 0; d < 6; d++) {
                        int nx = x + DIRS[d][0];
                        int ny = y + DIRS[d][1];
                        int nz = z + DIRS[d][2];

                        if (isTransparent(chunk, chunkManager, cx, cy, cz, nx, ny, nz)) {
                            // Ajouter la face
                            addFace(positions, normals, uvs, indices,
                                    x, y, z, d, blockId, vertexCount);
                            vertexCount += 4; // 4 sommets par face
                        }
                    }
                }
            }
        }

        // Chunk entièrement invisible
        if (vertexCount == 0) return null;

        return createMesh(positions, normals, uvs, indices);
    }

    /**
     * Vérifie si un bloc à la position locale (nx, ny, nz) est transparent.
     * Si la position sort du chunk, on vérifie dans le chunk voisin.
     */
    private static boolean isTransparent(Chunk chunk, ChunkManager chunkManager,
                                         int cx, int cy, int cz,
                                         int nx, int ny, int nz) {
        // Si dans les limites du chunk courant
        if (nx >= 0 && nx < Chunk.SIZE &&
                ny >= 0 && ny < Chunk.SIZE &&
                nz >= 0 && nz < Chunk.SIZE) {
            return chunk.getBlock(nx, ny, nz) == 0;
        }

        // Sinon, on vérifie dans le chunk voisin
        if (chunkManager == null) return true; // pas de voisin = face visible

        // Calculer la position du chunk voisin et la coordonnée locale
        int neighborCX = cx + (nx < 0 ? -1 : (nx >= Chunk.SIZE ? 1 : 0));
        int neighborCY = cy + (ny < 0 ? -1 : (ny >= Chunk.SIZE ? 1 : 0));
        int neighborCZ = cz + (nz < 0 ? -1 : (nz >= Chunk.SIZE ? 1 : 0));

        int localX = ((nx % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;
        int localY = ((ny % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;
        int localZ = ((nz % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;

        ChunkPos neighborPos = new ChunkPos(neighborCX, neighborCY, neighborCZ);
        Chunk neighbor = chunkManager.getChunk(neighborPos);

        if (neighbor == null) return true; // chunk pas chargé = face visible

        return neighbor.getBlock(localX, localY, localZ) == 0;
    }

    /**
     * Ajoute les 4 sommets et 6 indices d'une face de bloc.
     */
    private static void addFace(List<Float> positions, List<Float> normals,
                                List<Float> uvs, List<Integer> indices,
                                int x, int y, int z, int direction,
                                short blockId, int vertexOffset) {
        float[][] faceVerts = FACE_VERTICES[direction];
        float[] normal = NORMALS[direction];

        // 4 sommets du quad
        for (int i = 0; i < 4; i++) {
            positions.add(x + faceVerts[i][0]);
            positions.add(y + faceVerts[i][1]);
            positions.add(z + faceVerts[i][2]);

            normals.add(normal[0]);
            normals.add(normal[1]);
            normals.add(normal[2]);

            uvs.add(QUAD_UVS[i][0]);
            uvs.add(QUAD_UVS[i][1]);
        }

        // 2 triangles pour le quad (sens anti-horaire pour le culling)
        indices.add(vertexOffset);
        indices.add(vertexOffset + 1);
        indices.add(vertexOffset + 2);

        indices.add(vertexOffset);
        indices.add(vertexOffset + 2);
        indices.add(vertexOffset + 3);
    }

    /**
     * Crée le Mesh jME à partir des listes de données.
     */
    private static Mesh createMesh(List<Float> positions, List<Float> normals,
                                   List<Float> uvs, List<Integer> indices) {
        Mesh mesh = new Mesh();

        // Convertir les listes en tableaux
        float[] posArray = toFloatArray(positions);
        float[] normArray = toFloatArray(normals);
        float[] uvArray = toFloatArray(uvs);
        int[] idxArray = indices.stream().mapToInt(Integer::intValue).toArray();

        // Remplir les buffers du mesh
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(posArray));
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normArray));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uvArray));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(idxArray));

        mesh.updateBound();
        return mesh;
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
