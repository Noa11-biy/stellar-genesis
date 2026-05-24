package com.stellargenesis.core.physics.math;

import com.stellargenesis.core.math.Mat4;
import com.stellargenesis.core.math.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrustumTest {

    @Test
    void frustumFromIdentityIsCubeMinus1Plus1() {
        Frustum f = Frustum.fromViewProjection(Mat4.identity());

        assertTrue(f.contains(new Vec3(0, 0, 0)), "origine doit être dedans");
        assertTrue(f.contains(new Vec3(0.9, 0.9, 0.9)), "coin du cube dedans");
        assertFalse(f.contains(new Vec3(2, 0, 0)), "hors X");
        assertFalse(f.contains(new Vec3(0, -2, 0)), "hors Y");
        assertFalse(f.contains(new Vec3(0, 0, 2)), "hors Z");
    }

    @Test
    void frustumFromPerspectiveContainsPointInFront() {
        Mat4 proj = Mat4.perspective(Math.toRadians(60), 16.0 / 9.0, 0.1, 1000);
        Mat4 view = Mat4.lookAt(
                new Vec3(0, 0, 0),
                new Vec3(0, 0, -1),
                new Vec3(0, 1, 0)
        );
        Mat4 vp = proj.multiply(view);

        Frustum f = Frustum.fromViewProjection(vp);

        assertTrue(f.contains(new Vec3(0, 0, -10)), "devant la caméra → visible");
        assertFalse(f.contains(new Vec3(0, 0, 10)), "derrière la caméra → invisible");
    }
}
