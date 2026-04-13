package com.stellargenesis.client;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.light.DirectionalLight;
import com.jme3.light.AmbientLight;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

import com.stellargenesis.client.audio.OSTManager;
import com.stellargenesis.client.audio.SFXManager;
import com.stellargenesis.client.player.PlayerControl;
import com.stellargenesis.client.player.PlayerInteraction;
import com.stellargenesis.client.player.StaminaSystem;
import com.stellargenesis.client.render.DayNightCycle;
import com.stellargenesis.client.render.SkyManager;
import com.stellargenesis.client.ui.*;
import com.stellargenesis.core.inventory.Inventory;
import com.stellargenesis.core.player.MiningSystem;
import com.stellargenesis.core.world.*;
import com.stellargenesis.core.physics.PlanetData;
import com.stellargenesis.core.physics.PlanetPhysics;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

    private java.awt.Robot robot;
    private int centerX, centerY;

     // -- Monde --
    private  ChunkManager chunkManager;
    private WorldGenerator worldGenerator;
    private PlanetData planetData;

    // -- Rendu --
    private Node worldNode;              // noeud parent de tous les chunks
    private Material blockMaterial;      // matériau partagé par tous les blocs
    private int renderDistance = 4;      // en Chunks (4 = 64 blocs de vue)

    // -- État --
    private Vector3f lastUpdatePos;      // dernière position où on a mis à jour les chunks
    private static final float CHUNK_UPDATE_THRESHOLD = 8f;

    // -- Joueur --
    private PlayerControl playerControl;
    private BulletAppState bulletAppState;
    private PlayerInteraction playerInteraction;
    private StaminaSystem staminaSystem;
    private Inventory inventory;


    // -- UI --
    private GameHUD gameHUD;
    private Crosshair crosshair;
    private StaminaBar staminaBar;
    private MiningBar miningBar;
    private InventoryScreen invScreen;

    // -- Rendu --
    private SkyManager skyManager;
    private DayNightCycle dayNightCycle;
    private DirectionalLight sunLight;
    private AmbientLight ambientLight;

    // -- Audio --                          // ← AJOUT 1
    private OSTManager ostManager;
//    private SFXManager sfxManager;

    // ═══════════════════════════════════════════
    //  MAIN — Point d'entrée
    // ═══════════════════════════════════════════
    public static void main(String[] args){
        StellarGenesisApp app = new StellarGenesisApp();

        //Config de la fen
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Stellar Genesis");
        settings.setWidth(1920);
        settings.setHeight(1080);
//        settings.setResolution(1920, 1080);
//        settings.setFullscreen(true);
        settings.setVSync(true);
        settings.setSamples(4);
        settings.setFrameRate(60);


        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }


    @Override
    public void simpleInitApp() {
        // 0. Désactiver flyCam IMMÉDIATEMENT
        flyCam.setEnabled(false);
        flyCam.setDragToRotate(false);
        inputManager.deleteMapping("FLYCAM_RotateDrag");

        // 1. Physique Bullet EN PREMIER
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        // 2. Générer les données physiques de la planète
        initPlanetData();

        // 3. Appliquer la gravité (après attach, getPhysicsSpace() est dispo)
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -(float) planetData.getSurfaceGravity(), 0));

        // 4. Préparer le monde
        initWorld();

        // 5. Configurer le rendu
        initMaterial();
        initLighting();

        // 5b. Skybox
        skyManager = new SkyManager();
        skyManager.init(assetManager, rootNode, planetData);

        // 5c. Cycle jour/nuit
        dayNightCycle = new DayNightCycle();
        dayNightCycle.init(planetData, sunLight, ambientLight, skyManager);
        // Accélérer pour tester (10× plus vite), enlève ça après
        dayNightCycle.setTimeScale(10f);

        // 6. Configurer la caméra
        initCamera();

        // 7. Charger les premiers chunks autour du spawn
        loadInitialChunks();


        // 8. Créer le joueur avec la gravité de la planète
        float spawnY = findTerrainHeight(32, 32) + 3f;

        staminaSystem = new StaminaSystem(100.0, planetData.getSurfaceGravity());

        float planetGravity = (float) planetData.getSurfaceGravity();
        playerControl = new PlayerControl(
                rootNode, bulletAppState, cam, inputManager, planetGravity,spawnY, staminaSystem
        );

        // Forcer la capture de la souris (empêche de sortir de la fenêtre)
        inputManager.setCursorVisible(false);
        mouseInput.setCursorVisible(false);

        try {
            robot = new java.awt.Robot();
        } catch (Exception e) { e.printStackTrace(); }
        centerX = settings.getWidth() / 2;
        centerY = settings.getHeight() / 2;

    // Dans simpleUpdate()
        if (robot != null) {
            // Récupérer la position de la fenêtre sur l'écran
            java.awt.Point windowPos = new java.awt.Point(0, 0);
            if (context instanceof com.jme3.system.lwjgl.LwjglWindow) {
                // Recentrer la souris au milieu de la fenêtre
                robot.mouseMove(
                        settings.getWidth() / 2 + getContext().getSettings().getWindowXPosition(),
                        settings.getHeight() / 2 + getContext().getSettings().getWindowYPosition()
                );
            }
        }

        MiningSystem miningSystem = new MiningSystem(
                1.0,                // toolEfficiency (main nue)
                planetData.getSurfaceGravity(), // gravité locale
                100.0                           // stamina max
        );

        inventory = new Inventory(
                36,
                80.0,
                planetData.getSurfaceGravity()
        );
        inventory.addItem(BlockType.STONE, 64);
        int notAdded = inventory.addItem(BlockType.STONE, 64);
        System.out.println("STONE ajoutés: " + (64 - notAdded) + " / refusés: " + notAdded);

        playerInteraction = new PlayerInteraction(
                cam, worldNode, inputManager,
                chunkManager, miningSystem, inventory
        );


        // 9. HUD
        gameHUD = new GameHUD();
        gameHUD.init(this, guiNode);
        crosshair = new Crosshair();
        crosshair.initCrosshair(assetManager, guiNode, cam);
        staminaBar = new StaminaBar();
        staminaBar.init(guiNode, assetManager);
        miningBar = new MiningBar();
        miningBar.init(guiNode, assetManager, cam);
        invScreen = new InventoryScreen(assetManager, guiNode, cam);
        invScreen.build(inventory.getSize()); // 40 slots
        playerControl.setInventory(inventory);
        playerControl.setInventoryScreen(invScreen);

        // 10. Audio
        ostManager = new OSTManager(assetManager, rootNode);
        // Exploration
        ostManager.registerCategory("exploration", 0.4f,
                "Sounds/OST/exploration/Crossing_The_Last_Frontier.ogg",
                "Sounds/OST/exploration/Under_A_Cold_Sun.ogg"
//                "Sounds/OST/exploration/wandering_light.ogg",
//                "Sounds/OST/exploration/echoes_beneath.ogg"
        );

        // Combat
//        ostManager.registerCategory("combat", 0.6f,
//                "Sounds/OST/combat/iron_siege.ogg",
//                "Sounds/OST/combat/swarm_incoming.ogg",
//                "Sounds/OST/combat/last_stand.ogg"
//        );

        // Fabrique
//        ostManager.registerCategory("factory", 0.3f,
//                "Sounds/OST/factory/gears_and_gravity.ogg",
//                "Sounds/OST/factory/assembly_line.ogg",
//                "Sounds/OST/factory/molten_core.ogg"
//        );

        // Espace
//        ostManager.registerCategory("space", 0.5f,
//                "Sounds/OST/space/void_between_stars.ogg",
//                "Sounds/OST/space/orbital_drift.ogg",
//                "Sounds/OST/space/stellar_wind.ogg"
//        );
//
//        // Menu
//        ostManager.registerCategory("menu", 0.4f,
//                "Sounds/OST/menu/genesis_theme.ogg",
//                "Sounds/OST/menu/new_dawn.ogg"
//        );
//        sfxManager = new SFXManager(assetManager, rootNode);
        System.out.println("=== AVANT ostManager.playCategory ===");
        ostManager.playCategory("exploration");
        System.out.println("=== APRÈS ostManager.playCategory ===");


        System.out.println("=== STELLAR GENESIS ===");
        System.out.println("Planète : masse=" + String.format("%.2e", planetData.getMass()) + " kg");
        System.out.println("Gravité : " + String.format("%.2f", planetData.getSurfaceGravity()) + " m/s²");
        System.out.println("Température : " + String.format("%.1f", planetData.getEquilibriumTemp()) + " K");
        System.out.println("Pression : " + String.format("%.3f", planetData.getSurfacePressure()) + " bar");

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
    }

    /**
     * Configure l'éclairage de la scène.
     * La direction et la couleur dépendent du type spectral de l'étoile.
     */
    private void initLighting(){
        sunLight = new DirectionalLight();
        sunLight.setDirection(new Vector3f(-0.5f, -1f, -0.3f).normalizeLocal());
        sunLight.setColor(ColorRGBA.White);
        rootNode.addLight(sunLight);

        ambientLight = new AmbientLight();
        ambientLight.setColor(new ColorRGBA(0.3f, 0.3f, 0.3f, 1f));
        rootNode.addLight(ambientLight);
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
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(false);
        cam.setFrustumNear(0.1f);
        cam.setFrustumPerspective(70f, (float)cam.getWidth()/cam.getHeight(), 0.1f, 1000f);
        lastUpdatePos = cam.getLocation().clone();
    }

    /**
     * Trouve la hauteur du terrain à une position (x, z).
     * Parcourt les blocs de haut en bas jusqu'à trouver un solide.
     */
    private float findTerrainHeight(float x, float z) {
        int ix = (int) x;
        int iz = (int) z;

        // Forcer la génération des chunks sous le spawn (synchrone)
        int chunkX = Math.floorDiv(ix, Chunk.SIZE);
        int chunkZ = Math.floorDiv(iz, Chunk.SIZE);
        for (int cy = 0; cy <= 8; cy++) {
            chunkManager.getOrGenerate(new ChunkPos(chunkX, cy, chunkZ));
        }

        // Maintenant chercher de haut en bas
        for (int y = Chunk.SIZE * 8; y >= 0; y--) {
            short block = chunkManager.getBlock(ix, y, iz);
            if (block != 0) {
//                System.out.println("=== TERRAIN FOUND at Y=" + y + " block=" + block);
                return y + 1;
            }
        }

//        System.out.println("=== WARNING: no terrain found at (" + ix + "," + iz + ")");
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
    public void simpleUpdate(float tpf) {
        Vector3f playerPos = cam.getLocation();
        // -- Skybox suit la caméra --
        skyManager.update(cam.getLocation());
        // -- Cycle jour/nuit --
        dayNightCycle.setTimeOfDay(0.0f);  // midi
        dayNightCycle.setTimeScale(0f);     // temps gelé
        dayNightCycle.update(tpf);

        // -- Chunks --
        if (playerPos.distance(lastUpdatePos) > CHUNK_UPDATE_THRESHOLD) {
            updateVisibleChunks(playerPos);
            lastUpdatePos = playerPos.clone();
        }

        // -- Joueur --
        playerControl.update(tpf);
        playerInteraction.update(tpf);

        // -- Stamina --
        boolean sprinting = playerControl.isSprinting();
        boolean mining = playerInteraction.isMining();
        boolean resting = !sprinting && !mining;

        StaminaSystem.Activity activity;
        if (sprinting && staminaSystem.canSprint()) {
            activity = StaminaSystem.Activity.SPRINT;
        } else if (mining) {
            activity = StaminaSystem.Activity.WALK;
        } else {
            activity = StaminaSystem.Activity.IDLE;
        }

        staminaSystem.update(tpf, activity);

        // Sprint bloqué si exhausted
        if (!staminaSystem.canSprint()) {
            playerControl.setSprintAllowed(false);
            playerControl.setSprintMultiplier(1.0f);
        } else {
            playerControl.setSprintAllowed(true);
            if (sprinting) {
                playerControl.setSprintMultiplier(1.8f);
            } else {
                playerControl.setSprintMultiplier(1.0f);
            }
        }

        // -- UI --
        staminaBar.update((float) staminaSystem.getPercent());
        if (playerControl.isInventoryOpen()) {
            invScreen.refresh(inventory);
            return; // bloquer le mouvement
        }

        if (playerInteraction.isMining()) {
            miningBar.show();
            miningBar.update((float) playerInteraction.getMiningProgress());
        } else {
            miningBar.hide();
        }

        // -- Remesh chunks dirty --
        for (Spatial child : worldNode.getChildren()) {
            Integer cx = child.getUserData("cx");
            if (cx == null) continue;

            int cy = (int) child.getUserData("cy");
            int cz = (int) child.getUserData("cz");
            ChunkPos pos = new ChunkPos(cx, cy, cz);

            Chunk chunk = chunkManager.getChunk(pos);
            if (chunk == null || !chunk.isDirty()) continue;

            Mesh newMesh = ChunkMeshBuilder.buildMesh(chunk, chunkManager);

            if (newMesh == null) {
                RigidBodyControl rbc = child.getControl(RigidBodyControl.class);
                if (rbc != null) bulletAppState.getPhysicsSpace().remove(rbc);
                worldNode.detachChild(child);
            } else {
                ((Geometry) child).setMesh(newMesh);

                RigidBodyControl oldRbc = child.getControl(RigidBodyControl.class);
                if (oldRbc != null) {
                    bulletAppState.getPhysicsSpace().remove(oldRbc);
                    child.removeControl(oldRbc);
                }
                RigidBodyControl newRbc = new RigidBodyControl(
                        new MeshCollisionShape(newMesh), 0f
                );
                child.addControl(newRbc);
                bulletAppState.getPhysicsSpace().add(newRbc);
            }

            chunk.markClean();
        }

        gameHUD.update(playerPos, planetData);
        // Audio
        ostManager.update(tpf);   // gère les crossfades
//        sfxManager.updateEnvironmentSounds(tpf, (float) planetData.getSurfacePressure());
    }

    /**
     * Met à jour les chunks visibles autour du joueur.
     *
     * 1. Calculer quels chunks doivent être chargés
     * 2. Générer ceux qui ne le sont pas encore
     * 3. Construire leur mesh et les ajouter à la scène
     * 4. Retirer les chunks trop loin
     */
    private void updateVisibleChunks(Vector3f playerPos) {
        int pcx = Math.floorDiv((int) playerPos.x, Chunk.SIZE);
        int pcy = Math.floorDiv((int) playerPos.y, Chunk.SIZE);
        int pcz = Math.floorDiv((int) playerPos.z, Chunk.SIZE);

        // 1. Collecter les positions qui DOIVENT être visibles
        Set<ChunkPos> needed = new HashSet<>();
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                    needed.add(new ChunkPos(pcx + dx, pcy + dy, pcz + dz));
                }
            }
        }

        // 2. Retirer les chunks qui ne sont PLUS nécessaires
        Iterator<Spatial> it = worldNode.getChildren().iterator();
        while (it.hasNext()) {
            Spatial child = it.next();
            ChunkPos pos = (ChunkPos) child.getUserData("chunkPos");
            if (pos != null && !needed.contains(pos)) {
                RigidBodyControl rbc = child.getControl(RigidBodyControl.class);
                if (rbc != null) bulletAppState.getPhysicsSpace().remove(rbc);
                worldNode.detachChild(child);
            }
        }

        // 3. Ajouter les chunks manquants
        for (ChunkPos pos : needed) {
            if (worldNode.getChild("chunk_" + pos) != null) continue; // déjà affiché

            Chunk chunk = chunkManager.getOrGenerate(pos);
            if (chunk == null) continue;

            Mesh mesh = ChunkMeshBuilder.buildMesh(chunk, chunkManager);
            if (mesh == null) continue;

            Geometry geom = new Geometry("chunk_" + pos, mesh);
            geom.setMaterial(blockMaterial);
            geom.setLocalTranslation(
                    pos.x * Chunk.SIZE,
                    pos.y * Chunk.SIZE,
                    pos.z * Chunk.SIZE
            );
//            // Stocker la position pour le nettoyage
//            geom.setUserData("chunkPos", pos);

            worldNode.attachChild(geom);
            geom.setUserData("cx", pos.x);
            geom.setUserData("cy", pos.y);
            geom.setUserData("cz", pos.z);
            RigidBodyControl rb = new RigidBodyControl(new MeshCollisionShape(mesh), 0f);
            geom.addControl(rb);
            bulletAppState.getPhysicsSpace().add(rb);
        }
    }
}
