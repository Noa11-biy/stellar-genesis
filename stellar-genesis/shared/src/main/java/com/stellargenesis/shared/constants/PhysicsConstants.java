package com.stellargenesis.shared.constants;

/**
 * Constantes fondamentales de la physique utilisées dans tout Stellar Genesis
 *
 * POURQUOI UNE CLASSE DÉDIÉE ?
 * -----------------------------
 * 1. Lisibilité : dans une formule "PhysiquesConstants.G" est plus clair qu "6.674e-11"
 *
 * 2. Documentation : chaque constante a sa source, ses unités, son contexte.
 *
 * CONVENTION :
 * - Toutes les valeurs sont en unités SI (mètres, kilogrammes, seconde, Kelvin)
 * - Source : CODATA 2018 (NSIT) sauf mention contraire
 * - Les constantes sont final static -> compilées en constantes par le JIT,
 *  donc AUCUN coût de performance.
 *
 * CETTE classe ne doit jamais être instanciée. Elle ne contient que des constantes.
 * On évitera l'erreur avec un constructeur privé
 *
 * @author Noa Moal
 *
 */

public final class PhysicsConstants {

    private PhysicsConstants(){
        throw new AssertionError("PhysicsConstants ne doit pas être instancié");
    }

    // ================================================================
    // CONSTANTES GRAVITATIONNELLES
    // ================================================================

    /**
     * Constante gravitationnelle universelle.
     * Unité : N·m²·kg⁻² (Newton mètre-carré par kilogramme-carré)
     *
     * C'est LA constante qui relie masse et gravité dans l'équation de Newton :
     *   F = G × m₁ × m₂ / r²
     *
     * Elle apparaît dans :
     * - Gravité de surface : g = G × M / R²
     * - Vitesse de libération : v_esc = √(2GM/R)
     * - Vitesse orbitale : v_orb = √(GM/r)
     * - Période orbitale (Kepler) : T = 2π × √(a³ / GM)
     *
     * Valeur petite (10⁻¹¹) → la gravité est la plus faible des 4 forces fondamentales.
     * Il faut des masses ÉNORMES (planètes, étoiles) pour la ressentir.
     */
    public static final double G = 6.67430e-11; // N·m²·kg⁻²

    /**
     * Gravité de surface terrestre standard.
     * Unité : m·s⁻² (mètres par seconde au carré)
     *
     * Utilisée comme référence pour normaliser les effets de gravité sur le joueur.
     * Exemple : sur une planète où g = 4.9 m/s², le ratio g/G_EARTH = 0.5
     * -> le joueur peut porter 2× plus de masse (même poids ressenti).
     */
    public static final double G_EARTH =  9.80665; // m·s⁻²

    // ==============================================================
    //  CONSTANTES THERMODYNAMIQUES
    // ==============================================================

    /**
     * Constante de Stefan-Boltzmann.
     * Unité : W·m⁻²·K⁻⁴
     *
     * Relie la puissance rayonnée par un corps noir à sa température :
     *   P = σ × A × T⁴
     *
     * Utilisée pour :
     * - Luminosité stellaire : L = 4π × R² × σ × T⁴
     * - Température d'équilibre planétaire (bilan radiatif)
     * - Pertes thermiques des fonderies et réacteurs
     *
     * Le T⁴ est crucial : doubler la température multiplie l'énergie rayonnée par 16.
     * C'est pour ça que les étoiles bleues (chaudes) sont bien plus lumineuses
     * que les naines rouges (froides).
     */
    public static final double STEFAN_BLOTZMANN = 5.67037e-8; // W·m⁻²·K⁻⁴

    /**
     * Constante de Boltzmann.
     * Unité : J·K⁻¹ (joules par Kelvin)
     *
     * Relie l'énergie thermique microscopique à la température macroscopique :
     *   E_thermique_par_particule = (3/2) × k_B × T
     *
     * Utilisée dans :
     * - Pression barométrique : P(h) = P₀ × exp(-Mgh / RT) où R = k_B × N_A
     * - Vitesse thermique des gaz : v_th = √(3 k_B T / m_molécule)
     * - Distribution de Maxwell-Boltzmann pour l'échappement atmosphérique
     */
    public static final double K_BLOTZMANN = 1.380649e-23; // J·K⁻¹

    /**
     * Constante des gaz parfaits.
     * Unité : J·mol⁻¹·K⁻¹
     *
     * C'est k_B × N_A (Boltzmann × Avogadro).
     * Relie pression, volume, température et quantité de matière :
     *   PV = nRT
     *
     * Utilisée massivement pour :
     * - L'atmosphère : P(h) = P₀ × exp(-M_air × g × h / (R_GAZ × T))
     * - Les processus industriels (gaz dans les fonderies, vapeur dans les turbines)
     */
    public static final double R_GAZ = 8.31446; // J·mol⁻¹·K⁻¹


    // ==============================================================
    //  CONSTANTES ÉLECTROMAGNÉTIQUES
    // ===============================================================


    /**
     * Vitesse de la lumière dans le vide.
     * Unité : m·s⁻¹
     *
     * Utilisée pour :
     * - Rayon de Schwarzschild : Rs = 2GM/c² (Phase 14 — trou noir)
     * - Énergie de masse : E = mc² (Phase 13 — étoile à neutrons)
     * - Limite de vitesse pour le vol spatial
     * - Délai de communication interplanétaire (immersion)
     */
    public static final double C_LIGHT = 2.99792e8; // m·s⁻¹

    // ==============================================================
    //  CONSTANTES ASTRONOMIQUES DE RÉFÉRENCE
    // ==============================================================

    /**
     * Masse du Soleil. Référence pour exprimer les masses stellaires.
     * "Une étoile de 2 M☉" = une étoile de 2 × SOLAR_MASS kg.
     */
    public static final double SOLAR_MASS = 1.989e30; // kg

    /**
     * Rayon Soleil
     */
    public static final double SOLAR_RADIUS = 6.957e8; // m

    /**
     * Luminosité du Soleil — puissance totale rayonnée.
     * Utilisée pour calculer le flux reçu par une planète :
     *   F = L / (4π × d²)
     * où d est la distance planète-étoile.
     */
    public static final double SOLAR_LUMINOSITY = 3.846e26; // W

    /**
     * Température effective du Soleil.
     * Utilisée pour le calcul de la température d'équilibre planétaire.
     */
    public static final double SOLAR_TEMPERATURE = 5778.0; // K

    /**
     * Masse de la Terre. Référence pour les planètes.
     */
    public static final double EARTH_MASS = 5.972e24; // kg

    /**
     * Rayon moyen de la Terre.
     */
    public static final double EARTH_RADIUS = 6.371e6; // m

    /**
     * Unité astronomique — distance Terre-Soleil moyenne.
     * Unité naturelle pour les distances dans un système stellaire.
     */
    public static final double AU = 1.496e11; // m

    /**
     * Nombre d'Avogadro — nombre de particules dans une mole.
     * Utilisé avec R_GAZ et K_BOLTZMANN (R = k_B × N_A).
     */
    public static final double AVOGADRO = 6.022e23; // mol⁻¹

    /**
     * Masse molaire de l'air terrestre (approximation).
     * Utilisée comme référence. Chaque planète aura sa propre masse molaire
     * atmosphérique calculée à partir de sa composition.
     */
    public static final double MOLAR_MASS_AIR_EARTH = 0.02896; // kg·mol⁻¹

}
