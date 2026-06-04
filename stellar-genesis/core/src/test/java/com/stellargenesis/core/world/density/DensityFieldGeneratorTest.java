package com.stellargenesis.core.world.density;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DensityFieldGeneratorTest {

    @Test
    void testGeneratesNonEmptyField() {
        DensityFieldGenerator gen = new DensityFieldGenerator(12345L);

        boolean hasNegative = false, hasPositive = false;

        // Scanner verticalement plusieurs chunks pour traverser la surface
        for (int cy = 0; cy < 10 && !(hasNegative && hasPositive); cy++) {
            DensityField field = gen.generate(0, cy, 0);
            for (int z = 0; z < 17; z++)
                for (int y = 0; y < 17; y++)
                    for (int x = 0; x < 17; x++) {
                        float v = field.get(x, y, z);
                        if (v < 0) hasNegative = true;
                        if (v > 0) hasPositive = true;
                    }
        }

        assertTrue(hasNegative, "Le monde doit contenir du sol quelque part");
        assertTrue(hasPositive, "Le monde doit contenir de l'air quelque part");
    }

    @Test
    void testTwoAdjacentChunksAreContiguous() {
        DensityFieldGenerator gen = new DensityFieldGenerator(42L);
        DensityField fieldA = gen.generate(0, 0, 0);
        DensityField fieldB = gen.generate(1, 0, 0);  // chunk voisin en X

        // Le bord droit de A (lx=16) doit égaler le bord gauche de B (lx=0)
        for (int y = 0; y < 17; y++)
            for (int z = 0; z < 17; z++) {
                assertEquals(fieldA.get(16, y, z), fieldB.get(0, y, z), 1e-5f,
                        "Discontinuité à (y=" + y + ", z=" + z + ")");
            }
    }
}
