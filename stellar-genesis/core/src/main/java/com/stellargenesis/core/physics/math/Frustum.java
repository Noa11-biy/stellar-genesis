package com.stellargenesis.core.physics.math;

import com.stellargenesis.core.math.Mat4;
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

        Vec3 center = box.center();
        Vec3 half = box.halfExtents();

        for (Plane p : planes){
            Vec3 n = p.normal();

            double radius = Math.abs(n.x) * half.x + Math.abs(n.y) * half.y + Math.abs(n.z) * half.z;
            double dist = p.signedDistance(center) + radius;

            if (dist < 0) {return false;}
        }
        return true;
    }


    /**
     * Extrait les 6 plans du frustum depuis une matrice ViewProjection.
     * Algorithme de Gribb-Hartmann (2001).
     *
     * Les normales pointent vers l'intérieur du frustum, ce qui correspond
     * à la convention utilisée par contains() et intersects().
     *
     * @param vp matrice View · Projection (row-major)
     */
    public static Frustum fromViewProjection (Mat4 vp){
        // Récupère les 4 lignes de la matrice
        // m[ligne][colonne] : ligne i = (m[i][0], m[i][1], m[i][2], m[i][3])
        double[][] m = vp.m;

        // Left  : row3 + row0
        Plane left = Plane.fromCoefficients(
                m[3][0] + m[0][0],
                m[3][1] + m[0][1],
                m[3][2] + m[0][2],
                m[3][3] + m[0][3]
        );

        Plane right = Plane.fromCoefficients(
                m[3][0] - m[0][0],
                m[3][1] - m[0][1],
                m[3][2] - m[0][2],
                m[3][3] - m[0][3]
        );

        Plane bottom = Plane.fromCoefficients(
                m[3][0] + m[1][0],
                m[3][1] + m[1][1],
                m[3][2] + m[1][2],
                m[3][3] + m[1][3]
        );

        Plane top = Plane.fromCoefficients(
                m[3][0] - m[1][0],
                m[3][1] - m[1][1],
                m[3][2] - m[1][2],
                m[3][3] - m[1][3]
        );

        Plane near = Plane.fromCoefficients(
                m[3][0] + m[2][0],
                m[3][1] + m[2][1],
                m[3][2] + m[2][2],
                m[3][3] + m[2][3]
        );

        Plane far = Plane.fromCoefficients(
                m[3][0] - m[2][0],
                m[3][1] - m[2][1],
                m[3][2] - m[2][2],
                m[3][3] - m[2][3]
        );

        return new Frustum(near, far, left, right, top, bottom);
    }

    public Plane[] planes() { return planes; }
}
