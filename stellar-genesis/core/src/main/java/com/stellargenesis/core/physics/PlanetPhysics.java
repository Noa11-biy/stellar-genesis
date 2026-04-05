package com.stellargenesis.core.physics;

import com.stellargenesis.shared.constants.PhysicsConstants;

/**
 * Calculs physiques planétaires.
 *
 * Cette classe contient les formules fondamentales qui définissent
 * les conditions physiques sur une planète. Chaque méthode correspond
 * à une équation de physique réelle, documentée et expliquée.
 *
 * TOUTES les méthodes sont statiques et pures (pas d'état interne).
 * Entrées → Calcul → Sortie. Aucun effet de bord.
 *
 * @author Noa Moal
 */

public final class PlanetPhysics {

    private PlanetPhysics(){
        throw new AssertionError("Classe utilitaire");
    }

    // ============================================================
    // GRAVITÉ DE SURFACE
    //
    // g = G × M / R²
    //
    // C'est la formule de Newton appliquée à la surface d'une sphère.
    // - G : constante gravitationnelle (6.674e-11)
    // - M : masse de la planète (kg)
    // - R : rayon de la planète (m)
    //
    // Intuition :
    //   Plus la planète est massive → g augmente (proportionnel à M)
    //   Plus la planète est grande  → g diminue (inversement proportionnel à R²)
    //
    // Exemples :
    //   Terre : g = 6.674e-11 × 5.972e24 / (6.371e6)² ≈ 9.82 m/s²
    //   Mars  : g ≈ 3.72 m/s² (38% de la Terre)
    //   Lune  : g ≈ 1.62 m/s² (17% de la Terre)
    //
    // Dans le jeu : détermine la hauteur de saut, la vitesse de chute,
    //               le poids max de l'inventaire (W_max / g)
    // ============================================================

    public static double surfaceGravity(double planetMass, double planetRadius){
        if(planetRadius <= 0) {
            throw new IllegalArgumentException("Rayon doit être > 0");
        }
        return PhysicsConstants.G * planetMass / (planetRadius * planetRadius);
    }

    // ============================================================
    // VITESSE DE LIBÉRATION
    //
    // v_esc = √(2 × G × M / R)
    //
    // C'est la vitesse minimale pour quitter définitivement
    // le champ gravitationnel d'un corps céleste (sans propulsion continue).
    //
    // Dérivation simplifiée :
    //   Énergie cinétique = Énergie potentielle gravitationnelle
    //   ½mv² = GMm/R
    //   v = √(2GM/R)
    //
    // Exemples :
    //   Terre : 11.2 km/s (il faut aller TRÈS vite pour quitter la Terre)
    //   Lune  : 2.4 km/s (beaucoup plus facile)
    //   Jupiter : 59.5 km/s (quasi impossible avec des fusées chimiques)
    //
    // Dans le jeu : détermine le Δv nécessaire pour décoller (Phase 8)
    //               et le type de fusée requis
    // ============================================================

    public static double escapeVelocity(double planetMass, double planetRadius){
        if (planetRadius <= 0){
            throw new IllegalArgumentException("Rayon doit être > 0");
        }

        return Math.sqrt(2.0 * PhysicsConstants.G * planetMass / planetRadius);
    }

    // ============================================================
    // TEMPÉRATURE D'ÉQUILIBRE
    //
    // T_eq = T_star × √(R_star / (2 × d)) × (1 - albedo)^(1/4)
    //
    // C'est la température à laquelle une planète émet autant d'énergie
    // qu'elle en reçoit de son étoile (équilibre radiatif).
    //
    // Paramètres :
    //   T_star  : température de surface de l'étoile (K)
    //   R_star  : rayon de l'étoile (m)
    //   d       : distance planète-étoile (m)
    //   albedo  : fraction de lumière réfléchie (0 = absorbe tout, 1 = réfléchit tout)
    //             Terre ≈ 0.30, Lune ≈ 0.12, Vénus ≈ 0.77
    //
    // Dérivation :
    //   Puissance reçue = L_star × (1 - albedo) × πR²_planète / (4πd²)
    //   Puissance émise  = 4πR²_planète × σ × T⁴_eq
    //   En égalant : T_eq = T_star × √(R_star / (2d)) × (1-a)^(1/4)
    //
    // Exemples :
    //   Terre : T_eq ≈ 255 K (-18°C) — l'effet de serre ajoute ~33K
    //   Mars  : T_eq ≈ 210 K (-63°C)
    //   Vénus : T_eq ≈ 230 K mais T_réelle ≈ 740 K (effet de serre MASSIF)
    //
    // Dans le jeu : détermine le biome de base, la possibilité d'eau liquide,
    //               et les dangers thermiques pour le joueur
    // ============================================================

    public static double equilibriumTemperature(double starTemp, double starRadius, double distance, double albedo){
        if (distance <= 0){
            throw new IllegalArgumentException("La distance doit être > 0");
        }
        if(albedo < 0 || albedo >= 1){
            throw new IllegalArgumentException("Albedo doit être [0, 1[");
        }

        return starTemp * Math.sqrt(starRadius / (2.0 * distance)) * Math.pow(1.0 - albedo, 0.25);
    }

    // ============================================================
    // PRESSION ATMOSPHÉRIQUE EN FONCTION DE L'ALTITUDE
    //
    // P(h) = P₀ × exp(-M × g × h / (R_gaz × T))
    //
    // C'est la formule barométrique (atmosphère isotherme simplifiée).
    // La pression diminue exponentiellement avec l'altitude.
    //
    // Paramètres :
    //   P0          : pression au sol (Pa). Terre = 101325 Pa = 1 atm
    //   h           : altitude au-dessus du sol (m)
    //   molarMass   : masse molaire de l'atmosphère (kg/mol). Terre ≈ 0.029 kg/mol
    //   g           : gravité de surface (m/s²)
    //   temperature : température de l'atmosphère (K)
    //
    // Pourquoi exponentielle ?
    //   Chaque couche d'air est comprimée par le poids de toutes les couches au-dessus.
    //   Plus on monte, moins il y a d'air au-dessus → moins de pression.
    //   Le taux de diminution est proportionnel à la pression elle-même → exponentielle.
    //
    // Exemples (Terre) :
    //   h = 0m     : P = 101325 Pa (niveau de la mer)
    //   h = 5500m  : P ≈ 50000 Pa (moitié — à peu près le sommet du Mont Blanc)
    //   h = 8848m  : P ≈ 33000 Pa (Everest — 1/3 de la pression au sol)
    //   h = 100km  : P ≈ 0 Pa (bord de l'espace)
    //
    // Dans le jeu : détermine si le joueur peut respirer à une altitude donnée,
    //               l'efficacité des ailes/parachutes, la traînée aérodynamique
    // ============================================================

    public static double atmosphericPressure(double P0, double h, double molarMass, double g, double temperature){
        if(temperature <= 0){
            throw new IllegalArgumentException("Température doit être > 0K");
        }

        double exponent = -molarMass * g * h /
                (PhysicsConstants.R_GAZ * temperature);
        return P0 * Math.exp(exponent);
    }

    // ============================================================
    // VITESSE ORBITALE CIRCULAIRE
    //
    // v_orb = √(G × M / r)
    //
    // C'est la vitesse nécessaire pour rester en orbite circulaire
    // à une distance r du centre d'un corps de masse M.
    //
    // Intuition :
    //   La gravité tire l'objet vers le centre.
    //   La vitesse orbitale crée une force centrifuge qui compense exactement.
    //   Résultat : l'objet "tombe" en permanence mais rate toujours la surface.
    //
    // Relation avec v_esc : v_orb = v_esc / √2
    //   → L'orbite circulaire demande ~70% de la vitesse de libération
    //
    // Exemples :
    //   ISS (400 km altitude) : v_orb ≈ 7.67 km/s
    //   Orbite géostationnaire : v_orb ≈ 3.07 km/s
    //
    // Dans le jeu : calculer la vitesse pour se mettre en orbite (Phase 8)
    // ============================================================

    public static double orbitalVelocity(double centralMass, double orbitalRadius){
        if (orbitalRadius <= 0){
            throw new IllegalArgumentException("Rayon oribital doit être > 0");
        }

        return Math.sqrt(PhysicsConstants.G * centralMass / orbitalRadius);
    }

}
