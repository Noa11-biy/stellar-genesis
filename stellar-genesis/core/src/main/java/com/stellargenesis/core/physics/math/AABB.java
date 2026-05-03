package com.stellargenesis.core.physics.math;

import com.stellargenesis.core.math.Vec3;

/**
 * Axis-Aligned Bounding Box — boîte rectangulaire alignée sur les axes monde.
 *
 * Représentation : deux coins opposés (min, max).
 * On exige min.x <= max.x, min.y <= max.y, min.z <= max.z.
 *
 * Immutable : une fois construite, l'AABB ne change pas.
 *
 * Usage principal dans le projet : englober un chunk 16×16×16 pour
 * tester rapidement s'il est visible par la caméra (frustum culling).
 */
public final class AABB {

    private final Vec3 min;
    private final Vec3 max;

    /**
     * Construction directe à partir des deux coins.
     * @throws IllegalArgumentException si min n'est pas composante-par-composante <= max
     */
    public AABB(Vec3 min, Vec3 max) {
        if (!(min.x <= max.x) || !(min.y <= max.y) || !(min.z<= max.z)){
            throw new IllegalArgumentException("Les vecteurs ne sont pas bien initialisés");
        }
        this.min = min;
        this.max = max;
    }

    /**
     * Construit une AABB depuis un centre et des demi-dimensions.
     *
     * Dérivation :
     *   min = centre - half
     *   max = centre + half
     *
     * @throws IllegalArgumentException si une composante de half est négative
     */
    public static AABB fromCenterAndHalf(Vec3 center, Vec3 half) {
        if (!(half.x >= 0) || !(half.y >=0) || !(half.z >= 0)){
            throw new IllegalArgumentException("Les demi-dimensions sont négatifs");
        }
        Vec3 min = center.sub(half);
        Vec3 max = center.add(half);
        return new AABB(min, max);
    }

    public Vec3 min() { return min; }
    public Vec3 max() { return max; }

    /**
     * Centre de la boîte : (min + max) / 2 composante par composante.
     */
    public Vec3 center() {
        double cx = (min.x + max.x) / 2;
        double cy = (min.y + max.y) / 2;
        double cz = (min.z + max.z) / 2;

        return new Vec3(cx, cy, cz);
    }

    /**
     * Demi-dimensions : (max - min) / 2 composante par composante.
     * Toutes les composantes sont >= 0 par construction.
     */
    public Vec3 halfExtents() {
        double cx = (max.x - min.x) / 2;
        double cy = (max.y - min.y) / 2;
        double cz = (max.z - min.z) / 2;

        return new Vec3(cx, cy, cz);
    }

    public boolean contains(Vec3 point) {
        return min.x <= point.x && point.x <= max.x
                && min.y <= point.y && point.y <= max.y
                && min.z <= point.z && point.z <= max.z;
    }

    public boolean intersects(AABB other) {
        if (this.max.x < other.min.x || other.max.x < this.min.x) return false;
        if (this.max.y < other.min.y || other.max.y < this.min.y) return false;
        if (this.max.z < other.min.z || other.max.z < this.min.z) return false;
        return true;
    }

    public AABB expand(Vec3 point) {
        Vec3 newMin = new Vec3(
                Math.min(this.min.x, point.x),  // min.x vs point.x
        Math.min(this.min.y, point.y),  // min.y vs point.y
        Math.min(this.min.z, point.z)   // min.z vs point.z
    );
        Vec3 newMax = new Vec3(
                Math.max(this.max.x, point.x),
        Math.max(this.max.y, point.y),
        Math.max(this.max.z, point.z)
    );
        return new AABB(newMin, newMax);
    }

    public AABB merge(AABB other){
      Vec3 newMin = new Vec3(
              Math.min(this.min.x, other.min.x),
              Math.min(this.min.y, other.min.y),
              Math.min(this.min.z, other.min.z)
      );

      Vec3 newMax = new Vec3(
              Math.max(this.max.x, other.max.x),
              Math.max(this.max.y, other.max.y),
              Math.max(this.max.z, other.max.z)
      );

      return new AABB(newMin, newMax);
    }


    @Override
    public String toString() {
        return "AABB[min=" + min + ", max=" + max + "]";
    }
}
