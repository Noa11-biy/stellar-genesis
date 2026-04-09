package com.stellargenesis.core.world;

/**
 * Configuration d'un type de minerai pour la génération procédurale.
 *
 * Chaque minerai est défini par :
 *   - sa profondeur optimale (où il est le plus probable)
 *   - sa variance (sur quelle épaisseur il s'étend)
 *   - sa fréquence de bruit (taille des filons)
 *   - son seuil (rareté)
 *   - son seed offset (pour que chaque minerai ait un bruit différent)
 *
 * Ordonnés du plus RARE au plus COMMUN (le premier trouvé gagne).
 */

public class OreConfig {

    public final BlockType blockType;
    public final double optimalDepth;       // blocs sous la surface
    public final double depthVariance;      // écart-Type de la gaussienne
    public final double frequency;          // fréquence du bruit 3D (petit = gros filon)
    public final double threshold;          // seuil de placement (haut = rare)
    public final long seedOffset;            // décalage de ssed

    public OreConfig(BlockType blockType, double optimalDepth, double depthVariance,
                     double frequency, double threshold, long seedOffset){
        this.blockType = blockType;
        this.optimalDepth = optimalDepth;
        this.depthVariance = depthVariance;
        this.frequency = frequency;
        this.threshold = threshold;
        this.seedOffset = seedOffset;
    }

    /**
     * Tous les minerais, du plus rare au plus commun.
     *
     * Profondeurs inspirées de la géologie réelle :
     *   - Fer/Cuivre : croûte supérieure (20-100m)
     *   - Or/Platine : zones hydrothermales profondes (100-500m)
     *   - Uranium/Titane : très profond (200-800m)
     */
    public static final OreConfig[] ALL_ORES = {
            //                          blockType              depth  var   freq    thresh  seed
            new OreConfig(BlockType.URANIUM_ORE,               500,  150,  0.06,   0.82,   70000),
            new OreConfig(BlockType.PLATINUM_ORE,              400,  120,  0.07,   0.80,   60000),
            new OreConfig(BlockType.TITANIUM_ORE,              300,  100,  0.07,   0.78,   50000),
            new OreConfig(BlockType.GOLD_ORE,                  250,   80,  0.08,   0.75,   40000),
            new OreConfig(BlockType.LITHIUM_ORE,               150,   60,  0.09,   0.72,   35000),
            new OreConfig(BlockType.COPPER_ORE,                 60,   40,  0.10,   0.65,   20000),
            new OreConfig(BlockType.IRON_ORE,                   40,   50,  0.12,   0.58,   10000),
            new OreConfig(BlockType.COAL_ORE,                   30,   40,  0.14,   0.55,    5000),
    };

}
