package com.stellargenesis.core.math;

/**
 * Fonctions mathématiques utilitaires utilisées partout dans le projet.
 *
 * Ces fonctions n'existent pas dans java.lang.Math ou font exactement
 * ce dont on a besoin sans conversion de types.
 *
 * @author Noa Moal
 */

public final class MathUtils {

    private MathUtils(){
        throw new AssertionError("Classe utilitaire, pas d'instanciation");
    }

    // ============================================================
    // CLAMP : restreindre une valeur dans un intervalle [min, max]
    //
    // Exemple : clamp(150, 0, 100) → 100
    //           clamp(-5, 0, 100)  → 0
    //           clamp(50, 0, 100)  → 50
    //
    // Utilité : empêcher la vie du joueur de dépasser le max,
    //           borner une température, limiter une vitesse
    // ============================================================

    public static double clamp(double value, double min, double max){
        if(value < min) return min;
        if(value > max) return max;
        return value;
    }

    // ============================================================
    // LERP : interpolation linéaire entre deux valeurs
    //
    // lerp(a, b, t) = a + t × (b - a)
    //   t=0 → a, t=1 → b, t=0.5 → milieu
    //
    // Version scalaire (la version vectorielle est dans Vec3)
    // ============================================================

    public static double lerp(double a, double b, double t){
        return a + t* (b - a);
    }

    // ============================================================
    // INVERSE LERP : "où se situe value entre a et b ?"
    //
    // inverseLerp(0, 100, 25) → 0.25
    // inverseLerp(0, 100, 50) → 0.5
    //
    // Utilité : normaliser une valeur dans un intervalle
    // Exemple : température entre T_min et T_max → ratio 0..1 pour la couleur
    // ============================================================

    public static double inverseLerp(double a, double b, double value){
        if(Math.abs( b - a) < 1e-10) return 0.0;
        return (value - a) / (b - a);
    }

    // ============================================================
    // REMAP : transposer une valeur d'un intervalle vers un autre
    //
    // remap(50, 0,100, 0,1) → 0.5
    // remap(50, 0,100, 200,400) → 300
    //
    // Utilité : convertir une altitude en pression,
    //           une température en couleur, etc.
    // ============================================================

    public static double remap(double value, double fromMin, double fromMax, double toMin, double toMax){
        double t = inverseLerp(fromMin, fromMax, value);
        return lerp(toMin, toMax, t);
    }

    // ============================================================
    // APPROXIMATELY EQUAL : comparaison de doubles avec tolérance
    //
    // Les doubles ne sont JAMAIS exactement égaux à cause de la
    // représentation en virgule flottante (IEEE 754).
    // 0.1 + 0.2 ≠ 0.3 en double !
    //
    // On compare avec une tolérance epsilon.
    // ============================================================

    public static boolean approxEquals(double a, double b, double epsilon){
        return Math.abs(a-b) < epsilon;
    }

    public static boolean approxEquals(double a, double b){
        return approxEquals(a, b, 1e-6);
    }

}
