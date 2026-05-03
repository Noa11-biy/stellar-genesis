package com.stellargenesis.core.geometry;

import com.stellargenesis.core.math.Vec3;
import com.stellargenesis.core.physics.math.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AABBTest {

    @Test
    void testContainsPointsInside(){
        AABB box = new AABB(new Vec3(0,0,0), new Vec3(10,10,10));
        Vec3 point = new Vec3(5,5,5);

        assertTrue(box.contains(point));
    }

    @Test
    void testContainsPointsExt(){
        AABB box = new AABB(new Vec3(0,0,0), new Vec3(10,10,10));
        Vec3 point = new Vec3(14,14,14);

        assertFalse(box.contains(point));
    }

    @Test
    void testContainsPointsLimite(){
        AABB box = new AABB(new Vec3(0,0,0), new Vec3(10,10,10));
        Vec3 point = new Vec3(10,10,10);

        assertTrue(box.contains(point));
    }

    @Test
    void testIntersectsBoiteChevauche(){
        AABB box1 = new AABB(new Vec3(0,0,0), new Vec3(10,10,10));
        AABB box2 = new AABB(new Vec3(9,9,9), new Vec3(19,19,19));

        assertTrue(box2.intersects(box1));
    }

    @Test
    void testIntersectsBoiteChevauchePas(){
        AABB box1 = new AABB(new Vec3(0,0,0), new Vec3(10,10,10));
        AABB box2 = new AABB(new Vec3(11,11,11), new Vec3(19,19,19));

        assertFalse(box2.intersects(box1));
    }

    @Test
    void testIntersectsBoiteTouche(){
        AABB box1 = new AABB(new Vec3(0,0,0), new Vec3(10,10,10));
        AABB box2 = new AABB(new Vec3(10,10,10), new Vec3(19,19,19));

        assertTrue(box2.intersects(box1));
    }

    @Test
    void testCenterCubeSimpleSym(){
        AABB box1 = new AABB(new Vec3(0,0,0), new Vec3(10,10,10));
        Vec3 center = box1.center();

        assertEquals(5.0, center.x);
        assertEquals(5.0, center.y);
        assertEquals(5.0, center.z);
    }

    @Test
    void testCenterAsymetrique(){
        AABB box = new AABB(new Vec3(2,-4,0), new Vec3(8,0,6));
        assertEquals(5.0, box.center().x, 1e-6);
        assertEquals(-2.0, box.center().y, 1e-6);
        assertEquals(3.0, box.center().z, 1e-6);
    }


    @Test
    void testExpandPointInside() {
        // Point déjà à l'intérieur → boîte inchangée
        AABB box = new AABB(new Vec3(0, 0, 0), new Vec3(10, 10, 10));
        Vec3 point = new Vec3(5, 5, 5);

        AABB expanded = box.expand(point);

        // Doit rester (0,0,0) → (10,10,10)
        assertEquals(0.0, expanded.min().x);
        assertEquals(0.0, expanded.min().y);
        assertEquals(0.0, expanded.min().z);

        assertEquals(10.0, expanded.max().x);
        assertEquals(10.0, expanded.max().y);
        assertEquals(10.0, expanded.max().z);
    }

    @Test
    void testExpandPointOutside() {
        // Point à l'extérieur → boîte agrandie
        AABB box = new AABB(new Vec3(0, 0, 0), new Vec3(10, 10, 10));
        Vec3 point = new Vec3(-5, 5, 15); // Externe en X et en Z

        AABB expanded = box.expand(point);

        // Le nouveau min doit englober -5 en X
        assertEquals(-5.0, expanded.min().x);
        assertEquals(0.0, expanded.min().y);
        assertEquals(0.0, expanded.min().z);

        // Le nouveau max doit englober 15 en Z
        assertEquals(10.0, expanded.max().x);
        assertEquals(10.0, expanded.max().y);
        assertEquals(15.0, expanded.max().z);
    }

    @Test
    void testMergeBoitesSeparees() {
        // Deux boîtes séparées → la boîte résultante englobe les deux
        AABB box1 = new AABB(new Vec3(0, 0, 0), new Vec3(2, 2, 2));
        AABB box2 = new AABB(new Vec3(8, 8, 8), new Vec3(10, 10, 10));

        AABB merged = box1.merge(box2);

        assertEquals(0.0, merged.min().x);
        assertEquals(0.0, merged.min().y);
        assertEquals(0.0, merged.min().z);

        assertEquals(10.0, merged.max().x);
        assertEquals(10.0, merged.max().y);
        assertEquals(10.0, merged.max().z);
    }

    @Test
    void testMergeBoiteContenue() {
        // Une boîte contenue dans l'autre → résultat = la grande
        AABB grande = new AABB(new Vec3(0, 0, 0), new Vec3(10, 10, 10));
        AABB petite = new AABB(new Vec3(2, 2, 2), new Vec3(8, 8, 8));

        AABB merged = grande.merge(petite);

        // La grande boîte reste inchangée
        assertEquals(0.0, merged.min().x);
        assertEquals(0.0, merged.min().y);
        assertEquals(0.0, merged.min().z);

        assertEquals(10.0, merged.max().x);
        assertEquals(10.0, merged.max().y);
        assertEquals(10.0, merged.max().z);
    }

    @Test
    void testFromCenterAndHalfThrowsExceptionWhenNegative() {
        // Teste si l'exception est bien levée si une demi-dimension est négative
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            AABB.fromCenterAndHalf(new Vec3(0, 0, 0), new Vec3(-1, 5, 5));
        });
        assertEquals("Les demi-dimensions sont négatifs", exception.getMessage());
    }

    @Test
    void testHalfExtents(){
        AABB box = new AABB(new Vec3(2,-4,0), new Vec3(8,0,6));
        assertEquals(3.0, box.halfExtents().x, 1e-6);
        assertEquals(2.0, box.halfExtents().y, 1e-6);
        assertEquals(3.0, box.halfExtents().z, 1e-6);
    }

    @Test
    void testConstructorThrowsExceptionWhenInvalid() {
        // Teste si l'exception est bien levée quand min > max
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new AABB(new Vec3(10, 10, 10), new Vec3(0, 0, 0));
        });
        assertEquals("Les vecteurs ne sont pas bien initialisés", exception.getMessage());
    }

}
