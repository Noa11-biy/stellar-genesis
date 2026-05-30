package com.stellargenesis.core.world.density;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SphereDensityTest {

    private SphereDensity sphere;
    private static final float EPSILON = 0.0001f;

    @BeforeEach
    void setUp() {
        sphere = new SphereDensity(0, 0, 0, 10);
    }

    @Test
    void centerHasNegativeDensityEqualToMinusRadius() {
        assertEquals(-10, sphere.sample(0, 0, 0), EPSILON);
    }

    @Test
    void pointOnSurfaceHasZeroDensity() {
        assertEquals(0, sphere.sample(10, 0, 0), EPSILON);
        assertEquals(0, sphere.sample(0, 10, 0), EPSILON);
        assertEquals(0, sphere.sample(0, 0, 10), EPSILON);
        assertEquals(0, sphere.sample(-10, 0, 0), EPSILON);
    }


    @Test
    void pointOutsideHasPositiveDensity() {
        assertEquals(10, sphere.sample(20, 0, 0), EPSILON);
    }

    @Test
    void diagonalPointRespectsEuclideanDistance() {
        assertEquals(0, sphere.sample(6, 8, 0), EPSILON);
    }

    @Test
    void constructorRejectsNegativeOrZeroRadius() {
        assertThrows(IllegalArgumentException.class, ()-> new SphereDensity(0,0,0, -5));
        assertThrows(IllegalArgumentException.class, ()-> new SphereDensity(0,0,0, 0));
    }
}
