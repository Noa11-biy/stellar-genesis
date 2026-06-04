package com.stellargenesis.core.world.density;

import com.stellargenesis.core.noise.OpenSimplex2;

public class DensityFieldGenerator {

    private static final int CHUNK_SIZE = 16;       // nombre de cubes par côté
    private static final int FIELD_SIZE = 17;        // nombre de sommets par côté

    private final long seed;

    // Paramètres heightmap (fBm)
    private int octaves = 6;
    private float persistence = 0.5f;
    private float lacunarity = 2.0f;
    private float baseFrequency = 0.01f;
    private float amplitude = 20f;
    private float baseHeight = 64f;

    public DensityFieldGenerator(long seed) {
        this.seed = seed;
    }

    /**
     * Génère le DensityField pour un chunk donné.
     */
    public DensityField generate(int chunkX, int chunkY, int chunkZ) {
        DensityField field = new DensityField(FIELD_SIZE);

        // Précalcul heightmap 17x17 (une seule fois)
        float[][] heights = new float[FIELD_SIZE][FIELD_SIZE];
        for (int lz = 0; lz < FIELD_SIZE; lz++) {
            for (int lx = 0; lx < FIELD_SIZE; lx++) {
                float worldX = chunkX * CHUNK_SIZE + lx;
                float worldZ = chunkZ * CHUNK_SIZE + lz;
                heights[lx][lz] = computeHeightmap(worldX, worldZ);
            }
        }

        // Remplir le champ 3D en lisant le précalcul
        field.fill((lx, ly, lz) -> {
            float worldY = chunkY * CHUNK_SIZE + ly;
            return worldY - heights[lx][lz];
        });

        return field;
    }

    /**
     * Heightmap multi-octave (fBm) en (x, z).
     */
    private float computeHeightmap(float x, float z) {
        float total = 0f;
        float frequency = baseFrequency;
        float amp = 1f;
        float maxValue = 0f;  // pour normaliser

        for (int i = 0; i < octaves; i++) {
            // OpenSimplex2 retourne une valeur ~[-1, 1]
            float noise = OpenSimplex2.noise2(seed, x * frequency, z * frequency);
            total += noise * amp;
            maxValue += amp;

            amp *= persistence;
            frequency *= lacunarity;
        }

        // Normaliser [-1, 1] puis appliquer amplitude + baseHeight
        return baseHeight + (total / maxValue) * amplitude;
    }
}
