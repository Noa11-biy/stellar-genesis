package com.stellargenesis.core.geometry;

import com.stellargenesis.core.math.Vec3;
import com.stellargenesis.core.physics.geometry.Plane;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaneTest {

    private static final double EPS = 1e-9;

    @Test
    void signedDistance_horizontalPlaneAtY5() {
        // Plan y = 5 : normale (0,1,0), d = -5
        // Vérification mentale : n·p + d = 1*y + (-5) = y - 5
        //   point (0,10,0) → 10 - 5 = +5  (au-dessus)
        //   point (0, 0,0) →  0 - 5 = -5  (en-dessous)
        //   point (0, 5,0) →  5 - 5 =  0  (sur le plan)
        Plane p = new Plane(Vec3.UP, -5);

        assertEquals(+5, p.signedDistance(new Vec3(0, 10, 0)), EPS);
        assertEquals(-5, p.signedDistance(new Vec3(0,  0, 0)), EPS);
        assertEquals( 0, p.signedDistance(new Vec3(0,  5, 0)), EPS);
    }

    @Test
    void fromNormalAndPoint_verticalPlaneAtX3() {
        // Plan x = 3 construit via le point (3,0,0) et la normale (1,0,0)
        Plane p = Plane.fromNormalAndPoint(Vec3.RIGHT, new Vec3(3, 0, 0));

        assertEquals(+2, p.signedDistance(new Vec3(5, 0, 0)), EPS);
        assertEquals(-3, p.signedDistance(new Vec3(0, 0, 0)), EPS);
        assertEquals( 0, p.signedDistance(new Vec3(3, 7, 42)), EPS);
    }

    @Test
    void fromNormalAndPoint_normalizesNonUnitNormal() {
        // On passe une normale NON unitaire (longueur 5)
        // Le plan doit quand même être correct après normalisation interne.
        Vec3 nonUnit = new Vec3(0, 5, 0);  // longueur 5, direction Y
        Plane p = Plane.fromNormalAndPoint(nonUnit, new Vec3(0, 5, 0));

        // Doit se comporter comme le plan y=5
        assertEquals(+5, p.signedDistance(new Vec3(0, 10, 0)), EPS);
        // Et la normale stockée doit être unitaire
        assertEquals(1.0, p.normal().length(), EPS);
    }

    @Test
    void fromCoefficients_normalizesProperly() {
        // Plan y=5 écrit avec des coefficients NON normalisés :
        //   0x + 2y + 0z - 10 = 0   (équivalent à y - 5 = 0 après division par 2)
        Plane p = Plane.fromCoefficients(0, 2, 0, -10);

        assertEquals(+5, p.signedDistance(new Vec3(0, 10, 0)), EPS);
        assertEquals(1.0, p.normal().length(), EPS);
    }

    @Test
    void fromCoefficients_rejectsDegenerateNormal() {
        // Normale nulle → plan non défini → exception attendue
        assertThrows(IllegalArgumentException.class,
                () -> Plane.fromCoefficients(0, 0, 0, 5));
    }
}
