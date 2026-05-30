package com.stellargenesis.core.world.marching;

import com.stellargenesis.core.math.Vec3;
import com.stellargenesis.core.world.meshing.MarchingCube;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MarchingCubesTest {

    private static Vec3[] unitCubeCorners() {
        return new Vec3[]{
                new Vec3(0, 0, 0), // v0
                new Vec3(1, 0, 0), // v1
                new Vec3(1, 0, 1), // v2
                new Vec3(0, 0, 1), // v3
                new Vec3(0, 1, 0), // v4
                new Vec3(1, 1, 0), // v5
                new Vec3(1, 1, 1), // v6
                new Vec3(0, 1, 1)  // v7
        };

    }

    @Test
    void cubeEntierementDehors_retourneAucunTriangle() {
        Vec3[] corners = unitCubeCorners();
        float[] densities = {1, 1, 1, 1, 1, 1, 1, 1}; // tous > 0

        List<Vec3> result = MarchingCube.polygonise(corners, densities, 0f);

        assertEquals(0, result.size(), "Cube vide → 0 sommets");
    }

    @Test
    void cubeEntierementDedans_retourneAucunTriangle() {
        Vec3[] corners = unitCubeCorners();
        float[] densities = {-1, -1, -1, -1, -1, -1, -1, -1}; // tous < 0

        List<Vec3> result = MarchingCube.polygonise(corners, densities, 0f);

        assertEquals(0, result.size(), "Cube plein → 0 sommets");
    }

    @Test
    void solPlatHorizontal_genere2Triangles() {
        Vec3[] corners = unitCubeCorners();
        // v0-v3 (bas) = roche, v4-v7 (haut) = air
        float[] densities = {-1, -1, -1, -1, 1, 1, 1, 1};

        List<Vec3> result = MarchingCube.polygonise(corners, densities, 0f);

        // 2 triangles = 6 sommets
        assertEquals(6, result.size(), "Sol plat → 2 triangles = 6 sommets");

        // Propriété géométrique : tous les sommets doivent être à y = 0.5
        // (au milieu du cube car densités symétriques -1 / +1)
        for (Vec3 v : result) {
            assertEquals(0.5, v.y, 1e-6, "Sommet pas au milieu vertical");
        }
    }

    @Test
    void unSeulCoinDansRoche_genere1Triangle() {
        Vec3[] corners = unitCubeCorners();
        float[] densities = {-1, 1, 1, 1, 1, 1, 1, 1}; // seul v0 < 0

        List<Vec3> result = MarchingCube.polygonise(corners, densities, 0f);

        assertEquals(3, result.size(), "1 coin coupé → 1 triangle = 3 sommets");
    }


    @Test
    void unSeulCoinDansAir_genere1Triangle() {
        Vec3[] corners = unitCubeCorners();
        float[] densities = {1, -1, -1, -1, -1, -1, -1, -1}; // seul v0 > 0

        List<Vec3> result = MarchingCube.polygonise(corners, densities, 0f);

        assertEquals(3, result.size(), "Inverse de config 1 → 1 triangle aussi");
    }


}


