package com.stellargenesis.core.world;

/**
 * Système de biomes déterminé par les conditions physiques locales.
 *
 * Chaque point du monde a une température T(h) et une pression P(h)
 * qui varient avec l'altitude h :
 *
 *   T(h) = T_surface - lapse_rate × h
 *     lapse_rate terrestre ≈ 6.5 K/km (l'air refroidit en montant)
 *     Pourquoi ? L'air qui monte se détend (moins de pression)
 *     → détente adiabatique → refroidissement
 *     Valeur exacte dépend de la composition atmosphérique
 *
 *   P(h) = P₀ × exp(-M × g × h / (R × T))
 *     Formule barométrique (atmosphère isotherme simplifiée)
 *     P₀ = pression au sol (bar)
 *     M  = masse molaire de l'atmosphère (kg/mol)
 *     g  = gravité locale (m/s²)
 *     R  = 8.314 J/(mol·K) (constante des gaz parfaits)
 *     T  = température locale (K)
 *
 * L'humidité est simulée par un bruit de Simplex indépendant
 * qui représente la proximité d'eau / l'activité géologique locale.
 *
 * Le biome est choisi par SCORE : on calcule la distance entre
 * les conditions locales (T, P, humidité) et la plage idéale de
 * chaque biome. Le biome avec le score le plus bas gagne.
 *
 * Avantage du score continu vs if/else :
 *   - Pas de frontières brutales artificielles
 *   - Ajouter un biome = ajouter une entrée, rien à modifier
 *   - Pondération possible (certains biomes plus fréquents)
 *
 * @author Noa Moal
 */

public enum BiomeType {

    // nom, Tmin(K), Tmax(K), Pmin(bar), Pmax(bar), humMin, humMax,
    // surfaceBlock, subSurfaceBlock, fillerBlock

    ROCKY_PLAINS(
            "Plaine rocheuse",
            250.0, 310.0,       // T : tempéré
            0.7, 1.2,           // P : pression terrestre basse
            0.1, 0.4,           // humidité modérée
            BlockType.GRASS,
            BlockType.DIRT,
            BlockType.STONE
    ),

    DESERT(
            "Désert de sable",
            320.0, 450.0,       // T : chaud à très chaud
            0.8, 1.5,           // P : modérée
            0.0, 0.05,          // quasi aucune humidité
            BlockType.SAND,
            BlockType.SANDSTONE,
            BlockType.STONE
    ),

    RED_DESERT(
            "Désert rouge",
            260.0, 340.0,       // T : frais à chaud (type Mars)
            0.01, 0.5,          // P : très basse (atmosphère ténue)
            0.0, 0.02,          // quasi sec
            BlockType.RED_SAND,
            BlockType.SANDSTONE,
            BlockType.BASALT
    ),

    FROZEN_TUNDRA(
            "Toundra glaciale",
            150.0, 240.0,       // T : très froid
            0.5, 1.0,           // P : modérée
            0.3, 0.6,           // humidité moyenne (neige)
            BlockType.SNOW,
            BlockType.FROZEN_SOIL,
            BlockType.STONE
    ),

    ICE_FIELDS(
            "Champs de glace",
            80.0, 170.0,        // T : glacial (lune de Jupiter)
            0.0, 0.3,           // P : quasi vide
            0.4, 0.8,           // glace abondante
            BlockType.ICE,
            BlockType.ICE,
            BlockType.STONE
    ),

    VOLCANIC(
            "Zone volcanique",
            350.0, 600.0,       // T : très chaud (activité géothermique)
            1.5, 5.0,           // P : haute (gaz volcaniques denses)
            0.7, 1.0,           // vapeur d'eau abondante
            BlockType.VOLCANIC_ASH,
            BlockType.BASALT,
            BlockType.GRANITE
    ),

    SALT_FLATS(
            "Plaine de sel",
            260.0, 350.0,       // T : variable
            0.8, 1.2,           // P : terrestre
            0.0, 0.02,          // extrêmement sec
            BlockType.SALT_FLAT,
            BlockType.CLAY,
            BlockType.SANDSTONE
    ),

    HIGH_MOUNTAINS(
            "Haute montagne",
            180.0, 250.0,       // T : froid (altitude)
            0.1, 0.4,           // P : basse (altitude)
            0.2, 0.5,           // modéré
            BlockType.GRAVEL,
            BlockType.GRANITE,
            BlockType.GRANITE
    ),

    SWAMP(
            "Marécage",
            280.0, 310.0,       // T : tempéré chaud
            1.0, 1.5,           // P : terrestre
            0.8, 1.0,           // très humide
            BlockType.CLAY,
            BlockType.DIRT,
            BlockType.STONE
    ),

    LUNAR_REGOLITH(
            "Régolithe lunaire",
            100.0, 400.0,       // T : extrême (pas d'atmosphère → jour/nuit)
            0.0, 0.001,         // P : quasi vide
            0.0, 0.0,           // aucune humidité
            BlockType.REGOLITH,
            BlockType.GRAVEL,
            BlockType.BASALT
    );

    // === CHAMPS ===

    private final String name;
    private final double tempMin;        // K
    private final double tempMax;        // K
    private final double pressureMin;    // bar
    private final double pressureMax;    // bar
    private final double humidityMin;    // 0.0 à 1.0
    private final double humidityMax;    // 0.0 à 1.0
    private final BlockType surfaceBlock;
    private final BlockType subSurfaceBlock;
    private final BlockType fillerBlock;

    BiomeType(String name, double tempMin, double tempMax,
              double pressureMin, double pressureMax,
              double humidityMin, double humidityMax,
              BlockType surfaceBlock, BlockType subSurfaceBlock,
              BlockType fillerBlock) {
        this.name = name;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.pressureMin = pressureMin;
        this.pressureMax = pressureMax;
        this.humidityMin = humidityMin;
        this.humidityMax = humidityMax;
        this.surfaceBlock = surfaceBlock;
        this.subSurfaceBlock = subSurfaceBlock;
        this.fillerBlock = fillerBlock;

    }

    // === SÉLECTION DU BIOME ===

    /**
     * Sélectionne le biome le mieux adapté aux conditions locales.
     *
     * Algorithme :
     *   1. Pour chaque biome, calculer un "score de distance"
     *      = combien les conditions locales s'éloignent de la plage idéale
     *   2. Le biome avec le score le plus BAS est le meilleur
     *   3. Si les conditions sont DANS la plage → score = 0 (parfait)
     *
     * C'est une recherche du plus proche voisin dans l'espace (T, P, H).
     *
     * Complexité : O(n) avec n = nombre de biomes (10 ici → négligeable)
     * Appelé une fois par colonne (x,z) à la génération, pas chaque frame.
     *
     * @param temperature  Température locale en Kelvin
     * @param pressure     Pression locale en bar
     * @param humidity     Humidité locale [0.0 - 1.0]
     * @return Le biome le mieux adapté
     */
    public static BiomeType select(double temperature, double pressure, double humidity){
        BiomeType best = ROCKY_PLAINS; // fallback par défaut
        double bestScore = Double.MAX_VALUE;

        for (BiomeType biome : values()){
            double score = biome.computeScore(temperature, pressure, humidity);
            if (score < bestScore){
                bestScore = score;
                best = biome;
            }
        }
        return best;
    }

    /**
     * Score de distance entre les conditions locales et la plage idéale du biome.
     *
     * Pour chaque paramètre (T, P, H) :
     *   - Si la valeur est dans [min, max] → contribution = 0
     *   - Si la valeur est hors plage → contribution = (écart / largeur)²
     *
     * Pourquoi diviser par la largeur de la plage (normalisation) ?
     *   T varie de 80 à 600 K    (range 520)
     *   P varie de 0 à 5 bar     (range 5)
     *   H varie de 0 à 1         (range 1)
     *
     *   Sans normalisation : un écart de 10K en T compte autant
     *   qu'un écart de 10 bar en P, alors que 10 bar est ÉNORME
     *   et 10K est faible.
     *
     *   Avec normalisation : chaque paramètre pèse équitablement.
     *
     * Pourquoi l'écart au carré ?
     *   → Pénalise plus les gros écarts que les petits
     *   → Un biome "un peu hors plage" est préféré à un biome "très hors plage"
     *   → C'est le même principe que la régression par moindres carrés
     *
     * Exemple concret :
     *   Conditions locales : T=200K, P=0.05 bar, H=0.01
     *
     *   RED_DESERT (260-340K, 0.01-0.5 bar, 0-0.02) :
     *     score_T = ((260-200)/80)² = (60/80)² = 0.5625
     *     score_P = 0 (0.05 est dans [0.01, 0.5])
     *     score_H = 0 (0.01 est dans [0, 0.02])
     *     total = 0.5625
     *
     *   ICE_FIELDS (80-170K, 0-0.3 bar, 0.4-0.8) :
     *     score_T = ((180-170)/90)² = ... oh wait 200 > 170
     *     score_T = ((200-170)/90)² = (30/90)² = 0.111
     *     score_P = 0
     *     score_H = ((0.4-0.01)/0.4)² = 0.95² = 0.9025
     *     total = 1.0136
     *
     *   → RED_DESERT gagne (score 0.56 < 1.01) ✓
     */
    private double computeScore(double temp, double press, double humid){
        double score = 0.0;

        // --- Température ---
        double tRange = tempMax - tempMin;
        if (tRange <= 0) tRange = 1.0; // sécurité division par zéro

        if (temp < tempMin){
            double ecart = (tempMin - temp) / tRange;
            score += ecart * ecart;
        } else if (temp > tempMax) {
            double ecart = (temp - tempMax) / tRange;
            score += ecart * ecart;
        }
        // Si temp est dans [tempMin, tempMax] → contribution = 0

        // --- Pression ---
        double pRange = pressureMax - pressureMin;
        if (pRange <= 0) pRange = 0.01;

        if (press < pressureMin) {
            double ecart = (pressureMin - press) / pRange;
            score += ecart * ecart;
        } else if (press > pressureMax) {
            double ecart = (press - pressureMax) / pRange;
            score += ecart * ecart;
        }

        // --- Humidité ---
        double hRange = humidityMax - humidityMin;
        if (hRange <= 0) hRange = 0.01;

        if (humid < humidityMin) {
            double ecart = (humidityMin - humid) / hRange;
            score += ecart * ecart;
        } else if (humid > humidityMax) {
            double ecart = (humid - humidityMax) / hRange;
            score += ecart * ecart;
        }

        return score;
    }

    // === GETTERS ===

    public String getName() { return name; }
    public double getTempMin() { return tempMin; }
    public double getTempMax() { return tempMax; }
    public double getPressureMin() { return pressureMin; }
    public double getPressureMax() { return pressureMax; }
    public double getHumidityMin() { return humidityMin; }
    public double getHumidityMax() { return humidityMax; }
    public BlockType getSurfaceBlock() { return surfaceBlock; }
    public BlockType getSubSurfaceBlock() { return subSurfaceBlock; }
    public BlockType getFillerBlock() { return fillerBlock; }


    @Override
    public String toString() {
        return name + "[T:" + tempMin + "-" + tempMax + "K" + " P:" + pressureMin + "-" + pressureMax + "bar]";
    }
}
