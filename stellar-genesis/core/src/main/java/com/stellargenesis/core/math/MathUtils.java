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

    // ============================================================
    // SMOOTHSTEP : courbe lisse entre 0 et 1
    //
    // Comme lerp mais avec une transition douce (pas de cassure).
    // Formule : 3t² - 2t³
    //
    //   t=0 → 0, t=1 → 1, mais la dérivée est 0 aux bords
    //   → transition fluide, pas de "snap"
    //
    // Utilité : transition jour/nuit, fondu de biomes,
    //           mélange de textures selon l'altitude
    // ============================================================

    public static double smoothstep(double t){
        t = clamp(t, 0.0, 1.0);
        return t * t * (3.0 -2.0 * t);
    }

    // ============================================================
    // SMOOTHERSTEP : version encore plus lisse (Ken Perlin)
    //
    // Formule : 6t⁵ - 15t⁴ + 10t³
    //
    // Dérivée PREMIÈRE et SECONDE = 0 aux bords
    // → encore plus fluide, utilisé dans le bruit de Perlin
    //
    // Utilité : même chose que smoothstep mais quand on a besoin
    //           que la courbe soit C² (pas de discontinuité d'accélération)
    // ============================================================

    public static double smootherstep(double t){
        t = clamp(t, 0.0 , 1.0);
        return t * t * t *(t* (t * 6.0 - 15.0) + 10.0);
    }

    // ============================================================
    // NOISE 1D : bruit pseudo-aléatoire déterministe
    //
    // Entrée : un double quelconque (position, temps...)
    // Sortie : valeur dans [-1, 1], toujours la même pour la même entrée
    //
    // Principe : on utilise le hash d'un entier dérivé de l'entrée,
    //            puis on interpole entre deux valeurs hachées voisines
    //            avec smootherstep pour que ce soit lisse.
    //
    // Ce n'est PAS du Perlin/Simplex (qui est en 2D/3D).
    // C'est un bruit 1D simple pour varier des paramètres :
    //   - oscillation de température au cours du temps
    //   - variation de densité de minerai selon la profondeur
    //   - petites perturbations sur n'importe quel paramètre
    // ============================================================

    public static double noise1D(double x){
        // Partie entière (floor) et fraction
        long xi = (long) Math.floor(x);
        double frac = x - xi;

        // Hash des deux points voisins -> valeur dans [-1, 1]
        double a = hash1D(xi);
        double b = hash1D(xi + 1);

        // Interpolation lisse entre les deux
        double t = smootherstep(frac);
        return lerp(a, b, t);
    }

    // ============================================================
    // HASH 1D : fonction de hachage entier → double dans [-1, 1]
    //
    // Technique classique : on mélange les bits d'un long avec
    // des multiplications et XOR pour obtenir un résultat
    // qui "a l'air aléatoire" mais est 100% déterministe.
    //
    // Propriété essentielle : hash1D(42) renvoie TOUJOURS la même valeur.
    // → même seed = même monde généré
    // ============================================================

    private static double hash1D(long n){
        n = n * 6364136223846793005L + 1442695040888963407L;
        n = (n >> 16) ^ n;
        n = n * 6364136223846793005L + 1442695040888963407L;
        // Normaliser dans [-1,1]
        return (n & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL * 2.0 - 1.0;
    }

    // ============================================================
    // NOISE 1D MULTI-OCTAVE (fBm 1D)
    //
    // Même principe que le bruit fractal 2D/3D qu'on utilisera
    // pour le terrain, mais en 1D.
    //
    // On superpose plusieurs "couches" de bruit :
    //   - Octave 1 : grandes variations lentes (fréquence basse)
    //   - Octave 2 : variations moyennes (fréquence × lacunarité)
    //   - Octave 3 : petits détails (fréquence encore plus haute)
    //
    // À chaque octave :
    //   - fréquence × lacunarité (les détails sont plus fins)
    //   - amplitude × persistance (les détails ont moins d'impact)
    //
    // Paramètres :
    //   octaves     : nombre de couches (4-8 typique)
    //   persistance : 0.5 = chaque octave a moitié moins d'impact
    //   lacunarité  : 2.0 = chaque octave a 2× plus de détails
    // ============================================================

    public static double fbmNoise1D(double x, int octaves, double persistance, double lacunarite){
        double total = 0.0;
        double amplitude = 1.0;
        double frequence = 1.0;
        double maxAmplitude = 0.0;

        for (int i = 0; i < octaves; i++) {
            total += noise1D(x * frequence) * amplitude;
            maxAmplitude += amplitude;
            amplitude *= persistance;
            frequence *= lacunarite;
        }

        // Normaliser dans [-1, 1]
        return total / maxAmplitude;
    }



}
