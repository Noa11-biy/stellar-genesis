package com.stellargenesis.core.world.meshing;

import com.stellargenesis.core.math.Vec3;
import com.stellargenesis.core.world.density.DensityField;
import com.stellargenesis.core.world.marching.MarchingCubesTables;

public class ChunkMesher {

    private static final float ISO_LEVEL = 0.0f;

    public static ChunkMesh mesh(DensityField field) {
        MeshBuilder builder = new MeshBuilder();
        int size = field.getSize();

        for (int z = 0; z < size - 1; z++) {
            for (int y = 0; y < size - 1; y++) {
                for (int x = 0; x < size - 1; x++) {
                    processCube(field, x, y, z, builder);
                }
            }
        }

        return builder.build();
    }

    private static void processCube(DensityField field, int x, int y, int z, MeshBuilder builder) {

        // ============================================================
        // ÉTAPE 1 : Récupérer les 8 densités aux sommets du cube
        // Convention de Bourke (très important : ne pas inverser !)
        // ============================================================
        float[] d = new float[8];
        d[0] = field.get(x,     y,     z    );
        d[1] = field.get(x + 1, y,     z    );
        d[2] = field.get(x + 1, y,     z + 1);
        d[3] = field.get(x,     y,     z + 1);
        d[4] = field.get(x,     y + 1, z    );
        d[5] = field.get(x + 1, y + 1, z    );
        d[6] = field.get(x + 1, y + 1, z + 1);
        d[7] = field.get(x,     y + 1, z + 1);

        // ============================================================
        // ÉTAPE 2 : Construire cubeIndex (8 bits)
        // ============================================================
        int cubeIndex = 0;
        if (d[0] < ISO_LEVEL) cubeIndex |= 1;
        if (d[1] < ISO_LEVEL) cubeIndex |= 2;
        if (d[2] < ISO_LEVEL) cubeIndex |= 4;
        if (d[3] < ISO_LEVEL) cubeIndex |= 8;
        if (d[4] < ISO_LEVEL) cubeIndex |= 16;
        if (d[5] < ISO_LEVEL) cubeIndex |= 32;
        if (d[6] < ISO_LEVEL) cubeIndex |= 64;
        if (d[7] < ISO_LEVEL) cubeIndex |= 128;


        // ============================================================
        // ÉTAPE 3 : Early exit — pas de surface dans ce cube
        // ============================================================
        if (MarchingCubesTables.EDGE_TABLE[cubeIndex] == 0) {
            return;
        }


        // ============================================================
        // ÉTAPE 4 : Calculer les positions interpolées sur les 12 arêtes
        // ============================================================
        Vec3[] edgeVertices = new Vec3[12];
        int edgeFlags = MarchingCubesTables.EDGE_TABLE[cubeIndex];

        for (int i = 0; i < 12; i++) {
            // si le bit est activé, l'arrête i est coupée
            if ((edgeFlags & (1 << i)) != 0){
                // Récupérer les indices des 2 sommets de l'arête
                int v_a = MarchingCubesTables.EDGE_VERTICES[i][0];
                int v_b = MarchingCubesTables.EDGE_VERTICES[i][1];

                // Récupérer les positions 3D absolues des 2 sommets
                // (offset relatif + position du cube dans le chunk)
                float ax = x + MarchingCubesTables.VERTEX_OFFSETS[v_a][0];
                float ay = y + MarchingCubesTables.VERTEX_OFFSETS[v_a][1];
                float az = z + MarchingCubesTables.VERTEX_OFFSETS[v_a][2];

                float bx = x + MarchingCubesTables.VERTEX_OFFSETS[v_b][0];
                float by = y + MarchingCubesTables.VERTEX_OFFSETS[v_b][1];
                float bz = z + MarchingCubesTables.VERTEX_OFFSETS[v_b][2];

                // Densité aux 2 sommets
                float d_a = d[v_a];
                float d_b = d[v_b];

                // Interpolation Linéaire : trouver t telle que d_a + t*(d_b - d_a) = ISO_LEVEL
                // t = (ISO_LEVEL - d_a) / (d_b - d_a)
                float t;
                if (Math.abs(d_b - d_a) < 1e-6f){
                    t = 0.5f; // évite la division par 0
                } else {
                    t = (ISO_LEVEL - d_a) / (d_b - d_a);
                }

                // Position Interpolée
                double px = ax + t * (bx - ax);
                double py = ay + t * (by - ay);
                double pz = az + t * (bz - az);

                edgeVertices[i] = new Vec3(px, py, pz);
            }
        }

        // ============================================================
        // ÉTAPE 5 : Lire triTable et créer les triangles
        // ============================================================
        int[] triangles = MarchingCubesTables.TRI_TABLE[cubeIndex];

        for (int i = 0; triangles[i] != -1; i += 3) {
            Vec3 p0 = edgeVertices[triangles[i]];
            Vec3 p1 = edgeVertices[triangles[i + 1]];
            Vec3 p2 = edgeVertices[triangles[i + 2]];

            int idx0 = builder.addVertex((float) p0.x, (float) p0.y, (float) p0.z);
            int idx1 = builder.addVertex((float) p1.x, (float) p1.y, (float) p1.z);
            int idx2 = builder.addVertex((float) p2.x, (float) p2.y, (float) p2.z);

            builder.addTriangle(idx0, idx1, idx2);
        }
    }

}
