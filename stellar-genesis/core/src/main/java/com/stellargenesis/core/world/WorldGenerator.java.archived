package com.stellargenesis.core.world;

import com.stellargenesis.core.noise.OpenSimplex2;
import com.stellargenesis.core.noise.OpenSimplex2S;
import com.stellargenesis.core.physics.PlanetData;

/**
 * Génère le contenu des chunks à partir du bruit procédural.
 *
 * Pipeline de génération d'un chunk :
 *   1. Heightmap      → altitude du sol via fBm (fractal Brownian motion)
 *   2. Remplissage    → pierre sous le sol, air au-dessus
 *   3. Surface        → couche de surface selon le biome
 *   4. Minerais       → veines de minerai selon profondeur + bruit 3D
 *   5. Structures     → grottes, ravins (bruit 3D seuillé)
 *
 * fBm (fractal Brownian motion) :
 *   h(x,z) = Σ(i=0..octaves) amplitude_i × noise(x × freq_i, z × freq_i)
 *   amplitude_i = persistance^i
 *   freq_i      = lacunarité^i
 *
 *   persistance = 0.5 → chaque octave a la moitié de l'amplitude
 *   lacunarité  = 2.0 → chaque octave a le double de la fréquence
 *
 * Résultat normalisé dans [0, 1] puis mappé à [minHeight, maxHeight].
 */

public class WorldGenerator {

    // === DONNÉES PLANÉTAIRES ===
    private final PlanetData planet;
    private final long seed;

    // === PARAMÈTRES fBm ===
    // Ces valeurs contrôlent l'aspect visuel du terrain.
    // On pourrait les varier par planète (planète lisse vs rocailleuse).
    private final int octaves;           // nombre de couches de bruit
    private final double persistence;    // facteur d'amplitude entre octaves
    private final double lacunarity;     // facteur de fréquence entre octaves
    private final double baseFrequency;  // fréquence de l'octave 0 (échelle globale)

    // === PARAMÈTRES TERRAIN ===
    private final int seaLevel;          // niveau de la "mer" en blocs (même sans eau)
    private final int maxTerrainHeight;  // hauteur max du terrain en blocs
    private final int worldHeight;       // hauteur totale du monde en blocs (Y max)

    // === PARAMÈTRES GROTTES ===
    private final double caveThreshold;  // seuil : noise3D < threshold → grotte
    private final double caveFrequency;  // fréquence du bruit de grottes

    // === PARAMÈTRES HUMIDITÉ ===
    private final double humidityFrequency; // fréquence du bruit d'humidité

    // === AMPLITUDE MAX fBm (pré-calculée) ===
    // Sert à normaliser le bruit brut en [0, 1]
    // maxAmplitude = Σ persistance^i pour i=0..octaves-1
    //              = (1 - persistance^octaves) / (1 - persistance)  (série géométrique)
    private final double maxAmplitude;

    /**
     * Constructeur avec paramètres par défaut adaptés à une planète terrestre.
     */
    public WorldGenerator(PlanetData planet) {
        this(planet, 6, 0.5, 2.0, 0.005, 64, 128, 256, -0.55, 0.03, 0.008);
    }

    /**
     * Constructeur complet.
     *
     * @param planet           données physiques de la planète
     * @param octaves          couches de bruit fBm (4–10)
     * @param persistence      réduction d'amplitude par octave (0.3–0.7)
     * @param lacunarity       augmentation de fréquence par octave (1.5–2.5)
     * @param baseFrequency    fréquence de base (0.001–0.01, petit = grandes structures)
     * @param seaLevel         altitude 0 en blocs
     * @param maxTerrainHeight amplitude max du relief en blocs
     * @param worldHeight      hauteur Y max du monde
     * @param caveThreshold    seuil de grottes (négatif = moins de grottes)
     * @param caveFrequency    fréquence du bruit 3D des grottes
     * @param humidityFrequency fréquence du bruit d'humidité
     */

    public WorldGenerator(PlanetData planet,
                          int octaves, double persistence, double lacunarity,
                          double baseFrequency, int seaLevel, int maxTerrainHeight,
                          int worldHeight, double caveThreshold, double caveFrequency,
                          double humidityFrequency) {
        this.planet = planet;
        this.seed = planet.getSeed();
        this.octaves = octaves;
        this.persistence = persistence;
        this.lacunarity = lacunarity;
        this.baseFrequency = baseFrequency;
        this.seaLevel = seaLevel;
        this.maxTerrainHeight = maxTerrainHeight;
        this.worldHeight = worldHeight;
        this.caveThreshold = caveThreshold;
        this.caveFrequency = caveFrequency;
        this.humidityFrequency = humidityFrequency;

        //Pré-calcul amplitude max (série géométrique)
        double amp = 0.0;
        double a = 1.0;
        for (int i = 0; i < octaves; i++) {
            amp += a;
            a *= persistence;
        }
        this.maxAmplitude = amp;
    }

    // ================================================================
    //  1. HAUTEUR DU TERRAIN — fBm (fractal Brownian motion)
    // ================================================================

    /**
     * Calcule la hauteur du terrain en blocs à la position (worldX, worldZ).
     *
     * fBm = Σ amplitude_i × noise(x × freq_i, z × freq_i)
     *
     * Le résultat brut est normalisé en [0, 1] puis scalé :
     *   terrainHeight = seaLevel + h_norm × maxTerrainHeight
     *
     * @return hauteur en blocs (entier)
     */
    public int getTerrainHeight(int worldX, int worldZ){
        double rawNoise = 0.0;
        double amplitude = 1.0;
        double frequency = baseFrequency;

        for (int i = 0; i < octaves; i++) {
            // OpenSimplex2.noise2 retourne [-1, +1]
            double nx = worldX * frequency;
            double nz = worldZ * frequency;
            rawNoise += amplitude * OpenSimplex2S.noise3_ImproveXZ(seed, nx, 0, nz);

            amplitude *= persistence;
            frequency *= lacunarity;
        }

        // Normaliser [-maxAmplitude, +maxAmplitude] -> [0, 1]
        double normalized = (rawNoise + maxAmplitude) / (2.0 * maxAmplitude);

        // Clamper par sécurité (le Bruit peut être très rarement dépasser)
        normalized = Math.max(0.0, Math.min(1.0, normalized));

        return seaLevel + (int) (normalized * maxTerrainHeight);
    }

    // ================================================================
    //  2. HUMIDITÉ LOCALE — Bruit Simplex indépendant
    // ================================================================

    /**
     * Humidité locale en [0, 1] à la position (worldX, worldZ).
     *
     * Utilise un seed décalé (+12345) pour être indépendant du terrain.
     * C'est un bruit lent (basse fréquence) car l'humidité varie
     * sur de grandes distances (échelle de biomes, pas de blocs).
     */
    public double getHumidity(int worldX, int worldZ){
        double raw = OpenSimplex2.noise2(seed + 12345,
                worldX * humidityFrequency,
                worldZ * humidityFrequency
        );
        // [-1, 1] -> [0, 1]
        return (raw + 1.0) / 2.0;
    }

    // ================================================================
    //  3. BIOME LOCAL — Sélection par conditions physiques
    // ================================================================

    /**
     * Détermine le biome à la position (worldX, worldZ).
     *
     * Pipeline :
     *   1. Calculer la hauteur du terrain → altitude en mètres
     *   2. PlanetData.getTemperatureAtAltitude(h) → T(h)
     *   3. PlanetData.getPressureAtAltitude(h) → P(h)
     *   4. getHumidity(x, z) → humidité locale
     *   5. BiomeType.select(T, P, H) → meilleur biome
     *
     * CONVERSION BLOCS → MÈTRES :
     *   On considère 1 bloc = 1 mètre (simplification standard).
     *   L'altitude physique = (terrainHeight - seaLevel) mètres.
     *   seaLevel = altitude 0 en termes physiques.
     */
    public BiomeType getBiome(int worldX, int worldZ){
        int terrainHeight = getTerrainHeight(worldX, worldZ);

        // Altitude physique en mètres (1 bloc = 1m)
        double altitudeMeters = (terrainHeight - seaLevel);

        // Conditions physiques à cette altitude
        double temperature = planet.getTemperatureAtAltitude(altitudeMeters);
        double pressure = planet.getPressureAltitude(altitudeMeters);
        double humidity = getHumidity(worldX, worldZ);

        return BiomeType.select(temperature, pressure, humidity);
    }

    // ================================================================
    //  4. GROTTES — Bruit 3D
    // ================================================================

    /**
     * Détermine si un voxel (worldX, worldY, worldZ) est une grotte.
     *
     * On utilise un bruit 3D : si la valeur est inférieure au seuil,
     * le voxel est de l'air (grotte).
     *
     * Le seuil est négatif (ex: -0.55) → seules les valeurs très basses
     * du bruit créent des grottes → grottes rares et organiques.
     *
     * Plus le seuil est proche de 0, plus il y a de grottes.
     *   -0.7 → rares, petites poches
     *   -0.5 → moyennes, tunnels connectés
     *   -0.3 → abondantes, grandes cavernes
     *
     * On ne creuse pas de grotte au-dessus du terrain ni dans les
     * 3 derniers blocs avant la surface (éviter les trous en surface).
     */
    public boolean isCave(int worldX, int worldY, int worldZ){
        double noise = OpenSimplex2.noise3_ImproveXZ(
                seed + 99999,
                worldX * caveFrequency,
                worldY * caveFrequency,
                worldZ * caveFrequency
        );
        return noise < caveThreshold;
    }

    // ================================================================
    //  5. MINERAIS — Bruit 3D + Gaussienne de profondeur
    // ================================================================

    /**
     * Détermine quel minerai (ou rien) se trouve à (worldX, worldY, worldZ).
     *
     * Chaque type de minerai a :
     *   - une profondeur optimale (centre de la gaussienne)
     *   - une variance de profondeur (largeur de la gaussienne)
     *   - une fréquence de bruit (taille des filons)
     *   - un seuil (rareté : plus le seuil est haut, plus c'est rare)
     *
     * Algorithme :
     *   1. Pour chaque minerai, calculer un facteur de profondeur :
     *      depthFactor = exp(-(depth - optimal)² / (2 × variance²))
     *      C'est une gaussienne centrée sur la profondeur optimale.
     *      → Le minerai est PROBABLE à sa profondeur optimale
     *      → Il devient RARE en s'éloignant
     *
     *   2. Multiplier par un bruit 3D pour créer des filons :
     *      veinNoise = noise3D(x, y, z) avec un seed unique par minerai
     *
     *   3. Si depthFactor × veinNoise > seuil → placer le minerai
     *
     *   4. Le premier minerai qui passe le test gagne (priorité par ordre).
     *      Les minerais rares sont testés EN PREMIER pour ne pas être
     *      écrasés par les minerais communs.
     *
     * @param worldY   coordonnée Y du voxel (0 = bedrock, worldHeight = ciel)
     * @return le BlockType du minerai, ou null si pas de minerai
     */
    public BlockType getOre(int worldX, int worldY, int worldZ){
        // Profondeur = distance depuis la surface vers le bas
        // depth = 0 en surface, augmente vers le bas
        int terrainH = getTerrainHeight(worldX, worldZ);
        int depth = terrainH - worldY;

        if (depth < 0) return null; // au-dessus du terrain

        for (OreConfig ore : OreConfig.ALL_ORES){

            // 1. Gaussienne de profondeur
            double depthDiff = depth - ore.optimalDepth;
            double depthFactor = Math.exp(
                    -(depthDiff * depthDiff) / (2.0 * ore.depthVariance * ore.depthVariance)
            );

            // 2. Bruit 3D pour les filons (seed unique par minerai)
            double veinNoise = OpenSimplex2.noise3_ImproveXZ(
                    seed + ore.seedOffset,
                    worldX * ore.frequency,
                    worldY * ore.frequency,
                    worldZ * ore.frequency
                    );

            // veinNoise ∈ [-1, 1], on prend la partie positive
            veinNoise = (veinNoise +1.0) / 2.0; // -> [0, 1]

            // 3. Combinaison : si le produit dépasse le seuil → minerai
            double combined = depthFactor * veinNoise;
            if (combined > ore.threshold){
                return ore.blockType;
            }
        }
        return null; // pas de minerai
    }

    // ================================================================
    //  6. GÉNÉRATION D'UN CHUNK COMPLET
    // ================================================================

    /**
     * Remplit un chunk entier avec les voxels appropriés.
     *
     * Pour chaque colonne (localX, localZ) du chunk :
     *   1. Convertir en coordonnées monde
     *   2. Calculer la hauteur du terrain
     *   3. Déterminer le biome
     *   4. Pour chaque Y de bas en haut :
     *      a. Si Y > terrainHeight → AIR
     *      b. Si Y == terrainHeight → surfaceBlock du biome
     *      c. Si Y > terrainHeight - 4 → subSurfaceBlock (couche intermédiaire)
     *      d. Sinon → fillerBlock (roche de base)
     *      e. Vérifier grotte → remplacer par AIR
     *      f. Vérifier minerai → remplacer le filler
     *      g. Y == 0 → BEDROCK (indestructible)
     *
     * @param chunk le chunk à remplir (déjà créé, voxels initialisés à AIR)
     */
    public void generateChunk(Chunk chunk){
        int chunkWorldX = chunk.getPosition().x * Chunk.SIZE;
        int chunkWorldY = chunk.getPosition().y * Chunk.SIZE;
        int chunkWorldZ = chunk.getPosition().z * Chunk.SIZE;

        for (int lx = 0; lx < Chunk.SIZE; lx++) {
            for (int lz = 0; lz < Chunk.SIZE; lz++) {

                int worldX = chunkWorldX + lx;
                int worldZ = chunkWorldZ + lz;

                // Hauteur et biome pour cette colonne
                int terrainHeight = getTerrainHeight(worldX, worldZ);
                BiomeType biome = getBiome(worldX, worldZ);

                for (int ly = 0; ly < Chunk.SIZE; ly++) {

                    int worldY = chunkWorldY + ly;

                    // --- Déterminer le type de bloc ---
                    BlockType block;

                    if (worldY > terrainHeight){
                        // Au-dessus du terrain -> air
                        block = BlockType.AIR;

                    } else if (worldY == 0) {
                        // Fond du monde -> bedrock indestructible
                        block = BlockType.BEDROCK;

                    } else if (worldY == terrainHeight) {
                        // Surface
                        block = biome.getSurfaceBlock();

                    } else if (worldY > terrainHeight - 4) {
                        // 3 blocs sous la surface
                        block = biome.getSubSurfaceBlock();

                    } else {
                        // Profondeur -> roche de base du biome
                        block = biome.getFillerBlock();

                        // Vérifier minerai (remplace le filler)
                        BlockType ore = getOre(worldX, worldY, worldZ);
                        if (ore != null) {
                            block = ore;
                        }
                    }

                    // --- Grottes ---
                    // Ne pas creuser dans les 3 blocs sous la surface
                    // (évite les trous disgracieux en surface)
                    if (worldY < terrainHeight - 3 && worldY > 0){
                        if (isCave(worldX, worldY, worldZ)){
                            block = BlockType.AIR;
                        }
                    }

                    // Stocker le voxel
                    chunk.setBlock(lx, ly, lz, block);
                }
            }
        }
        chunk.markGenerated();
    }

    // === GETTERS ===

    public PlanetData getPlanet() { return planet; }
    public int getSeaLevel() { return seaLevel; }
    public int getMaxTerrainHeight() { return maxTerrainHeight; }
    public int getWorldHeight() { return worldHeight; }

}
