package com.stellargenesis.core.world;


/**
 * Chaque bloc du monde avec ses propriétés physiques.
 *
 * Dureté : échelle de Mohs (1=talc, 10=diamant)
 * Densité : kg/m³ réels
 * Masse par bloc : densité × volume (1m³)
 *
 * Le temps de minage dépend de la dureté :
 *   t_mine = (dureté / outil_efficacité)² secondes
 *
 * @author Noa Moal
 */

public enum BlockType {

    // id, nom, dureté Mohs, densité kg/m³, minable
    AIR         (0, "Air",        0.0, 0.0,     false),
    STONE       (1, "Pierre",     6.0, 2700.0,     true),
    DIRT        (2, "Terre",      1.5, 1500.0,     true),
    SAND        (3, "Sable",      1.0, 1600.0,     true),
    IRON_ORE    (4, "Minerai Fer",5.0, 5200.0,     true),
    COPPER_ORE  (5, "Minerai Cu", 3.5, 4200.0,     true),
    COAL        (6, "Charbon",    2.5, 1400.0,     true),
    ICE         (7, "Glace",      1.5, 917.0,     true),
    BASALT      (8, "Basalte",    6.5, 3000.0,     true),
    TITANIUM_ORE(9, "Minerai Ti", 6.0, 4500.0,     true),
    BEDROCK     (10, "Socle",     10.0, 5500.0,     false);

    private final int id;
    private final String name;
    private final double hardness;
    private final double density;
    private final boolean minable;

    BlockType(int id, String name, double hardness, double density, boolean minable){
        this.id = id;
        this.name = name;
        this.hardness = hardness; // Mohs
        this.density = density;   // kg/m³
        this.minable  = minable;
    }

    // === GETTERS ===
    public int getId(){ return id; }
    public String getName(){ return name; }
    public double getHardness(){ return hardness; }
    public double getDensity(){ return density; }
    public boolean isMinable(){ return minable; }


    /**
     * Masse d'un bloc de 1m³ en kg.
     * masse = densité x volume
     */
    public double getBlockMass(){
        return density * 1.0;
    }

    /**
     * Temps de minage en secondes.
     * t = (dureté / efficacité_outils)²
     *
     * Main nue : effi = 1.0
     * Pioche en pierre : effi = 2.0
     * Pioche en fer : effi = 4.0
     */
    public double getMiningTime(double toolEfficiency){
        if (!minable) return Double.MAX_VALUE;
        if (toolEfficiency <= 0) return  Double.MAX_VALUE;
        double ratio = hardness / toolEfficiency;
        return ratio * ratio;
    }

    /**
     * Recherche par ID (pour désérialisation chunks)
     */
    public  static BlockType fromId(int id){
        for (BlockType b : values()){
            if (b.id == id) return b;
        }
        return AIR;
    }

}
