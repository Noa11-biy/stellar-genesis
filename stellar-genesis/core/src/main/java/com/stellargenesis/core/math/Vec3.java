package com.stellargenesis.core.math;

/**
 * Vecteur 3D immutable pour tous les calculs spatiaux du jeu.
 *
 * POURQUOI IMMUTABLE ?
 * - Pas de bugs liés à une modification accidentelle
 * - Thread-safe gratuitement (important pour le multijoueur plus tard)
 * - Chaque opération retourne un NOUVEAU Vec3
 *
 * POURQUOI PAS jMonkeyEngine Vector3f ?
 * - On veut comprendre chaque opération mathématique
 * - Le module core ne dépend PAS du moteur 3D
 * - On pourra convertir vers Vector3f quand on branchera jME
 *
 * RAPPEL MATHÉMATIQUE :
 * Un vecteur 3D représente une direction + une magnitude dans l'espace.
 * (x, y, z) où chaque composante est une coordonnée sur un axe.
 *
 * @author Noa Moal
 */

public class Vec3 {

    // Composantes publiques et finales (immutable)
    public final double x;
    public final double y;
    public final double z;

    // Vecteurs constants réutilisables (évite de créer des objets)
    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public static final Vec3 UP = new Vec3(0, 1, 0);
    public static final Vec3 RIGHT = new Vec3(1, 0, 0);
    public static final Vec3 FORWARD = new Vec3(0, 0, 1);

    public Vec3(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // ============================================================
    // ADDITION : a + b = (ax+bx, ay+by, az+bz)
    // Utilité : déplacer une position, combiner des forces
    // ============================================================

    public  Vec3 add(Vec3 other){
        return new Vec3(this.x + other.x, this.y + other.y,this.z + other.z);
    }

    // ============================================================
    // SOUSTRACTION : a - b = (ax-bx, ay-by, az-bz)
    // Utilité : calculer la direction entre deux points
    // Exemple : direction = cible.sub(position) → vecteur qui pointe vers la cible
    // ============================================================

    public  Vec3 sub(Vec3 other){
        return new Vec3(this.x - other.x, this.y - other.y,this.z - other.z);
    }

    // ============================================================
    // MULTIPLICATION PAR SCALAIRE : s × a = (s×ax, s×ay, s×az)
    // Utilité : changer la magnitude sans changer la direction
    // Exemple : force = direction.scale(100.0) → force de 100N dans cette direction
    // ============================================================

    public Vec3 scale(double s){
        return new Vec3(this.x * s, this.y * s, this.z * s);
    }

    // ============================================================
    // PRODUIT SCALAIRE (DOT PRODUCT) : a · b = ax×bx + ay×by + az×bz
    //
    // Résultat : un NOMBRE (pas un vecteur)
    // Signification géométrique : a · b = |a| × |b| × cos(θ)
    //   - Si positif : les vecteurs pointent dans le même sens (angle < 90°)
    //   - Si zéro    : perpendiculaires (angle = 90°)
    //   - Si négatif : sens opposés (angle > 90°)
    //
    // Utilité dans le jeu :
    //   - Éclairage : intensité = max(0, lumière · normale_surface)
    //   - Détection : le joueur est-il devant ou derrière un ennemi ?
    //   - Projection : composante d'une force dans une direction
    // ============================================================

    public double dot(Vec3 other){
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    // ============================================================
    // PRODUIT VECTORIEL (CROSS PRODUCT) : a × b
    //
    // Résultat : un VECTEUR perpendiculaire à a et b
    // Formule :
    //   cx = ay×bz - az×by
    //   cy = az×bx - ax×bz
    //   cz = ax×by - ay×bx
    //
    // Propriétés :
    //   - |a × b| = |a| × |b| × sin(θ) → magnitude = aire du parallélogramme
    //   - a × b = -(b × a) → l'ordre compte ! (anticommutatif)
    //   - Si a et b sont parallèles : a × b = (0,0,0)
    //
    // Utilité dans le jeu :
    //   - Calculer la normale d'une surface (rendu 3D)
    //   - Calculer le couple (torque) d'une force
    //   - Construire un repère orthonormé à partir de 2 vecteurs
    // ============================================================

    public Vec3 cross(Vec3 other){
        return new Vec3(
                this.y * other.z - this.z * other.z,
                this.z * other.x - this.x * other.z,
                this.x * other.y - this.y * other.x
        );
    }
    // ============================================================
    // NORME (LONGUEUR) : |a| = √(ax² + ay² + az²)
    //
    // C'est le théorème de Pythagore en 3D.
    // Utilité : connaître la distance, la vitesse, la magnitude d'une force
    // ============================================================

    public double length(){
        return Math.sqrt(x * x + y * y + z * z);
    }

    // ============================================================
    // NORME AU CARRÉ : |a|² = ax² + ay² + az²
    //
    // POURQUOI ? Évite le √ qui est coûteux en calcul.
    // Quand on compare des distances, on peut comparer les carrés :
    //   dist² < seuil² est équivalent à dist < seuil
    // ============================================================

    public double lengthSquared(){
        return x * x + y * y + z * z;
    }

    // ============================================================
    // NORMALISATION : â = a / |a|
    //
    // Retourne un vecteur de longueur 1 dans la même direction.
    // Utilité : quand on veut juste la DIRECTION, pas la magnitude.
    // Exemple : direction_vers_ennemi = (ennemi - joueur).normalize()
    //
    // ATTENTION : normaliser un vecteur nul (0,0,0) est impossible
    // → on retourne ZERO pour éviter NaN
    // ============================================================

    public Vec3 normalize(){
        double len = length();
        if(len < 1e-10) return ZERO;// Sécurité pour empêcher division par zéro

        return new Vec3( x/len, y/len,z/len);
    }

    // ============================================================
    // DISTANCE entre deux points
    // distance(A, B) = |B - A|
    // ============================================================

    public double distanceTo(Vec3 other){
        return this.sub(other).length();
    }

    // ============================================================
    // INTERPOLATION LINÉAIRE (LERP)
    // lerp(a, b, t) = a + t × (b - a)    avec t ∈ [0, 1]
    //
    //   t = 0 → retourne a
    //   t = 1 → retourne b
    //   t = 0.5 → retourne le milieu
    //
    // Utilité : animations fluides, transitions de caméra,
    //           interpolation de positions réseau (multijoueur)
    // ============================================================

    public Vec3 lerp(Vec3 target, double t){
        return new Vec3(
                this.x + t * (target.x - this.x),
                this.y + t * (target.y - this.y),
                this.z + t * (target.z - this.z)
        );
    }

    @Override
    public String toString() {
        return String.format("Vec3(%.4f, %.4f, %.4f", x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if(!(obj instanceof Vec3)) return false;
        Vec3 v = (Vec3) obj;
        return Double.compare(v.x, x) == 0
                && Double.compare(v.y, y) == 0
                && Double.compare(v.z, z) == 0;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(x);
        bits = 31 * bits + Double.doubleToLongBits(y);
        bits = 31 * bits + Double.doubleToLongBits(z);
        return (int)(bits^(bits >>> 32));
    }
}
