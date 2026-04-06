package com.stellargenesis.core.math;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Mat4Test {

    private static final double EPS = 1e-9;

    @Test
    void identiteNeChangeRien() {
        Mat4 id = Mat4.identity();
        Vec3 v = new Vec3(3, 7, -2);
        Vec3 r = id.transformPoint(v);
        assertEquals(3.0, r.x, EPS);
        assertEquals(7.0, r.y, EPS);
        assertEquals(-2.0, r.z, EPS);
    }

    @Test
    void translationDeplaceUnPoint() {
        Mat4 t = Mat4.translation(10, 20, 30);
        Vec3 r = t.transformPoint(new Vec3(1, 2, 3));
        assertEquals(11.0, r.x, EPS);
        assertEquals(22.0, r.y, EPS);
        assertEquals(33.0, r.z, EPS);
    }

    @Test
    void translationIgnoreeLesDirections() {
        Mat4 t = Mat4.translation(10, 20, 30);
        Vec3 r = t.transformDirection(new Vec3(1, 0, 0));
        assertEquals(1.0, r.x, EPS);
        assertEquals(0.0, r.y, EPS);
        assertEquals(0.0, r.z, EPS);
    }

    @Test
    void scaleMiseAEchelle() {
        Mat4 s = Mat4.scale(2, 3, 4);
        Vec3 r = s.transformPoint(new Vec3(1, 1, 1));
        assertEquals(2.0, r.x, EPS);
        assertEquals(3.0, r.y, EPS);
        assertEquals(4.0, r.z, EPS);
    }

    @Test
    void rotationZ90DegresTourneXversY() {
        Mat4 rz = Mat4.rotationZ(Math.PI / 2);
        Vec3 r = rz.transformPoint(new Vec3(1, 0, 0));
        assertEquals(0.0, r.x, EPS);
        assertEquals(1.0, r.y, EPS);
        assertEquals(0.0, r.z, EPS);
    }

    @Test
    void rotationX90DegresTourneYversZ() {
        Mat4 rx = Mat4.rotationX(Math.PI / 2);
        Vec3 r = rx.transformPoint(new Vec3(0, 1, 0));
        assertEquals(0.0, r.x, EPS);
        assertEquals(0.0, r.y, EPS);
        assertEquals(1.0, r.z, EPS);
    }

    @Test
    void rotationY90DegresTourneZversX() {
        Mat4 ry = Mat4.rotationY(Math.PI / 2);
        Vec3 r = ry.transformPoint(new Vec3(0, 0, 1));
        assertEquals(1.0, r.x, EPS);
        assertEquals(0.0, r.y, EPS);
        assertEquals(0.0, r.z, EPS);
    }

    @Test
    void multiplicationIdentiteNeutre() {
        Mat4 a = Mat4.translation(5, 10, 15);
        Mat4 r = a.multiply(Mat4.identity());
        Vec3 v = r.transformPoint(Vec3.ZERO);
        assertEquals(5.0, v.x, EPS);
        assertEquals(10.0, v.y, EPS);
        assertEquals(15.0, v.z, EPS);
    }

    @Test
    void chaineTranslationPuisRotation() {
        // D'abord translater (1,0,0), puis rotation Z de 90°
        // Résultat attendu : (0,1,0)
        Mat4 t = Mat4.translation(1, 0, 0);
        Mat4 rz = Mat4.rotationZ(Math.PI / 2);
        Mat4 combined = rz.multiply(t); // rotation appliquée après translation
        Vec3 r = combined.transformPoint(Vec3.ZERO);
        assertEquals(0.0, r.x, EPS);
        assertEquals(1.0, r.y, EPS);
        assertEquals(0.0, r.z, EPS);
    }

    @Test
    void determinantIdentiteVautUn() {
        assertEquals(1.0, Mat4.identity().determinant(), EPS);
    }

    @Test
    void determinantScaleVautProduit() {
        Mat4 s = Mat4.scale(2, 3, 5);
        assertEquals(30.0, s.determinant(), EPS);
    }

    @Test
    void inverseAnnuleTranslation() {
        Mat4 t = Mat4.translation(7, -3, 12);
        Mat4 inv = t.inverse();
        assertNotNull(inv);
        Vec3 r = t.multiply(inv).transformPoint(new Vec3(5, 5, 5));
        assertEquals(5.0, r.x, EPS);
        assertEquals(5.0, r.y, EPS);
        assertEquals(5.0, r.z, EPS);
    }

    @Test
    void inverseMatriceSinguliereRetourneNull() {
        Mat4 zero = new Mat4(); // tout à 0, déterminant = 0
        assertNull(zero.inverse());
    }

    @Test
    void transposeeDeIdentiteEstIdentite() {
        Mat4 id = Mat4.identity();
        Mat4 t = id.transpose();
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                assertEquals(id.m[i][j], t.m[i][j], EPS);
    }
}
