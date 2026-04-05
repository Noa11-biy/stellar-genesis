package com.stellargenesis.core.math;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Vec3Test {

    private static final double EPS = 1e-9;

    @Test
    void additionDeDeuxVecteurs() {
        Vec3 a = new Vec3(1, 2, 3);
        Vec3 b = new Vec3(4, 5, 6);
        Vec3 r = a.add(b);
        assertEquals(5.0, r.x, EPS);
        assertEquals(7.0, r.y, EPS);
        assertEquals(9.0, r.z, EPS);
    }

    @Test
    void soustractionDeDeuxVecteurs() {
        Vec3 a = new Vec3(5, 7, 9);
        Vec3 b = new Vec3(1, 2, 3);
        Vec3 r = a.sub(b);
        assertEquals(4.0, r.x, EPS);
        assertEquals(5.0, r.y, EPS);
        assertEquals(6.0, r.z, EPS);
    }

    @Test
    void produitScalaire() {
        // (1,0,0) · (0,1,0) = 0 → perpendiculaires
        Vec3 right = Vec3.RIGHT;
        Vec3 up = Vec3.UP;
        assertEquals(0.0, right.dot(up), EPS);

        // (1,2,3) · (4,5,6) = 4+10+18 = 32
        Vec3 a = new Vec3(1, 2, 3);
        Vec3 b = new Vec3(4, 5, 6);
        assertEquals(32.0, a.dot(b), EPS);
    }

    @Test
    void produitVectoriel() {
        // i × j = k → (1,0,0) × (0,1,0) = (0,0,1)
        Vec3 r = Vec3.RIGHT.cross(Vec3.UP);
        assertEquals(0.0, r.x, EPS);
        assertEquals(0.0, r.y, EPS);
        assertEquals(1.0, r.z, EPS);
    }

    @Test
    void normeDuVecteur() {
        Vec3 v = new Vec3(3, 4, 0);
        assertEquals(5.0, v.length(), EPS); // triangle 3-4-5
    }

    @Test
    void normalisation() {
        Vec3 v = new Vec3(0, 0, 5);
        Vec3 n = v.normalize();
        assertEquals(0.0, n.x, EPS);
        assertEquals(0.0, n.y, EPS);
        assertEquals(1.0, n.z, EPS);
    }

    @Test
    void normalisationVecteurNulRetourneZero() {
        Vec3 n = Vec3.ZERO.normalize();
        assertEquals(Vec3.ZERO, n);
    }

    @Test
    void lerpMilieu() {
        Vec3 a = new Vec3(0, 0, 0);
        Vec3 b = new Vec3(10, 10, 10);
        Vec3 mid = a.lerp(b, 0.5);
        assertEquals(5.0, mid.x, EPS);
        assertEquals(5.0, mid.y, EPS);
        assertEquals(5.0, mid.z, EPS);
    }
}
