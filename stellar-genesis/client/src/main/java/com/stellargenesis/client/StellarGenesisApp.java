package com.stellargenesis.client;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.light.DirectionalLight;
import com.jme3.light.AmbientLight;
import com.jme3.system.AppSettings;

import com.stellargenesis.core.world.*;
import com.stellargenesis.core.physics.PlanetData;
import com.stellargenesis.core.physics.PlanetPhysics;

/**
 * Point d'entrée du jeu.
 *
 * Cycle de vie jME :
 *   1. simpleInitApp()  → appelé UNE FOIS au démarrage
 *   2. simpleUpdate(tpf) → appelé CHAQUE FRAME (60 fps = 60 appels/sec)
 *   3. simpleRender()    → rendu automatique après chaque update
 *
 * tpf = "time per frame" en secondes (ex: 0.016 pour 60 fps)
 */

public class StellarGenesisApp extends SimpleApplication {

     // -- Monde --
    private  ChunkManager chunkManager;
    private WorldGenerator worldGenerator;
    private PlanetData planetData;

    // -- Rendu --
    private Node worldNode;              // noeud parent de tous les chunks
    private Material blockMaterial;      // matériau partagé par tous les blocs
    private int renderDistance = 4;      // en Chunks (4 = 64 blocs de vue)

    // -- État
    private Vector3f lastUpdatePos;      // dernière position où on a mis à jour les chunks
    private static final float CHUNK_UPDATE_THRESHOLD = 8f;

    // ═══════════════════════════════════════════
    //  MAIN — Point d'entrée
    // ═══════════════════════════════════════════
    public static void main(String[] args){
        StellarGenesisApp app = new StellarGenesisApp();

        //Config de la fen
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Stellar Genesis");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setVSync(true);
        settings.setSamples(4);
        settings.setFrameRate(60);

        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }


    @Override
    public void simpleInitApp() {
        // 1. Générer les données physiques de la planète
        initPlanetData();

        // 2. Préparer le monde
        initWorld();
// 3. Configurer le rendu
        initMaterial();
        initLighting();

        // 4. Configurer la caméra
        initCamera();

        // 5. Charger les premiers chunks autour du spawn
        loadInitialChunks();

        System.out.println("=== STELLAR GENESIS ===");
        System.out.println("Planète : masse=" + String.format("%.2e", planetData.getMass()) + " kg");
        System.out.println("Gravité : " + String.format("%.2f", planetData.getSurfaceGravity()) + " m/s²");
        System.out.println("Température : " + String.format("%.1f", planetData.getEquilibriumTemp()) + " K");
        System.out.println("Pression : " + String.format("%.3f", planetData.getSurfacePressure()) + " bar");

//        // DEBUG : cube rouge visible à l'origine
//        com.jme3.scene.shape.Box b = new com.jme3.scene.shape.Box(5, 5, 5);
//        Geometry testCube = new Geometry("test", b);
//        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        mat.setColor("Color", ColorRGBA.Red);
//        testCube.setMaterial(mat);
//        testCube.setLocalTranslation(32, 30, 32);
//        rootNode.attachChild(testCube);
//
//        System.out.println("CAM: " + cam.getLocation());
//        System.out.println("CAM DIR: " + cam.getDirection());

        // DEBUG : vérifier qu'un chunk contient des blocs
        ChunkPos testPos = new ChunkPos(0, 3, 0); // chunk qui devrait contenir du terrain
        Chunk testChunk = new Chunk(testPos);
        worldGenerator.generateChunk(testChunk);

        int solidCount = 0;
        for (int x = 0; x < 16; x++)
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    if (testChunk.getBlock(x, y, z) != 0) solidCount++;

        System.out.println("=== DEBUG: Chunk " + testPos + " solid blocks: " + solidCount);
        System.out.println("=== DEBUG: terrainHeight at (0,0): " + worldGenerator.getTerrainHeight(0, 0));
        System.out.println("=== DEBUG: seaLevel: " + worldGenerator.getSeaLevel());
    }

    /**
     * Crée une planète avec des paramètres physiques réalistes.
     * Plus tard ces valeurs seront générées aléatoirement par seed.
     */
    private void initPlanetData(){
        String name = "Terra Nova";
        long seed = 42L;
        double mass = 5.972e24;
        double radius = 6.371e6;
        double distanceToStar = 1.496e11;
        double rotationPeriod = 86400.0;        // 24h en secondes
        double axialTilt = Math.toRadians(23.5);
        double albedo = 0.3;
        double starMass = 1.989e30;
        double starRadius = 6.957e8;
        double starTemp = 5778.0;
        double starLuminosity = 3.846e26;
        double surfacePressure = 1.013;         // bar
        double atmosMolarMass = 0.029;          // kg/mol
        double[] atmosComposition = {0.78, 0.21, 0.0004, 0.009, 0.01, 0.0};

        planetData = new PlanetData(name, seed,
                mass, radius, distanceToStar, rotationPeriod,
                axialTilt, albedo,
                starMass, starRadius, starTemp, starLuminosity,
                surfacePressure, atmosMolarMass, atmosComposition);
    }

    /**
     * Initialise le générateur de monde et le gestionnaire de chunks.
     */
    private void initWorld(){

        worldGenerator = new WorldGenerator(planetData);
        chunkManager = new ChunkManager(worldGenerator, renderDistance);

        worldNode = new Node("World");
        rootNode.attachChild(worldNode);
    }


    /**
     * Crée le matériau utilisé pour tous les blocs.
     * Pour l'instant : un matériau simple avec couleur par vertex.
     * Plus tard : textures atlas, shaders custom.
     */
    private void initMaterial(){
        // Matériau Lighting = réagit à la lumière (ombres, reflets)
        blockMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        blockMaterial.setBoolean("UseMaterialColors", true);
        blockMaterial.setColor("Diffuse", ColorRGBA.Gray);
        blockMaterial.setColor("Ambient", ColorRGBA.DarkGray);

        // La grosse flemme sah plus tard
        // Alternative simple sans vertex color :
        // blockMaterial.setColor("Diffuse", ColorRGBA.Gray);
        // blockMaterial.setColor("Ambient", ColorRGBA.DarkGray);
        // blockMaterial.setBoolean("UseMaterialColors", true);
    }

    /**
     * Configure l'éclairage de la scène.
     * La direction et la couleur dépendent du type spectral de l'étoile.
     */
    private void initLighting(){
        // Lumière directionnelle = le "soleil"
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1f, -0.5f).normalizeLocal());

        // Couleur selon le type spectral de l'étoile
        // Soleil (G2) = lumière blanche légèrement jaune
        sun.setColor(getStarLightColor(planetData.getStarTemperature()));
        rootNode.addLight(sun);

        // Lumière ambiante = éclairage minimum dans les ombres
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(ambient);
    }

    /**
     * Calcule la couleur de la lumière stellaire selon la température.
     *
     * Étoile froide (3000K) → rouge/orange
     * Étoile moyenne (5800K) → blanc/jaune
     * Étoile chaude (10000K+) → blanc/bleu
     *
     * Basé sur la loi de Wien : λ_max = 2.898×10⁶ / T
     */
    private ColorRGBA getStarLightColor(double starTemp){
        // Approximation simplifiée de la couleur d'un corps noir
        float t = (float) (starTemp / 10000.0);

        float r, g, b;
        if (t < 0.4f) {
            // Étoile froide : rouge-orange (naine M)
            r = 1.0f;
            g = 0.4f + t;
            b = 0.2f;
        } else if (t < 0.7f) {
            // Étoile moyenne : blanc-jaune (type G/K)
            r = 1.0f;
            g = 0.8f + (t - 0.4f) * 0.6f;
            b = 0.6f + (t - 0.4f) * 1.3f;
        } else {
            // Étoile chaude : blanc-bleu (type A/B/O)
            r = 0.7f + (1.0f - t) * 0.3f;
            g = 0.85f;
            b = 1.0f;
        }

        return new ColorRGBA(r, g, b, 1.0f);
    }

    /**
     * Configure la caméra FPS.
     * Le joueur spawn au-dessus du terrain.
     */
    private void initCamera(){
        // Vitesse de déplacement de la caméra (fly cam par défaut)
        flyCam.setMoveSpeed(50f);
        flyCam.setRotationSpeed(2f);

//        // Position de départ : au centre, au-dessus du terrain
//        // On trouvera la hauteur du terrain au spawn
//        float spawnX = 0;
//        float spawnZ = 0;
//        float spawnY = findTerrainHeight(spawnX, spawnZ) + 5f;

        cam.setLocation(new Vector3f(32, 150, 32));
        cam.lookAt(new Vector3f(32, 128, 32), Vector3f.UNIT_Y);

        lastUpdatePos = cam.getLocation().clone();
    }

    /**
     * Trouve la hauteur du terrain à une position (x, z).
     * Parcourt les blocs de haut en bas jusqu'à trouver un solide.
     */
    private float findTerrainHeight(float x, float z){
        int ix = (int) x;
        int iz = (int) z;

        // Chercher de haut en bas (chunk max → 0)
        for (int y = Chunk.SIZE * 8; y >= 0 ; y--) {
            ChunkPos cPos = new ChunkPos(
                    Math.floorDiv(ix, Chunk.SIZE),
                    Math.floorDiv(y, Chunk.SIZE),
                    Math.floorDiv(iz, Chunk.SIZE)
            );

            Chunk chunk = chunkManager.getChunk(cPos);
            if (chunk != null) {
                int localX = ((ix % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;
                int localY = ((y % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;
                int localZ = ((iz % Chunk.SIZE) + Chunk.SIZE) % Chunk.SIZE;

                if(chunk.getBlock(localX, localY, localZ) != 0){
                    return y + 1;
                }
            }
        }
        return 64f;
    }

    /**
     * Charge et affiche les chunks autour du point de spawn.
     */
    private void loadInitialChunks(){
        updateVisibleChunks(cam.getLocation());
    }

    // ═══════════════════════════════════════════
    //  UPDATE — Appelé chaque frame
    // ═══════════════════════════════════════════

    @Override
    public void simpleUpdate(float tpf){
        Vector3f playerPos = cam.getLocation();

        // Ne recalculer les chunks que si le joueur a bougé assez
        if(playerPos.distance(lastUpdatePos) > CHUNK_UPDATE_THRESHOLD){
            updateVisibleChunks(playerPos);
            lastUpdatePos = playerPos.clone();
        }
    }

    /**
     * Met à jour les chunks visibles autour du joueur.
     *
     * 1. Calculer quels chunks doivent être chargés
     * 2. Générer ceux qui ne le sont pas encore
     * 3. Construire leur mesh et les ajouter à la scène
     * 4. Retirer les chunks trop loin
     */
    private void updateVisibleChunks(Vector3f playerPos){
        // Position du joueur en coordonnées chunk
        int pcx = Math.floorDiv((int) playerPos.x, Chunk.SIZE);
        int pcy = Math.floorDiv((int) playerPos.y, Chunk.SIZE);
        int pcz = Math.floorDiv((int) playerPos.z, Chunk.SIZE);

        // 1. Détacher tous les anciens chunks
        worldNode.detachAllChildren();

        // 2. Parcourir tous les chunks dans le rayon de rendu
        for (int dx = -renderDistance; dx <= renderDistance ; dx++) {
            for (int dy = -2; dy <= 4 ; dy++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {

                    ChunkPos pos = new ChunkPos(pcx + dx, pcy + dy, pcz + dz);

                    // Charger/générer le chunk si nécessaire
                    Chunk chunk = chunkManager.getOrGenerate(pos);
                    if (chunk == null) continue;

                    // Construire le mesh
                    Mesh mesh = ChunkMeshBuilder.buildMesh(chunk, chunkManager);
                    if (mesh == null) continue;

                    // Créer le Geometry jME et le positioner
                    Geometry geom = new Geometry("chunk_" + pos, mesh);
                    geom.setMaterial(blockMaterial);

                    // Position monde du chunk
                    geom.setLocalTranslation(
                            pos.x * Chunk.SIZE,
                            pos.y * Chunk.SIZE,
                            pos.z * Chunk.SIZE
                    );

                    worldNode.attachChild(geom);
                }
            }
        }
    }
}
