package com.stellargenesis.core.physics.geometry;

import com.stellargenesis.core.math.Vec3;

/**
 * Plane — Plan infini dans l'espace 3D.
 *
 * Équation : n · p + d = 0
 *   n = normale unitaire au plan
 *   p = un point quelconque de l'espace
 *   d = décalage signé par rapport à l'origine
 *
 * Convention Stellar Genesis :
 *   Les 6 plans d'un Frustum ont leur normale orientée VERS L'INTÉRIEUR.
 *   → distance signée > 0 signifie "côté intérieur du frustum".
 *
 * Pourquoi cette classe :
 *   Le frustum de la caméra est défini par 6 plans. Tout le test
 *   "ce chunk est-il visible ?" se réduit à 6 calculs de distance
 *   signée entre les coins de la AABB du chunk et ces plans.
 *
 * Immuabilité : cohérent avec Vec3, sûr pour le multi-threading.
 *
 * @author Noa Moal
 */

public final class Plane {

    /** Normal unitaire - doit être normalisée à la construction*/
    private final Vec3 normal;

    /** Terme constant d dans l'équation n · p + d = 0. */
    private final double d;

    /**
     * Constructeur direct. ATTENTION : la normale doit DÉJÀ être unitaire.
     * En cas de doute, utilise fromNormalAndPoint() ou fromCoefficients().
     */
    public Plane(Vec3 normal, double d) {
        this.normal = normal;
        this.d = d;
    }


    /**
     * Plan passant par `point` avec la normale donnée (normalisée ici).
     *
     * Dérivation :
     *   n · point + d = 0   (le point appartient au plan)
     *   →  d = -(n · point)
     */
    public static Plane fromNormalAndPoint(Vec3 normal, Vec3 point) {
        // normaliser la normale
        Vec3 n = normal.normalize();
        // calculer d = -(n_unit · point)
        double d = -(n.dot(point));
        // return new Plane(n_unit, d)
        return new Plane(n, d);
    }

    /**
     * Plan depuis les 4 coefficients bruts (a, b, c, d) de l'équation
     * ax + by + cz + d = 0.
     *
     * POURQUOI cette méthode ?
     *   Quand on extrait le frustum depuis une matrice de projection-vue,
     *   on obtient directement les 4 coefficients (a, b, c, d).
     *   Mais (a, b, c) n'a aucune raison d'être unitaire → il faut
     *   normaliser en divisant les 4 coefficients par |(a,b,c)|.
     *
     *   Invariant du plan : multiplier (a,b,c,d) par une même constante
     *   k ≠ 0 ne change pas le plan géométrique, mais change la "distance
     *   signée" par ce facteur k. D'où la nécessité de normaliser si on
     *   veut une vraie distance euclidienne.
     */
    public static Plane fromCoefficients(double a, double b, double c, double d) {
        double len = Math.sqrt(a * a + b * b + c * c);
        if (len < 1e-10) {
            throw new IllegalArgumentException("Normale de plan dégénérée (nulle)");
        }
        double inv = 1.0 / len;
        return new Plane(new Vec3(a * inv, b * inv, c * inv), d * inv);
    }

    /**
     * Distance signée d'un point au plan.
     *
     *   > 0  : point du côté de la normale  (INTÉRIEUR pour un frustum)
     *   = 0  : point sur le plan
     *   < 0  : point du côté opposé         (EXTÉRIEUR pour un frustum)
     *
     * Formule : dist = n · p + d
     *
     * C'est l'opération la plus chaude du culling — appelée des
     * milliers de fois par frame. 3 mul + 3 add, c'est imbattable.
     */
    public double signedDistance(Vec3 point) {
        return  normal.dot(point)+d;
    }

    public Vec3 normal() { return normal; }
    public double d()    { return d; }

    @Override
    public String toString() {
        return String.format("Plane[n=%s, d=%.3f]", normal, d);
    }
}
