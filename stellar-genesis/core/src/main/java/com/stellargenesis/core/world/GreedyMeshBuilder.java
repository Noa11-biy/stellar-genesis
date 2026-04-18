package com.stellargenesis.core.world;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * GreedyMeshBuilder — Version optimisée de ChunkMeshBuilder.
 *
 * PRINCIPE DU GREEDY MESHING :
 * Au lieu de générer 1 quad par face de bloc, on fusionne
 * les faces adjacentes identiques en un seul grand quad.
 *
 * EXEMPLE :
 *   Mur de pierre 16×16 :
 *     - Naïf (culled)  : 256 quads, 512 triangles
 *     - Greedy         : 1 quad,    2 triangles
 *     - Gain           : ×256 moins de triangles
 *
 * ALGORITHME :
 *   Pour chaque axe (X=0, Y=1, Z=2) :
 *     Pour chaque direction (+1 ou -1) :
 *       Pour chaque tranche perpendiculaire à l'axe :
 *         1. Construire un masque 2D des faces visibles
 *         2. Parcourir le masque, fusionner les rectangles identiques
 *         3. Générer un quad par rectangle fusionné
 *
 * POURQUOI AXE PAR AXE ?
 *   On ne peut fusionner que des faces coplanaires (dans le même plan).
 *   Les faces +Y de Y=5 sont coplanaires entre elles.
 *   Une face +Y et une face +X ne peuvent pas fusionner → plans différents.
 *
 * COMPLEXITÉ :
 *   O(n²) par tranche (n=16), 6 directions, 16 tranches = O(16³) = O(n³)
 *   Identique au culled meshing mais génère BEAUCOUP moins de géométrie.
 *
 * @author Noa Moal
 */
public class GreedyMeshBuilder {

    /**
     * Point d'entrée — même signature que ChunkMeshBuilder.buildMesh().
     * Remplacement direct, aucun autre fichier à modifier.
     */
    public static Mesh buildMesh(Chunk chunk, ChunkManager chunkManager) {
        List<Float> positions = new ArrayList<>();
        List<Float> normals   = new ArrayList<>();
        List<Float> uvs       = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int vertexCount = 0;

        int cx = chunk.getPosition().x;
        int cy = chunk.getPosition().y;
        int cz = chunk.getPosition().z;

        /*
         * On itère sur les 3 axes et les 2 directions.
         *
         * axis  : 0=X, 1=Y, 2=Z  (l'axe PERPENDICULAIRE à la face)
         * side  : 0 = face positive (+X, +Y, +Z)
         *         1 = face négative (-X, -Y, -Z)
         */
        for (int axis = 0; axis < 3; axis++) {
            for (int side = 0; side < 2; side++) {

                // Direction normale à la face (+1 ou -1)
                int dir = (side == 0) ? 1 : -1;

                // Pour chaque tranche le long de l'axe
                for (int slice = 0; slice < Chunk.SIZE; slice++) {

                    // === ÉTAPE 1 : Construire le masque 2D ===
                    short[][] mask = buildMask(chunk, chunkManager, cx, cy, cz,
                            axis, dir, slice);

                    // === ÉTAPE 2 : Greedy loop sur le masque ===
                    vertexCount = greedyLoop(mask, positions, normals, uvs, indices,
                            axis, dir, slice, vertexCount);
                }
            }
        }

        if (vertexCount == 0) return null;
        return createMesh(positions, normals, uvs, indices);
    }

    // =========================================================
    //  ÉTAPE 1 — Construction du masque 2D
    // =========================================================

    /**
     * Construit un masque 2D [SIZE][SIZE] pour une tranche donnée.
     *
     * masque[u][v] = blockId  si la face dans la direction (axis, dir)
     *                         du bloc à cette position est visible
     *             = 0         sinon (bloc air, ou voisin non transparent)
     *
     * SYSTÈME DE COORDONNÉES :
     *   axis=1 (Y), dir=+1, slice=5 (tranche Y=5)
     *   → u correspond à X, v correspond à Z
     *   → on regarde la face +Y des blocs à Y=5
     *   → la face est visible si le bloc Y=6 est transparent
     *
     * Les axes u et v sont les deux axes perpendiculaires à 'axis' :
     *   axis=0 (X) → u=Y, v=Z
     *   axis=1 (Y) → u=X, v=Z
     *   axis=2 (Z) → u=X, v=Y
     */
    private static short[][] buildMask(Chunk chunk, ChunkManager chunkManager,
                                       int cx, int cy, int cz,
                                       int axis, int dir, int slice) {
        short[][] mask = new short[Chunk.SIZE][Chunk.SIZE];

        // Les deux axes perpendiculaires à 'axis'
        int uAxis = (axis == 0) ? 1 : 0;  // X si axis!=0, sinon Y
        int vAxis = (axis == 2) ? 1 : 2;  // Y si axis==2, sinon Z

        for (int u = 0; u < Chunk.SIZE; u++) {
            for (int v = 0; v < Chunk.SIZE; v++) {

                // Coordonnées locales du bloc courant
                int[] pos = new int[3];
                pos[axis] = slice;
                pos[uAxis] = u;
                pos[vAxis] = v;

                int x = pos[0], y = pos[1], z = pos[2];
                short blockId = chunk.getBlock(x, y, z);

                // Air → pas de face
                if (blockId == 0) {
                    mask[u][v] = 0;
                    continue;
                }

                // Coordonnées du voisin dans la direction de la face
                int[] neighborPos = new int[3];
                neighborPos[axis] = slice + dir;
                neighborPos[uAxis] = u;
                neighborPos[vAxis] = v;

                int nx = neighborPos[0], ny = neighborPos[1], nz = neighborPos[2];

                // La face est visible si le voisin est transparent
                if (isTransparent(chunk, chunkManager, cx, cy, cz, nx, ny, nz)) {
                    mask[u][v] = blockId;
                } else {
                    mask[u][v] = 0;
                }
            }
        }

        return mask;
    }

    // =========================================================
    //  ÉTAPE 2 — Greedy loop
    // =========================================================

    /**
     * Parcourt le masque 2D et fusionne les rectangles identiques.
     *
     * Pour chaque cellule non nulle non traitée :
     *   1. Étendre en U tant que même blockId
     *   2. Étendre en V tant que toute la largeur W a le même blockId
     *   3. Générer 1 quad pour ce rectangle W×H
     *   4. Effacer le rectangle du masque (éviter double traitement)
     *
     * POURQUOI EFFACER LE MASQUE ?
     *   Sans ça, on retraiterait les mêmes cellules plusieurs fois.
     *   En mettant mask[u][v]=0 après traitement, on marque "déjà fait".
     */
    private static int greedyLoop(short[][] mask,
                                  List<Float> positions, List<Float> normals,
                                  List<Float> uvs, List<Integer> indices,
                                  int axis, int dir, int slice,
                                  int vertexCount) {
        int uAxis = (axis == 0) ? 1 : 0;
        int vAxis = (axis == 2) ? 1 : 2;

        for (int v = 0; v < Chunk.SIZE; v++) {
            for (int u = 0; u < Chunk.SIZE; ) {

                short blockId = mask[u][v];

                // Cellule vide ou déjà traitée → avancer
                if (blockId == 0) {
                    u++;
                    continue;
                }

                // --- Étendre en U (largeur W) ---
                int w = 1;
                while (u + w < Chunk.SIZE && mask[u + w][v] == blockId) {
                    w++;
                }

                // --- Étendre en V (hauteur H) ---
                int h = 1;
                outer:
                while (v + h < Chunk.SIZE) {
                    // Vérifier que TOUTE la ligne [u, u+w[ à v+h est identique
                    for (int k = u; k < u + w; k++) {
                        if (mask[k][v + h] != blockId) break outer;
                    }
                    h++;
                }

                // --- Générer le quad W×H ---
                vertexCount = addGreedyQuad(
                        positions, normals, uvs, indices,
                        axis, uAxis, vAxis, dir, slice,
                        u, v, w, h,
                        blockId, vertexCount
                );

                // --- Effacer le rectangle du masque ---
                for (int dv = 0; dv < h; dv++) {
                    for (int du = 0; du < w; du++) {
                        mask[u + du][v + dv] = 0;
                    }
                }

                u += w; // Sauter les colonnes traitées
            }
        }

        return vertexCount;
    }

    // =========================================================
    //  ÉTAPE 3 — Génération du quad
    // =========================================================

    /**
     * Génère les 4 sommets et 6 indices d'un quad W×H.
     *
     * On reconstruit les coordonnées 3D à partir de :
     *   - axis  : axe perpendiculaire à la face
     *   - uAxis : premier axe dans le plan de la face
     *   - vAxis : second axe dans le plan de la face
     *   - slice : position sur l'axe perpendiculaire
     *   - u, v  : coin bas-gauche du quad dans le masque
     *   - w, h  : largeur et hauteur du quad
     *
     * DÉCALAGE DE SLICE :
     *   Face +Y à slice=5 → la face est AU DESSUS du bloc → Y=6
     *   Face -Y à slice=5 → la face est EN DESSOUS → Y=5
     *   dir=+1 → sliceOffset = slice + 1
     *   dir=-1 → sliceOffset = slice
     */
    private static int addGreedyQuad(List<Float> positions, List<Float> normals,
                                     List<Float> uvs, List<Integer> indices,
                                     int axis, int uAxis, int vAxis,
                                     int dir, int slice,
                                     int u, int v, int w, int h,
                                     short blockId, int vertexCount) {

        // Position de la face sur l'axe perpendiculaire
        int sliceOffset = (dir > 0) ? slice + 1 : slice;

        // Normale
        float[] normal = new float[3];
        normal[axis] = dir;

        /*
         * Les 4 coins du quad :
         *   (u,   v  ) → coin bas-gauche
         *   (u+w, v  ) → coin bas-droit
         *   (u+w, v+h) → coin haut-droit
         *   (u,   v+h) → coin haut-gauche
         *
         * On reconstruit les coordonnées 3D pour chaque coin.
         */
        int[][] corners = {
                {u,     v    },
                {u + w, v    },
                {u + w, v + h},
                {u,     v + h}
        };

        for (int[] corner : corners) {
            float[] p = new float[3];
            p[axis]  = sliceOffset;
            p[uAxis] = corner[0];
            p[vAxis] = corner[1];

            positions.add(p[0]);
            positions.add(p[1]);
            positions.add(p[2]);

            normals.add(normal[0]);
            normals.add(normal[1]);
            normals.add(normal[2]);
        }

        // UVs proportionnelles à la taille du quad (texture étirée)
        uvs.add(0f); uvs.add(0f);
        uvs.add((float) w); uvs.add(0f);  // ← étirée sur W blocs
        uvs.add((float) w); uvs.add((float) h);
        uvs.add(0f); uvs.add((float) h);  // ← étirée sur H blocs

        /*
         * Ordre des indices selon la direction de la face.
         * Inversion pour les faces négatives → normal facing correct.
         * (backface culling OpenGL : sens anti-horaire = face visible)
         */
        boolean flipWinding = (dir < 0) ^ (axis == 1);

        if (!flipWinding) {
            indices.add(vertexCount);
            indices.add(vertexCount + 1);
            indices.add(vertexCount + 2);
            indices.add(vertexCount);
            indices.add(vertexCount + 2);
            indices.add(vertexCount + 3);
        } else {
            indices.add(vertexCount);
            indices.add(vertexCount + 2);
            indices.add(vertexCount + 1);
            indices.add(vertexCount);
            indices.add(vertexCount + 3);
            indices.add(vertexCount + 2);
        }

        return vertexCount + 4;
    }

    // =========================================================
    //  UTILITAIRES (repris de ChunkMeshBuilder)
    // =========================================================

    /** Même logique que dans ChunkMeshBuilder — voisin transparent ? */
    private static boolean isTransparent(Chunk chunk, ChunkManager chunkManager,
                                         int cx, int cy, int cz,
                                         int nx, int ny, int nz) {
        if (nx >= 0 && nx < Chunk.SIZE &&
                ny >= 0 && ny < Chunk.SIZE &&
                nz >= 0 && nz < Chunk.SIZE) {
            return chunk.getBlock(nx, ny, nz) == 0;
        }

        if (chunkManager == null) return true;

        int neighborCX = cx + (nx < 0 ? -1 : (nx >= Chunk.SIZE ? 1 : 0));
        int neighborCY = cy + (ny < 0 ? -1 : (ny >= Chunk.SIZE ? 1 : 0));
        int neighborCZ = cz + (nz < 0 ? -1 : (nz >= Chunk.SIZE ? 1 : 0));

        int localX = ((nx % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;
        int localY = ((ny % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;
        int localZ = ((nz % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;

        Chunk neighbor = chunkManager.getChunk(new ChunkPos(neighborCX, neighborCY, neighborCZ));
        if (neighbor == null) return true;

        return neighbor.getBlock(localX, localY, localZ) == 0;
    }

    private static Mesh createMesh(List<Float> positions, List<Float> normals,
                                   List<Float> uvs, List<Integer> indices) {
        Mesh mesh = new Mesh();

        float[] posArray  = toFloatArray(positions);
        float[] normArray = toFloatArray(normals);
        float[] uvArray   = toFloatArray(uvs);
        int[]   idxArray  = indices.stream().mapToInt(Integer::intValue).toArray();

        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(posArray));
        mesh.setBuffer(VertexBuffer.Type.Normal,   3, BufferUtils.createFloatBuffer(normArray));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uvArray));
        mesh.setBuffer(VertexBuffer.Type.Index,    3, BufferUtils.createIntBuffer(idxArray));

        mesh.updateBound();
        return mesh;
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }
}
