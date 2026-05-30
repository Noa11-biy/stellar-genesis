package com.stellargenesis.core.world.meshing;

import com.stellargenesis.core.math.Vec3;
import com.stellargenesis.core.world.marching.MarchingCubesTables;

import java.util.ArrayList;
import java.util.List;

public class MarchingCube {

    private static final double EPSILON = 1e-6;

    public static List<Vec3> polygonise(
            Vec3[] corners,
            float[] densities,
            float isoLevel
    ) {
        // Étape 1 : calculer cubeIndex
        int cubeIndex = 0;
        for (int i = 0; i < 8; i++) {
            if (densities[i] < isoLevel) cubeIndex |= (1 << i);
        }

        // Étape 2 : skip si pas de surface
        int edgeFlags = MarchingCubesTables.EDGE_TABLE[cubeIndex];
        if (edgeFlags == 0) return new ArrayList<>();

        // Étape 3 : calculer les points d'intersection sur les arêtes coupées
        Vec3[] vertList = new Vec3[12];
        for (int i = 0; i < 12; i++) {
            if ((edgeFlags & (1 << i)) != 0) {
                int[] edge = MarchingCubesTables.EDGE_VERTICES[i];
                int a = edge[0];
                int b = edge[1];
                vertList[i] = interpolate(
                        isoLevel,
                        corners[a], corners[b],
                        densities[a], densities[b]
                );
            }
        }

        // Étape 4 : générer les triangles
        List<Vec3> triangles = new ArrayList<>();
        int[] triList = MarchingCubesTables.TRI_TABLE[cubeIndex];
        for (int i = 0; triList[i] != -1; i += 3) {
            triangles.add(vertList[triList[i]]);
            triangles.add(vertList[triList[i + 1]]);
            triangles.add(vertList[triList[i + 2]]);
        }

        return triangles;
    }

    private static Vec3 interpolate(
            float isoLevel,
            Vec3 pa, Vec3 pb,
            float da, float db
    ) {
        // Sécurité : densités quasi-égales → milieu de l'arête
        if (Math.abs(da - db) < EPSILON) {
            return pa.lerp(pb, 0.5);
        }
        double t = (isoLevel - da) / (db - da);
        return pa.lerp(pb, t);
    }
}
