package com.stellargenesis.core.math;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MathUtilsTest {

    @Test
    void clampDansLIntervalle() {
        assertEquals(50.0, MathUtils.clamp(50, 0, 100));
    }

    @Test
    void clampSousLeMin() {
        assertEquals(0.0, MathUtils.clamp(-10, 0, 100));
    }

    @Test
    void clampAuDessusMax() {
        assertEquals(100.0, MathUtils.clamp(150, 0, 100));
    }

    @Test
    void lerpExtremes() {
        assertEquals(0.0, MathUtils.lerp(0, 100, 0.0), 1e-9);
        assertEquals(100.0, MathUtils.lerp(0, 100, 1.0), 1e-9);
        assertEquals(50.0, MathUtils.lerp(0, 100, 0.5), 1e-9);
    }

    @Test
    void inverseLerp() {
        assertEquals(0.25, MathUtils.inverseLerp(0, 100, 25), 1e-9);
    }

    @Test
    void remapValeur() {
        // 50 dans [0,100] → 300 dans [200,400]
        assertEquals(300.0, MathUtils.remap(50, 0, 100, 200, 400), 1e-9);
    }

    @Test
    void approxEquals() {
        assertTrue(MathUtils.approxEquals(0.1 + 0.2, 0.3, 1e-9));
        assertFalse(MathUtils.approxEquals(1.0, 2.0));
    }

    @Test
    void smoothstepBornes() {
        assertEquals(0.0, MathUtils.smoothstep(0.0), 1e-9);
        assertEquals(1.0, MathUtils.smoothstep(1.0), 1e-9);
        assertEquals(0.5, MathUtils.smoothstep(0.5), 1e-9);
    }

    @Test
    void smoothstepClamp() {
        // Valeurs hors [0,1] doivent être clampées
        assertEquals(0.0, MathUtils.smoothstep(-5.0), 1e-9);
        assertEquals(1.0, MathUtils.smoothstep(10.0), 1e-9);
    }

    @Test
    void smootherstepBornes() {
        assertEquals(0.0, MathUtils.smootherstep(0.0), 1e-9);
        assertEquals(1.0, MathUtils.smootherstep(1.0), 1e-9);
        assertEquals(0.5, MathUtils.smootherstep(0.5), 1e-9);
    }

    @Test
    void noise1DDeterministe() {
        // Même entrée → même sortie, toujours
        double a = MathUtils.noise1D(42.7);
        double b = MathUtils.noise1D(42.7);
        assertEquals(a, b, 0.0);
    }

    @Test
    void noise1DDansIntervalle() {
        // Le bruit doit rester dans [-1, 1]
        for (int i = -1000; i < 1000; i++) {
            double val = MathUtils.noise1D(i * 0.1);
            assertTrue(val >= -1.0 && val <= 1.0,
                    "noise1D(" + (i * 0.1) + ") = " + val + " hors [-1,1]");
        }
    }

    @Test
    void noise1DVariation() {
        // Deux entrées différentes → sorties (probablement) différentes
        assertNotEquals(MathUtils.noise1D(0.0), MathUtils.noise1D(1.0));
    }

    @Test
    void fbmNoise1DDansIntervalle() {
        for (int i = -500; i < 500; i++) {
            double val = MathUtils.fbmNoise1D(i * 0.1, 6, 0.5, 2.0);
            assertTrue(val >= -1.0 && val <= 1.0,
                    "fbmNoise1D hors [-1,1] : " + val);
        }
    }

    @Test
    void fbmNoise1DPlusDetailleAvecPlusOctaves() {
        // Plus d'octaves = plus de variation locale
        // On mesure la "rugosité" (somme des différences entre points voisins)
        double rugositeO2 = 0, rugositeO8 = 0;
        for (int i = 0; i < 1000; i++) {
            double x = i * 0.01;
            rugositeO2 += Math.abs(MathUtils.fbmNoise1D(x + 0.01, 2, 0.5, 2.0)
                    - MathUtils.fbmNoise1D(x, 2, 0.5, 2.0));
            rugositeO8 += Math.abs(MathUtils.fbmNoise1D(x + 0.01, 8, 0.5, 2.0)
                    - MathUtils.fbmNoise1D(x, 8, 0.5, 2.0));
        }
        assertTrue(rugositeO8 > rugositeO2,
                "8 octaves devrait être plus rugueux que 2");
    }
}
