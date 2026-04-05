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
}
