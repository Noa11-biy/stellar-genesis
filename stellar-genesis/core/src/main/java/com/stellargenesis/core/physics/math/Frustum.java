package com.stellargenesis.core.physics.math;

import com.stellargenesis.core.math.Vec3;
import com.stellargenesis.core.physics.geometry.Plane;

/**
 * Frustum — Volume de visibilité d'une caméra (pyramide tronquée).
 *
 * Délimité par 6 plans dont les normales pointent VERS L'INTÉRIEUR.
 * → Un objet est visible ssi sa distance signée à chaque plan est >= 0.
 *
 * Usage typique :
 *   if (frustum.intersects(chunk.getAABB())) {
 *       renderer.draw(chunk);
 *   }
 */
public final class Frustum {

    private final Plane[] planes; // [near, far, left, right, top, bottom]

    public Frustum(Plane near, Plane far, Plane left, Plane right, Plane top, Plane bottom) {
        this.planes = new Plane[] { near, far, left, right, top, bottom };
    }

    /**
     * Teste si un point est dans le frustum.
     * Visible ssi distance signée >= 0 pour TOUS les plans.
     */
    public boolean contains(Vec3 point) {
        for (Plane plane : planes) {
            if (plane.signedDistance(point) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Teste si une AABB est (au moins partiellement) dans le frustum.
     *
     * Pour chaque plan :
     *   dist = n · center + (|nx|·ex + |ny|·ey + |nz|·ez) + d
     *   si dist < 0 → AABB entièrement derrière ce plan → return false (cull)
     *
     * Si on passe les 6 plans → return true (visible)
     */
    public boolean intersects(AABB box) {
        // TODO 2 : récupérer center et halfExtents de la box
        Vec3 center = box.center();
        Vec3 half = box.halfExtents();
        // TODO 3 : pour chaque plan :
        //            - n = plan.normal()
        //            - radius = |n.x|*half.x + |n.y|*half.y + |n.z|*half.z
        //            - dist = plan.signedDistance(center) + radius
        //            - si dist < 0 → return false
        Vec3 n;

        // TODO 4 : return true
        return true;
    }

    public Plane[] planes() { return planes; }
}
