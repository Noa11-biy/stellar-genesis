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
import com.stellargenesis.client.screens.PauseScreenState;
import com.stellargenesis.client.screens.TitleScreenState;
import com.stellargenesis.client.ui.*;
import com.stellargenesis.core.inventory.Inventory;
import com.stellargenesis.core.player.MiningSystem;
import com.stellargenesis.core.world.*;
import com.stellargenesis.core.physics.PlanetData;
import com.stellargenesis.core.physics.PlanetPhysics;

import java.util.*;

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
    private boolean paused = false;

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

    private boolean gameInitialized = false;
    private boolean chunksReady = false;
    private float spawnY;

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
//        settings.setResolution(1920, 1080);
//        settings.setFullscreen(true);
        settings.setVSync(false);
        settings.setFrameRate(-1);
        settings.setSamples(4);


        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }


    @Override
    public void simpleInitApp() {
        inputManager.deleteMapping(INPUT_MAPPING_EXIT);
        /*
         * AVANT : tout le jeu s'initialisait ici.
         * MAINTENANT : on affiche juste l'écran titre.
         * Le jeu se lance quand le joueur clique "Solo".
         *
         * Pourquoi ce changement ?
         * → Le joueur voit un menu propre au lancement
         * → On peut ajouter "Continuer", "Paramètres" etc.
         * → La musique du menu joue pendant que le joueur réfléchit
         * → initGame() n'est appelé qu'une fois, au bon moment
         */

        // Désactiver la caméra libre (on est sur un menu, pas en jeu)
        flyCam.setEnabled(false);
        flyCam.setDragToRotate(false);

        setDisplayStatView(true);
        setDisplayFps(true);

        // Supprimer le mapping par défaut de flyCam
        // sinon il capture la souris même sur le menu
        if (inputManager.hasMapping("FLYCAM_RotateDrag")) {
            inputManager.deleteMapping("FLYCAM_RotateDrag");
        }

        ostManager = new OSTManager(assetManager, rootNode);

        ostManager.registerCategory("menu", 0.4f,
                "Sounds/OST/menu/First_Step_Into_The_Void.ogg");

        ostManager.registerCategory("exploration", 0.4f,
                "Sounds/OST/exploration/Crossing_The_Last_Frontier.ogg",
                "Sounds/OST/exploration/Under_A_Cold_Sun.ogg"
        );

        ostManager.playCategory("menu");

        // Attacher l'écran titre — c'est lui qui gère tout
        stateManager.attach(new TitleScreenState());
    }

    /**
     * Initialise le jeu complet.
     *
     * Appelé par TitleScreenState quand le joueur clique "Solo".
     * Contient TOUT le code qui était avant dans simpleInitApp().
     *
     * Cette méthode est publique car TitleScreenState doit y accéder
     * depuis un autre package (screens/).
     */
    public void initGame() {
        if (gameInitialized) return; // sécurité anti double-init
        gameInitialized = true;

        // Le curseur redevient invisible (on est en FPS maintenant)
        inputManager.setCursorVisible(false);

        // ════════════════════════════════════════════
        //  Tout ton code d'initialisation existant :
        // ════════════════════════════════════════════

        // 1. Physique Bullet
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        // 2. Données planétaires
        initPlanetData();

        // 3. Gravité
        bulletAppState.getPhysicsSpace().setGravity(
                new Vector3f(0, -(float) planetData.getSurfaceGravity(), 0));

        // 4. Monde
        initWorld();

        // 5. Rendu
        initMaterial();
        initLighting();

        // 5b. Skybox
        skyManager = new SkyManager();
        skyManager.init(assetManager, rootNode, planetData);

        // 5c. Cycle jour/nuit
        dayNightCycle = new DayNightCycle();
        dayNightCycle.init(planetData, sunLight, ambientLight, skyManager);
        dayNightCycle.setTimeScale(10f);

        // 6. Caméra
        initCamera();

        // 7. Chunks initiaux
//        loadInitialChunks();

        // 8. Joueur
        spawnY = findTerrainHeight(32, 32) + 3f;

        // Lancer le chargement async du reste
        chunkManager.update(32, (int) spawnY, 32);


        staminaSystem = new StaminaSystem(100.0, planetData.getSurfaceGravity());
        float planetGravity = (float) planetData.getSurfaceGravity();
        playerControl = new PlayerControl(
                this,
                rootNode, bulletAppState, cam, inputManager,
                planetGravity, spawnY, staminaSystem
        );

        preloadSpawnChunks();
        System.out.println("=== SPAWN Y: " + spawnY);
        playerControl.teleportTo(new Vector3f(32, spawnY, 32));
        chunksReady = true;

        chunkManager.update(32, (int) spawnY, 32);
        loadInitialChunks();

        inputManager.setCursorVisible(false);
        mouseInput.setCursorVisible(false);

        try {
            robot = new java.awt.Robot();
        } catch (Exception e) {
            e.printStackTrace();
        }
        centerX = settings.getWidth() / 2;
        centerY = settings.getHeight() / 2;

        if (robot != null) {
            if (context instanceof com.jme3.system.lwjgl.LwjglWindow) {
                robot.mouseMove(
                        settings.getWidth() / 2 + getContext().getSettings().getWindowXPosition(),
                        settings.getHeight() / 2 + getContext().getSettings().getWindowYPosition()
                );
            }
        }

        MiningSystem miningSystem = new MiningSystem(
                1.0, planetData.getSurfaceGravity(), 100.0
        );

        inventory = new Inventory(36, 80.0, planetData.getSurfaceGravity());
        inventory.addItem(BlockType.STONE, 64);

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
        invScreen.build(inventory.getSize());
        playerControl.setInventory(inventory);
        playerControl.setInventoryScreen(invScreen);

        // 10. Audio du jeu (pas du menu)
        ostManager.crossfadeTo("exploration", 2.0f);

        System.out.println("=== STELLAR GENESIS — GAME STARTED ===");
        System.out.println("Planète : masse=" +
                String.format("%.2e", planetData.getMass()) + " kg");
        System.out.println("Gravité : " +
                String.format("%.2f", planetData.getSurfaceGravity()) + " m/s²");
        System.out.println("Température : " +
                String.format("%.1f", planetData.getEquilibriumTemp()) + " K");
        System.out.println("Pression : " +
                String.format("%.3f", planetData.getSurfacePressure()) + " bar");
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

        int chunkX = Math.floorDiv(ix, Chunk.SIZE);
        int chunkZ = Math.floorDiv(iz, Chunk.SIZE);

        // Générer les chunks de la colonne de spawn directement (appelé UNE FOIS au spawn)
        for (int cy = 0; cy <= 8; cy++) {
            ChunkPos pos = new ChunkPos(chunkX, cy, chunkZ);

            // Si pas encore chargé → générer maintenant sur le thread principal
            if (chunkManager.getChunk(pos) == null) {
                Chunk chunk = new Chunk(pos);
                worldGenerator.generateChunk(chunk);
                chunkManager.forceInsert(pos, chunk); // ← à ajouter dans ChunkManager
            }
        }

        // Chercher le premier bloc solide de haut en bas
        for (int y = Chunk.SIZE * 8; y >= 0; y--) {
            short block = chunkManager.getBlock(ix, y, iz);
            if (block != 0) {
                return y + 1;
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
    public void simpleUpdate(float tpf) {
        if (paused) return;
        if (!gameInitialized) return;

        // ═══════════════════════════════════════════
        //  PHASE 1 — POMPER LA QUEUE DE CHUNKS
        //  Toujours fait, même si chunksReady = false
        // ═══════════════════════════════════════════
        {
            int limit = chunksReady ? 4 : Integer.MAX_VALUE; // Au démarrage : tout vider
            int attached = 0;
            ChunkManager.ChunkMeshPair pair;
            while (attached < limit && (pair = chunkManager.getReadyQueue().poll()) != null) {
                attachChunk(pair.pos(), pair.mesh());
                attached++;
            }
        }

        // ═══════════════════════════════════════════
        //  PHASE 2 — ATTENDRE QUE LE SOL EXISTE
        //  Bloquer la logique joueur jusqu'aux chunks de spawn
        // ═══════════════════════════════════════════

        int attached = 0;
        ChunkManager.ChunkMeshPair pair;
        while (attached < 4 && (pair = chunkManager.getReadyQueue().poll()) != null) {
            attachChunk(pair.pos(), pair.mesh());
            attached++;
        }

        // ═══════════════════════════════════════════
        //  PHASE 3 — LOGIQUE JOUEUR
        // ═══════════════════════════════════════════
        if (playerControl != null) playerControl.update(tpf);
        if (playerInteraction != null) playerInteraction.update(tpf);

        Vector3f playerPos = cam.getLocation();

        // ═══════════════════════════════════════════
        //  PHASE 4 — STAMINA
        // ═══════════════════════════════════════════
        {
            boolean sprinting = playerControl.isSprinting();
            boolean mining    = playerInteraction.isMining();

            StaminaSystem.Activity activity;
            if (sprinting && staminaSystem.canSprint()) {
                activity = StaminaSystem.Activity.SPRINT;
            } else if (mining) {
                activity = StaminaSystem.Activity.WALK;
            } else {
                activity = StaminaSystem.Activity.IDLE;
            }

            staminaSystem.update(tpf, activity);

            if (!staminaSystem.canSprint()) {
                playerControl.setSprintAllowed(false);
                playerControl.setSprintMultiplier(1.0f);
            } else {
                playerControl.setSprintAllowed(true);
                playerControl.setSprintMultiplier(sprinting ? 1.8f : 1.0f);
            }
        }

        // ═══════════════════════════════════════════
        //  PHASE 5 — SKYBOX & CYCLE JOUR/NUIT
        // ═══════════════════════════════════════════
        skyManager.update(playerPos);
        dayNightCycle.setTimeOfDay(0.0f);
        dayNightCycle.setTimeScale(0f);
        dayNightCycle.update(tpf);

        // ═══════════════════════════════════════════
        //  PHASE 6 — MISE À JOUR DES CHUNKS VISIBLES
        // ═══════════════════════════════════════════
        if (playerPos.distance(lastUpdatePos) > CHUNK_UPDATE_THRESHOLD) {
            updateVisibleChunks(playerPos);
            lastUpdatePos = playerPos.clone();
        }

        // ═══════════════════════════════════════════
        //  PHASE 7 — REMESH DES CHUNKS DIRTY
        //  (blocs minés, placés, modifiés)
        // ═══════════════════════════════════════════
        {
            List<Spatial> children = new ArrayList<>(worldNode.getChildren());
            for (Spatial child : children) {
                Integer cx = child.getUserData("cx");
                if (cx == null) continue;

                int cy  = (int) child.getUserData("cy");
                int cz  = (int) child.getUserData("cz");
                ChunkPos pos = new ChunkPos(cx, cy, cz);

                Chunk chunk = chunkManager.getChunk(pos);
                if (chunk == null || !chunk.isDirty()) continue;

                Mesh newMesh = ChunkMeshBuilder.buildMesh(chunk, chunkManager);

                if (newMesh == null) {
                    // Chunk vide → retirer de la scène
                    RigidBodyControl rbc = child.getControl(RigidBodyControl.class);
                    if (rbc != null) bulletAppState.getPhysicsSpace().remove(rbc);
                    worldNode.detachChild(child);
                } else {
                    // Mettre à jour le mesh et la physique
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
        }

        // ═══════════════════════════════════════════
        //  PHASE 8 — UI
        // ═══════════════════════════════════════════
        if (ostManager != null) ostManager.update(tpf);

        staminaBar.update((float) staminaSystem.getPercent());

        if (playerControl.isInventoryOpen()) {
            invScreen.refresh(inventory);
            return; // Ne pas mettre à jour le reste de l'UI
        }

        if (playerInteraction.isMining()) {
            miningBar.show();
            miningBar.update((float) playerInteraction.getMiningProgress());
        } else {
            miningBar.hide();
        }

        gameHUD.update(playerPos, planetData);
    }

    private void attachChunk(ChunkPos pos, com.jme3.scene.Mesh mesh) {
        // Vérifier qu'il n'est pas déjà attaché (sécurité)
        if (worldNode.getChild("chunk_" + pos) != null) return;

        Geometry geom = new Geometry("chunk_" + pos, mesh);
        geom.setMaterial(blockMaterial);
        geom.setLocalTranslation(
                pos.x * Chunk.SIZE,
                pos.y * Chunk.SIZE,
                pos.z * Chunk.SIZE
        );
        geom.setUserData("cx", pos.x);
        geom.setUserData("cy", pos.y);
        geom.setUserData("cz", pos.z); // un seul userData

        RigidBodyControl rb = new RigidBodyControl(
                new MeshCollisionShape(mesh), 0f
        );
        geom.addControl(rb);
        bulletAppState.getPhysicsSpace().add(rb);
        worldNode.attachChild(geom);
    }

    private void preloadSpawnChunks() {
        int pcx = Math.floorDiv(32, Chunk.SIZE);
        int pcz = Math.floorDiv(32, Chunk.SIZE);
        int spawnChunkY = Math.floorDiv((int) spawnY, Chunk.SIZE);

        System.out.println("[PRELOAD] Génération synchrone des chunks de spawn...");

        // Générer synchroniquement UNIQUEMENT les chunks autour du spawn (3x3x4)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    ChunkPos pos = new ChunkPos(pcx + dx, spawnChunkY + dy, pcz + dz);

                    if (chunkManager.getChunk(pos) != null) continue;

                    // Synchrone — on attend que ce soit fait
                    Chunk chunk = new Chunk(pos);
                    worldGenerator.generateChunk(chunk);
                    chunkManager.forceInsert(pos, chunk);

                    Mesh mesh = ChunkMeshBuilder.buildMesh(chunk, chunkManager);
                    if (mesh != null) {
                        attachChunk(pos, mesh);
                    }
                }
            }
        }

        System.out.println("[PRELOAD] Chunks de spawn prêts !");
    }

    public void togglePause() {
        if (!gameInitialized) return;
        if (paused) resumeGame();
        else pauseGame();
    }

    public void pauseGame() {
        if (paused) return;
        paused = true;

        playerControl.setEnabled(false);
        bulletAppState.setEnabled(false);
        inputManager.setCursorVisible(true);

        stateManager.attach(new PauseScreenState());
        System.out.println("[App] Pause.");
    }

    public void resumeGame() {
        if (!paused) return;
        paused = false;

        PauseScreenState pause = stateManager.getState(PauseScreenState.class);
        if (pause != null) stateManager.detach(pause);

        playerControl.setEnabled(true);
        bulletAppState.setEnabled(true);
        inputManager.setCursorVisible(false);
        System.out.println("[App] Reprise.");
    }

    public void backToTitle() {
        System.out.println("[App] Retour menu.");

        // 1. Retirer pause
        PauseScreenState pause = stateManager.getState(PauseScreenState.class);
        if (pause != null) stateManager.detach(pause);
        paused = false;

        // Vérifier s'il y a déjà un TitleScreenState attaché
        TitleScreenState existing = stateManager.getState(TitleScreenState.class);
        if (existing != null) {
            stateManager.detach(existing);
        }

        // 2. Nettoyer les inputs du joueur (délégué à PlayerControl)
        if (playerControl != null) {
            playerControl.cleanup();
            playerControl = null;
        }

        // 3. Détacher la physique
        if (bulletAppState != null) {
            stateManager.detach(bulletAppState);
            bulletAppState = null;
        }

        // 4. Nettoyer la scène
        rootNode.detachAllChildren();
        guiNode.detachAllChildren();

        if (sunLight != null) rootNode.removeLight(sunLight);
        if (ambientLight != null) rootNode.removeLight(ambientLight);

        gameInitialized = false;

        // Remettre musique du menu
        if (ostManager != null) {
            ostManager.playCategory("menu");
        }

        // 5. Relancer l'écran titre
        stateManager.attach(new TitleScreenState());
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

        // 1. Collecter les positions nécessaires
        Set<ChunkPos> needed = new HashSet<>();
        for (int dx = -renderDistance; dx <= renderDistance; dx++)
            for (int dy = -2; dy <= 4; dy++)
                for (int dz = -renderDistance; dz <= renderDistance; dz++)
                    needed.add(new ChunkPos(pcx+dx, pcy+dy, pcz+dz));

        // 2. Retirer les chunks hors rayon — copie AVANT de modifier
        List<Spatial> toDetach = new ArrayList<>();
        for (Spatial child : worldNode.getChildren()) {
            ChunkPos pos = child.getUserData("chunkPos");
            if (pos != null && !needed.contains(pos))
                toDetach.add(child);
        }
        for (Spatial child : toDetach) {
            RigidBodyControl rbc = child.getControl(RigidBodyControl.class);
            if (rbc != null) bulletAppState.getPhysicsSpace().remove(rbc);
            worldNode.detachChild(child);
        }

        // 3. Demander les chunks manquants — NON BLOQUANT
        chunkManager.update(
                (int) playerPos.x,
                (int) playerPos.y,
                (int) playerPos.z
        );
    }

    public PlayerControl getPlayerControl() { return playerControl; }
    public PlayerInteraction getPlayerInteraction() { return playerInteraction; }
    public BulletAppState getBulletAppState() { return bulletAppState; }
}
