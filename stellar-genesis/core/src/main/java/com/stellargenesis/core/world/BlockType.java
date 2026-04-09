package com.stellargenesis.core.world;

/**
 * Chaque bloc du monde avec ses propriétés physiques.
 *
 * Dureté : échelle de Mohs (1=talc, 10=diamant)
 * Densité : kg/m³ réels
 * Masse par bloc : densité × volume (1m³)
 *
 * Le temps de minage dépend de la dureté :
 *   t_mine = (dureté / outil_efficacité)² × facteur_gravité
 *   facteur_gravité = sqrt(g_local / 9.81)
 *
 * Ajouts par rapport à la version initiale :
 *   - transparent : pour le culling des faces (rendu)
 *   - dropId      : ce que le bloc donne quand miné
 *   - Blocs de surface par biome (herbe, neige, sable rouge...)
 *   - Blocs construits par le joueur (fer, acier, verre...)
 *   - Lookup table BY_ID[] au lieu de boucle for
 *
 * @author Noa Moal
 */
public enum BlockType {

    // === AIR & FLUIDES ===
    // id, nom, dureté, densité, minable, transparent, dropId
    AIR          (0,   "Air",              0.0,    0.0,    false, true,  0),
    WATER        (1,   "Eau",              0.0,    1000.0, false, true,  0),
    LAVA         (2,   "Lave",             0.0,    2600.0, false, true,  0),

    // === TERRAIN DE BASE ===
    STONE        (10,  "Pierre",           6.0,    2700.0, true,  false, 10),
    DIRT         (11,  "Terre",            1.5,    1500.0, true,  false, 11),
    SAND         (12,  "Sable",            1.0,    1600.0, true,  false, 12),
    GRAVEL       (13,  "Gravier",          1.5,    1800.0, true,  false, 13),
    CLAY         (14,  "Argile",           1.0,    1750.0, true,  false, 14),
    ICE          (15,  "Glace",            2.0,    917.0,  true,  false, 15),
    SNOW         (16,  "Neige",            0.5,    500.0,  true,  false, 16),
    SANDSTONE    (17,  "Grès",             4.0,    2300.0, true,  false, 17),
    BASALT       (18,  "Basalte",          6.5,    3000.0, true,  false, 18),
    GRANITE      (19,  "Granite",          6.5,    2750.0, true,  false, 19),

    // === SURFACE BIOME ===
    GRASS        (20,  "Herbe",            1.5,    1400.0, true,  false, 11),
    RED_SAND     (21,  "Sable rouge",      1.0,    1650.0, true,  false, 21),
    VOLCANIC_ASH (22,  "Cendre volcanique",0.8,    1100.0, true,  false, 22),
    FROZEN_SOIL  (23,  "Sol gelé",         3.0,    1800.0, true,  false, 23),
    SALT_FLAT    (24,  "Plaine de sel",    2.0,    2170.0, true,  false, 24),
    REGOLITH     (25,  "Régolithe",        1.0,    1500.0, true,  false, 25),

    // === MINERAIS ===
    IRON_ORE     (100, "Minerai de fer",   5.5,    3500.0, true,  false, 100),
    COPPER_ORE   (101, "Minerai de cuivre",5.0,    3300.0, true,  false, 101),
    TIN_ORE      (102, "Minerai d'étain",  4.5,    3100.0, true,  false, 102),
    NICKEL_ORE   (103, "Minerai de nickel", 5.5,   3400.0, true,  false, 103),
    COAL_ORE         (104, "Charbon",          3.0,    1400.0, true,  false, 104),
    GOLD_ORE     (105, "Minerai d'or",     6.0,    4300.0, true,  false, 105),
    TITANIUM_ORE (106, "Minerai de titane", 7.0,   4500.0, true,  false, 106),
    URANIUM_ORE  (107, "Minerai d'uranium", 7.5,   4900.0, true,  false, 107),
    LITHIUM_ORE  (108, "Minerai de lithium",4.0,   2600.0, true,  false, 108),
    PLATINUM_ORE (109, "Minerai de platine",7.0,    5500.0, true,  false, 109),
    SILICON_ORE  (110, "Quartz (silicium)", 7.0,   2650.0, true,  false, 110),
    ALUMINUM_ORE (111, "Bauxite",          3.5,    2500.0, true,  false, 111),
    SULFUR_ORE   (112, "Soufre natif",     2.0,    2070.0, true,  false, 112),

    // === BLOCS CONSTRUITS ===
    IRON_BLOCK   (200, "Bloc de fer",      6.0,    7870.0, true,  false, 200),
    COPPER_BLOCK (201, "Bloc de cuivre",   5.5,    8960.0, true,  false, 201),
    STEEL_BLOCK  (202, "Bloc d'acier",     7.0,    7850.0, true,  false, 202),
    GLASS        (203, "Verre",            5.5,    2500.0, true,  true,  0),
    CONCRETE     (204, "Béton",            5.0,    2400.0, true,  false, 204),

    // === IMMINABLE ===
    BEDROCK      (255, "Socle",            10.0,   5500.0, false, false, 0);

    // === CHAMPS ===

    private final int id;
    private final String name;
    private final double hardness;     // Mohs
    private final double density;      // kg/m³
    private final boolean minable;
    private final boolean transparent; // pour le culling des faces
    private final int dropId;          // ID du bloc/item droppé

    BlockType(int id, String name, double hardness, double density,
              boolean minable, boolean transparent, int dropId) {
        this.id = id;
        this.name = name;
        this.hardness = hardness;
        this.density = density;
        this.minable = minable;
        this.transparent = transparent;
        this.dropId = dropId;
    }

    // === LOOKUP RAPIDE PAR ID ===

    /**
     * Table de lookup : id → BlockType.
     *
     * Pourquoi un tableau statique plutôt qu'une boucle for ?
     *   - La boucle for dans fromId() parcourt TOUS les enums à chaque appel
     *   - Construction d'un mesh chunk = ~4096 appels à fromId()
     *   - 500 chunks visibles × 4096 = 2 millions d'appels potentiels
     *   - Tableau : O(1) direct, pas de comparaison
     *   - Boucle : O(n) avec n = nombre d'enums
     *
     * Taille 256 car nos IDs vont de 0 à 255.
     * Si on ajoute des IDs > 255, agrandir le tableau.
     */
    private static final BlockType[] BY_ID = new BlockType[256];

    static {
        for (BlockType type : values()) {
            if (type.id >= 0 && type.id < BY_ID.length) {
                BY_ID[type.id] = type;
            }
        }
    }

    /**
     * Recherche par ID (pour désérialisation chunks).
     * O(1) au lieu de O(n).
     */
    public static BlockType fromId(int id) {
        if (id < 0 || id >= BY_ID.length || BY_ID[id] == null) {
            return AIR;
        }
        return BY_ID[id];
    }

    // === CALCULS PHYSIQUES ===

    /**
     * Masse d'un bloc de 1m³ en kg.
     * masse = densité × volume
     */
    public double getBlockMass() {
        return density * 1.0;
    }

    /**
     * Temps de minage en secondes (version originale sans gravité).
     * t = (dureté / efficacité_outil)²
     *
     * Main nue      : effi = 1.0
     * Pioche pierre  : effi = 2.0
     * Pioche fer     : effi = 4.0
     */
    public double getMiningTime(double toolEfficiency) {
        if (!minable) return Double.MAX_VALUE;
        if (toolEfficiency <= 0) return Double.MAX_VALUE;
        double ratio = hardness / toolEfficiency;
        return ratio * ratio;
    }

    /**
     * Temps de minage avec prise en compte de la gravité locale.
     *
     * t = (dureté / efficacité_outil)² × sqrt(g / g_terre)
     *
     * Pourquoi sqrt(g) et pas g directement ?
     *   Miner = lever un outil et frapper vers le bas.
     *   L'énergie pour lever l'outil E = m × g × h (proportionnel à g).
     *   Mais la vitesse de frappe descendante est AIDÉE par g.
     *   Le bilan net : la fatigue augmente, mais pas linéairement.
     *   sqrt(g) est un compromis réaliste entre :
     *     - g^0 (la gravité n'a aucun effet → absurde)
     *     - g^1 (la gravité double le temps → trop punitif)
     *
     * Exemples avec pioche fer (effi=4.0), pierre (dureté=6.0) :
     *   Terre (g=9.81) : (6/4)² × sqrt(1.0) = 2.25s
     *   Lune  (g=1.62) : (6/4)² × sqrt(0.165) = 0.91s  → plus facile
     *   Mars  (g=3.72) : (6/4)² × sqrt(0.379) = 1.39s
     *   Super-Terre (g=15) : (6/4)² × sqrt(1.53) = 2.78s → plus dur
     */
    public double getMiningTime(double toolEfficiency, double localGravity) {
        if (!minable) return Double.MAX_VALUE;
        if (toolEfficiency <= 0) return Double.MAX_VALUE;
        double ratio = hardness / toolEfficiency;
        double gravityFactor = Math.sqrt(localGravity / 9.81);
        return ratio * ratio * gravityFactor;
    }

    /**
     * Est-ce qu'un outil de cette dureté peut miner ce bloc ?
     *
     * Règle : l'outil doit avoir une dureté Mohs >= celle du bloc.
     * Sinon le bloc ne cède pas du tout (pas juste plus lent → impossible).
     *
     * C'est cohérent avec Mohs : un ongle (2.5) ne raye pas le quartz (7).
     *   Main nue      = 1.0
     *   Outil pierre   = 3.0
     *   Outil fer      = 5.0
     *   Outil acier    = 7.0
     *   Outil titane   = 8.5
     *   Outil diamant  = 10.0
     */
    public boolean canBeMined(double toolHardness) {
        if (!minable) return false;
        return toolHardness >= hardness;
    }

    // === RENDU ===

    /**
     * Faut-il dessiner la face entre ce bloc et son voisin ?
     *
     * On dessine la face SI le voisin est transparent.
     *   Pierre à côté d'air    → face VISIBLE
     *   Pierre à côté de pierre → face CACHÉE (pas dessinée)
     *   Pierre à côté de verre  → face VISIBLE
     *
     * C'est L'optimisation la plus importante du rendu voxel :
     *   Chunk plein : 4096 blocs × 6 faces = 24 576 faces
     *   Après culling : ~2000 faces (celles exposées à l'air)
     *   = réduction de ~92% des triangles
     */
    public boolean shouldRenderFaceAgainst(BlockType neighbor) {
        return neighbor.transparent;
    }

    // === GETTERS ===

    public int getId() { return id; }
    public String getName() { return name; }
    public double getHardness() { return hardness; }
    public double getDensity() { return density; }
    public boolean isMinable() { return minable; }
    public boolean isTransparent() { return transparent; }
    public int getDropId() { return dropId; }
    public boolean isSolid() { return !transparent; }
}
