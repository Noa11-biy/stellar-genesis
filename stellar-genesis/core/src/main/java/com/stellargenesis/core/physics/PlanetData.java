package com.stellargenesis.core.physics;

import com.stellargenesis.shared.constants.PhysicsConstants;

/**
 * Contient TOUS les paramètres physiques d'une planète.
 * Calculés UNE FOIS à la génération à partir de :
 *   - masse (kg)
 *   - rayon (m)
 *   - distance à l'étoile (m)
 *   - type spectral de l'étoile
 *   - composition atmosphérique
 *
 * Toutes les autres propriétés en DÉCOULENT par les lois de la physique.
 * Rien n'est arbitraire — si tu changes la masse, la gravité change,
 * ce qui change la pression atmosphérique, ce qui change les biomes,
 * ce qui change le gameplay.
 *
 * Chaîne de dépendances :
 *
 *   masse, rayon ──► gravité g = GM/R²
 *                        │
 *                        ├──► vitesse de libération v_esc = sqrt(2gR)
 *                        ├──► P(h) = P₀ × exp(-Mgh/RT)
 *                        └──► poids max inventaire W = mg
 *
 *   distance étoile ──► flux stellaire F = L★/(4πd²)
 *                            │
 *                            └──► T_equilibre = T★ × sqrt(R★/2d) × (1-albedo)^(1/4)
 *
 *   T_eq + P₀ + humidité ──► biomes
 *   type spectral étoile ──► couleur du ciel (diffusion Rayleigh)
 *
 * @author Noa Moal
 */
public class PlanetData {

    // === PARAMÈTRES D'ENTRÉE (choisis à la génération) ===

    private final String name;
    private final long seed;

    private final double mass;                  // kg
    private final double radius;                // m
    private final double distanceToStar;        // m (demi-axe orbital)
    private final double rotationPeriod;        // secondes (durée d'un jour)
    private final double axialTilt;             // radians (inclinaison de l'axe)
    private final double albedo;                // 0.0 à 1.0 (fraction de lumière réfléchie)
    private double rotationPeriodHours = 24.0; // par défaut comme la Terre

    // Étoile parente
    private final double starMass;             // kg
    private final double starRadius;            // m
    private final double starTemperature;       // K (température de surface)
    private final double starLuminosity;        // W

    // Atmosphère
    private final double surfacePressure;       // bar (pression au sol)
    private final double atmosMolarMass;        // kg/mol (masse molaire moyenne)
    private final double[] atmosComposition;    // fractions [N2, O2, CO2, Ar, H2O, CH4, ...]

    // === PARAMÈTRES CALCULÉS (dérivés par la physique) ===
    private final double surfaceGravity;        // m/s²
    private final double escapeVelocity;        // m/s
    private final double equilibriumTemp;       // K
    private final double surfaceTemperature;    // K (avec effet de serre)
    private final double stellarFlux;           // W/m²
    private final double orbitalPeriod;         // secondes
    private final double atmosphereScaleHeight; // m
    private final double lapseRate;             // K/m

    /**
     * Constructeur : prend les paramètres d'entrée, calcule tout le reste.
     *
     * L'ordre des calculs est important car certains dépendent d'autres :
     *   1. Gravité (dépend de masse + rayon)
     *   2. Vitesse de libération (dépend de gravité + rayon)
     *   3. Flux stellaire (dépend de luminosité étoile + distance)
     *   4. Température d'équilibre (dépend de flux + albedo)
     *   5. Température de surface (dépend de T_eq + effet de serre)
     *   6. Scale height atmosphérique (dépend de T, g, masse molaire)
     *   7. Lapse rate (dépend de g, composition atm)
     *   8. Période orbitale (dépend de masse étoile + distance)
     */
    public PlanetData(String name, long seed,
                      double mass, double radius,
                      double distanceToStar, double rotationPeriod,
                      double axialTilt, double albedo,
                      double starMass, double starRadius,
                      double starTemperature, double starLuminosity,
                      double surfacePressure, double atmosMolarMass,
                      double[] atmosComposition) {

        // Stocker les entrées
        this.name = name;
        this.seed = seed;
        this.mass = mass;
        this.radius = radius;
        this.distanceToStar = distanceToStar;
        this.rotationPeriod = rotationPeriod;
        this.axialTilt = axialTilt;
        this.albedo = albedo;
        this.starMass = starMass;
        this.starRadius = starRadius;
        this.starTemperature = starTemperature;
        this.starLuminosity = starLuminosity;
        this.surfacePressure = surfacePressure;
        this.atmosMolarMass = atmosMolarMass;
        this.atmosComposition = atmosComposition;

        // ============================================
        // CALCUL 1 : Gravité de surface
        // ============================================
        // g = G × M / R²
        //
        // C'est la loi de gravitation de Newton évaluée à la surface.
        // Une sphère uniforme attire comme si toute sa masse était au centre
        // (théorème de la coquille de Newton, démontré en 1687).
        //
        // Exemples :
        //   Terre : g = 6.674e-11 × 5.97e24 / (6.37e6)² = 9.81 m/s²
        //   Mars  : g = 6.674e-11 × 6.42e23 / (3.39e6)² = 3.72 m/s²
        //   Lune  : g = 6.674e-11 × 7.34e22 / (1.74e6)² = 1.62 m/s²
        this.surfaceGravity = PhysicsConstants.G * mass / (radius * radius);

        // ============================================
        // CALCUL 2 : Vitesse de libération
        // ============================================
        // v_esc = sqrt(2 × G × M / R) = sqrt(2 × g × R)
        //
        // C'est la vitesse minimale pour quitter la planète sans propulsion.
        // Obtenue en égalisant énergie cinétique et énergie potentielle :
        //   ½mv² = GMm/R  →  v = sqrt(2GM/R)
        //
        // Impact gameplay :
        //   - Détermine le Δv minimum pour quitter la planète en fusée
        //   - Planète massive → il faut plus de carburant pour décoller
        //   - Atmosphère dense + v_esc élevé = "piège gravitationnel"
        //
        // Exemples :
        //   Terre : 11 186 m/s
        //   Mars  :  5 027 m/s
        //   Lune  :  2 376 m/s
        this.escapeVelocity = Math.sqrt(2.0 * PhysicsConstants.G * mass / radius);
        // ============================================
        // CALCUL 3 : Flux stellaire à la distance de la planète
        // ============================================
        // F = L★ / (4π × d²)
        //
        // La luminosité de l'étoile se répartit sur une sphère de rayon d.
        // Surface de la sphère = 4πd² → flux = énergie/surface
        //
        // C'est la loi en 1/r² : doubler la distance = diviser le flux par 4.
        //
        // Exemples (avec L_soleil = 3.846e26 W) :
        //   Terre (1 UA)     : 1361 W/m²
        //   Mars  (1.52 UA)  :  589 W/m²
        //   Jupiter (5.2 UA) :   50 W/m²
        this.stellarFlux = starLuminosity
                / (4.0 * Math.PI * distanceToStar * distanceToStar);

        // ============================================
        // CALCUL 4 : Température d'équilibre
        // ============================================
        // T_eq = T★ × sqrt(R★ / (2 × d)) × (1 - albedo)^(1/4)
        //
        // C'est la température qu'aurait la planète si elle était un
        // corps noir parfait en équilibre radiatif (énergie reçue = émise).
        //
        // Dérivation :
        //   Énergie reçue   = F × π × R_p² × (1 - albedo)
        //   Énergie émise   = σ × T⁴ × 4π × R_p²
        //   Équilibre : F(1-A)πR² = σT⁴ × 4πR²
        //   → T⁴ = F(1-A) / (4σ)
        //   → T = [L★(1-A) / (16πσd²)]^(1/4)
        //   Simplification : T = T★ × sqrt(R★/2d) × (1-A)^(1/4)
        //
        // Ne prend PAS en compte l'effet de serre → T_surface sera plus haute.
        this.equilibriumTemp = starTemperature
                * Math.sqrt(starRadius / (2.0 * distanceToStar))
                * Math.pow(1.0 - albedo, 0.25);

        // ============================================
        // CALCUL 5 : Température de surface (avec effet de serre)
        // ============================================
        // T_surface = T_eq × (1 + τ_serre)^(1/4)
        //
        // τ_serre = facteur d'opacité infrarouge de l'atmosphère
        // Dépend principalement du CO₂, H₂O, CH₄ et de la pression totale.
        //
        // Modèle simplifié :
        //   τ = pression × (0.5 × fraction_CO2 + 0.3 × fraction_H2O + 0.2 × fraction_CH4)
        //   Ce n'est PAS un modèle climatique réel, mais une approximation
        //   qui donne des résultats plausibles pour un jeu.
        //
        // Exemples réels :
        //   Terre : T_eq=255K, T_surface=288K  (τ≈0.78)
        //   Vénus : T_eq=227K, T_surface=737K  (τ≈150 → emballement)
        //   Mars  : T_eq=210K, T_surface=218K  (τ≈0.07 → quasi rien)
        double greenhouseFactor = computeGreenhouseFactor();
        this.surfaceTemperature = equilibriumTemp * Math.pow(1.0 + greenhouseFactor, 0.25);

        // ============================================
        // CALCUL 6 : Hauteur d'échelle atmosphérique
        // ============================================
        // H = R × T / (M × g)
        //
        // R = 8.314 J/(mol·K) (constante gaz parfaits)
        // M = masse molaire atmosphère (kg/mol)
        // g = gravité (m/s²)
        // T = température de surface (K)
        //
        // H représente l'altitude à laquelle la pression tombe à 1/e (≈37%)
        // de sa valeur au sol. C'est "l'épaisseur caractéristique" de l'atmosphère.
        //
        // Exemples :
        //   Terre : H = 8.314 × 288 / (0.029 × 9.81) = 8 400 m
        //   Mars  : H = 8.314 × 218 / (0.044 × 3.72) = 11 100 m
        //   (Mars a H plus grand malgré moins de pression car g faible et M élevé)
        //
        // Usage en jeu : P(h) = P₀ × exp(-h / H)
        if (surfaceGravity > 0 && atmosMolarMass > 0) {
            this.atmosphereScaleHeight = PhysicsConstants.R_GAZ * surfaceTemperature
                    / (atmosMolarMass * surfaceGravity);
        } else {
            this.atmosphereScaleHeight = 0;  // pas d'atmosphère
        }

        // ============================================
        // CALCUL 7 : Lapse rate (gradient thermique vertical)
        // ============================================
        // Γ = g / c_p
        //
        // Γ = gradient adiabatique sec (K/m)
        // g = gravité (m/s²)
        // c_p = chaleur spécifique à pression constante (J/(kg·K))
        //
        // Pour l'air terrestre : c_p ≈ 1005 J/(kg·K)
        //   Γ = 9.81 / 1005 = 0.00976 K/m ≈ 9.8 K/km
        //
        // En réalité le lapse rate moyen est ~6.5 K/km car l'humidité
        // libère de la chaleur latente en condensant. On utilise un
        // facteur 0.65 pour approximer :
        //   Γ_effectif = 0.65 × g / c_p
        //
        // Usage : T(h) = T_surface - Γ × h
        double cp = estimateCp();
        if (cp > 0) {
            this.lapseRate = 0.65 * surfaceGravity / cp;
        } else {
            this.lapseRate = 0;
        }

        // ============================================
        // CALCUL 8 : Période orbitale (3e loi de Kepler)
        // ============================================
        // T² = (4π² / GM★) × a³
        //
        // T = période orbitale (s)
        // G = constante gravitationnelle
        // M★ = masse de l'étoile (kg)
        // a = demi-grand axe = distanceToStar (m)
        //
        // Dérivation :
        //   Force centripète = force gravitationnelle
        //   m × v²/r = G × M★ × m / r²
        //   v = 2πr / T (vitesse orbitale circulaire)
        //   → T² = 4π²r³ / (GM★)
        //
        // Terre : T = 365.25 jours ≈ 3.156e7 s
        this.orbitalPeriod = 2.0 * Math.PI
                * Math.sqrt(Math.pow(distanceToStar, 3)
                / (PhysicsConstants.G * starMass));
    }

    // === MÉTHODES DE CALCUL INTERNES ===

    /**
     * Facteur d'effet de serre simplifié.
     *
     * Indices dans atmosComposition[] :
     *   0 = N₂ (azote)        — inerte pour l'effet de serre
     *   1 = O₂ (oxygène)      — quasi inerte
     *   2 = CO₂ (dioxyde C)   — gaz à effet de serre majeur
     *   3 = Ar (argon)         — inerte
     *   4 = H₂O (vapeur eau)  — gaz à effet de serre puissant
     *   5 = CH₄ (méthane)     — gaz à effet de serre très puissant
     */
    private double computeGreenhouseFactor(){
        if (atmosComposition == null || atmosComposition.length < 6) {
            return 0.0;
        }

        double co2 = atmosComposition[2];
        double h2o = atmosComposition[4];
        double ch4 = atmosComposition[5];

        // Pondération empirique (simplifiée)
        // En réalité c'est bien plus complexe (bandes d'absorption IR, rétroactions...)
        double tau = surfacePressure * (0.5 * co2 + 0.3 * h2o + 0.2 * ch4);

        // Plafonner pour éviter des T absurdes (emballement vénusien)
        return Math.min(tau, 150.0);
    }


    /**
     * Estime c_p moyen de l'atmosphère à partir de sa composition.
     *
     * c_p de chaque gaz (J/(kg·K)) :
     *   N₂  = 1040
     *   O₂  = 919
     *   CO₂ = 844
     *   Ar  = 520
     *   H₂O = 1864
     *   CH₄ = 2226
     *
     * c_p_moyen = Σ (fraction_i × cp_i)
     * (moyenne pondérée par fraction massique)
     */
    private double estimateCp(){
        if (atmosComposition == null || atmosComposition.length < 6) {
            return 1000.0;
        }

        double[] cpValues = { 1040.0, 919.0, 844.0, 520.0, 1864.0, 2226.0 };
        double cpMean = 0.0;

        for (int i = 0; i < Math.min(atmosComposition.length, cpValues.length); i++) {
            cpMean += atmosComposition[i] * cpValues[i];
        }

        return cpMean > 0 ? cpMean : 1000.0;
    }

    // === MÉTHODES DE REQUÊTE PHYSIQUE ===
    /**
     * Pression atmosphérique à une altitude h (mètres).
     *
     * P(h) = P₀ × exp(-h / H)
     *
     * Forme simplifiée de la formule barométrique complète :
     *   P(h) = P₀ × exp(-M × g × h / (R × T))
     *
     * Comme H = RT/(Mg), les deux formes sont identiques.
     * On utilise H pré-calculé pour éviter de recalculer à chaque appel.
     */
    public double getPressureAltitude(double altitude){
        if (atmosphereScaleHeight <= 0) return 0.0;
        return surfacePressure * Math.exp(-altitude / atmosphereScaleHeight);
    }

    /**
     * Température à une altitude h (mètres).
     *
     * T(h) = T_surface - Γ × h
     *
     * Plafonnée à un minimum de 2.7K (température du fond cosmique).
     * En réalité la stratosphère se réchauffe, mais on ignore ça pour le jeu.
     */
    public double getTemperatureAtAltitude(double altitude){
        double temp = surfaceTemperature - lapseRate * altitude;
        return Math.max(temp, 2.7);
    }

    // === GETTERS ===

    public String getName() { return name; }
    public long getSeed() { return seed; }
    public double getMass() { return mass; }
    public double getRadius() { return radius; }
    public double getDistanceToStar() { return distanceToStar; }
    public double getRotationPeriod() { return rotationPeriod; }
    public double getAxialTilt() { return axialTilt; }
    public double getAlbedo() { return albedo; }
    public double getStarMass() { return starMass; }
    public double getStarRadius() { return starRadius; }
    public double getStarTemperature() { return starTemperature; }
    public double getStarLuminosity() { return starLuminosity; }
    public double getSurfacePressure() { return surfacePressure; }
    public double getAtmosMolarMass() { return atmosMolarMass; }
    public double[] getAtmosComposition() { return atmosComposition; }
    public double getSurfaceGravity() { return surfaceGravity; }
    public double getEscapeVelocity() { return escapeVelocity; }
    public double getEquilibriumTemp() { return equilibriumTemp; }
    public double getSurfaceTemperature() { return surfaceTemperature; }
    public double getStellarFlux() { return stellarFlux; }
    public double getOrbitalPeriod() { return orbitalPeriod; }
    public double getAtmosphereScaleHeight() { return atmosphereScaleHeight; }
    public double getLapseRate() { return lapseRate; }
    public double getRotationPeriodHours() { return rotationPeriodHours; }
    public void setRotationPeriodHours(double h) { this.rotationPeriodHours = h; }


    @Override
    public String toString() {
        return String.format(
                "=== %s ===\n"
                        + "Masse: %.3e kg | Rayon: %.0f km\n"
                        + "Gravité: %.2f m/s² (%.2f g)\n"
                        + "V_escape: %.0f m/s\n"
                        + "T_eq: %.1f K | T_surface: %.1f K (%.1f °C)\n"
                        + "P_surface: %.3f bar\n"
                        + "Flux stellaire: %.1f W/m²\n"
                        + "Période orbitale: %.1f jours\n"
                        + "Rotation: %.1f heures\n"
                        + "Scale height: %.0f m\n"
                        + "Lapse rate: %.2f K/km",
                name,
                mass, radius / 1000.0,
                surfaceGravity, surfaceGravity / 9.81,
                escapeVelocity,
                equilibriumTemp, surfaceTemperature, surfaceTemperature - 273.15,
                surfacePressure,
                stellarFlux,
                orbitalPeriod / 86400.0,
                rotationPeriod / 3600.0,
                atmosphereScaleHeight,
                lapseRate * 1000.0
        );
    }

}
