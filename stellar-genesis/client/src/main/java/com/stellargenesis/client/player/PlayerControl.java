package com.stellargenesis.client.player;

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.*;
import com.jme3.input.event.*;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.stellargenesis.client.StellarGenesisApp;
import com.stellargenesis.client.input.GameAction;
import com.stellargenesis.client.input.InputBindings;
import com.stellargenesis.client.input.InputContextManager;
import com.stellargenesis.client.render.FrustumDebugRenderer;
import com.stellargenesis.client.ui.InventoryScreen;
import com.stellargenesis.core.inventory.Inventory;

import java.util.Objects;

/**
 * Contrôle du joueur avec physique réaliste.
 *
 * La gravité affecte :
 * - La vitesse de chute (évident)
 * - La hauteur de saut : h = v0² / (2g)
 * - La vitesse de marche (pénalité si g élevé)
 *
 * BetterCharacterControl de jME gère :
 * - Collision avec le terrain
 * - Gravité appliquée chaque frame
 * - Détection du sol (isOnGround)
 */

public class PlayerControl {

    // === Physique du joueur ===
    private BetterCharacterControl characterControl;
    private Node playerNode;
    private Camera cam;
    private InputManager inputManager;
    private InputBindings inputBindings;
    private InputContextManager contextManager;
    private boolean sprintAllowed = true;
    private StaminaSystem staminaSystem;
    private Inventory inventory;


    // === Paramètres physiques ===
    private float gravity;              // m/s² de la planète
    private float walkSpeed;            // m/s, ajuste selon g
    private float jumpImpulse;          // m/s, vitesse initiale du saut
    private float sprintMultiplier = 1.0f;

    // === Constantes de base (à g = 9.81) ===
    private static final float BASE_WALK_SPEED = 5.0f;      // m/s sur Terre
    private static final float BASE_SPRINT_SPEED = 9.0f;
    private static final float BASE_JUMP_HEIGHT = 1.2f;     // mètres dur Terre
    private static final float MOUSE_SENSITIVITY = 1.0f;
    /** Multiplicateur pour la souris analogique jME (compense la normalisation interne). */
    private static final float MOUSE_AXIS_SCALE = 3f;

    // === État du joueur ===
    private boolean inventoryOpen = false;
    private float yaw = 0;      // rotation horizontale (gauche/droite)
    private float pitch = 0;    // rotation verticale (haut/bas)

    // === État actif/inactif ===
    private boolean enabled = true;

    // === Référence à l'app pour déclencher la pause ===
    private StellarGenesisApp app;

    private InventoryScreen inventoryScreen;
    private FrustumDebugRenderer frustumDebug;


    /**
     * @param gravity gravité de surface en m/s² (ex: 9.81 pour Terre, 3.72 pour Mars)
     */
    public PlayerControl(StellarGenesisApp app, Node rootNode, BulletAppState bulletState,
                         Camera cam, InputManager inputManager,
                         InputBindings inputBindings, InputContextManager contextManager,
                         float gravity, float spawnY, StaminaSystem staminaSystem){
        this.app = app;
        this.cam = cam;
        this.inputManager = inputManager;
        this.gravity = gravity;
        this.staminaSystem = staminaSystem;
        this.inputBindings = Objects.requireNonNull(inputBindings);
        this.contextManager = contextManager;

        // Calculer les paramètres adaptés à la gravité
        calculatePhysicsParams();

        // Créer le noeud joueur
        playerNode = new Node("Player");
        rootNode.attachChild(playerNode);

        // BetterCharacterControl(rayon, hauteur, masse)
        // Rayon 0.3m, hauteur 1.8m, masse 80kg
        characterControl = new BetterCharacterControl(0.3f, 1.8f, 80f);

        // Appliquer la gravité de la planète
        // setGravity attend un Vec3f (direction + magnitude)
        characterControl.setGravity(new Vector3f(0, -gravity, 0));

        // Vitesse de saut = v0 pour atteindre BASE_JUMP_HEIHGHT sous cette gravité
        // h = v0²/(2g) -> v0 = sqrt(2gh)
        characterControl.setJumpForce(new Vector3f(0, jumpImpulse * 80f, 0));

        playerNode.addControl(characterControl);
        bulletState.getPhysicsSpace().add(characterControl);

        // Positions initiala (au-dessus du terrain)
        characterControl.warp(new Vector3f(32, spawnY + 10, 32));

        inputManager.setCursorVisible(false);
    }

    /**
     * Calcule walkSpeed et jumpImpulse en fonction de la gravité.
     *
     * Vitesse de marche : on applique une pénalité si g > g_Terre
     *   walkSpeed = BASE × min(1, g_Terre / g)
     *   → Sur Mars (g=3.72) : on marche à vitesse normale
     *   → Sur Super-Terre (g=15) : on marche à 65% de la vitesse
     *
     * Saut : hauteur fixe en "effort", donc v0 = sqrt(2 × g × h_voulue)
     *   → Sur Mars : v0 plus petit mais g aussi → même hauteur
     *   → Sur Lune : v0 petit, g très petit → même hauteur en effort,
     *     mais on pourrait aussi garder v0 constant → saut plus haut
     *
     * Choix de design : v0 CONSTANT = sauts plus hauts sur planète légère
     *   v0 = sqrt(2 × g_Terre × BASE_JUMP_HEIGHT) ≈ 4.85 m/s
     *   Hauteur réelle = v0² / (2g) → sur Lune: 7.2m, sur Jupiter: 0.47m
     */
    private void calculatePhysicsParams(){
        float gRatio = 9.81f / gravity;

        // Vitesse de marche : pénalité si gravité forte
        if (gravity <= 9.81f){
            walkSpeed = BASE_WALK_SPEED;
        } else {
            walkSpeed = BASE_WALK_SPEED * Math.min(1.0f, gRatio);
        }

        // Saut : v0 constant (même force musculaire)
        // v0 = sqrt(2 x g_Terre x h_base)
        jumpImpulse = (float) Math.sqrt(2.2 * 9.81f * BASE_JUMP_HEIGHT);
        // ≈ 4.85 m/s

        System.out.println("=== Physique Joueur ===");
        System.out.println("Gravité planète : " + gravity + " m/s²");
        System.out.println("Vitesse marche  : " + walkSpeed + " m/s");
        System.out.println("Impulsion saut  : " + jumpImpulse + " m/s");
        float jumpHeight = (jumpImpulse * jumpImpulse) / (2 * gravity);
        System.out.println("Hauteur saut    : " + jumpHeight + " m");
    }

    public void setSprintAllowed(boolean allowed) {
        this.sprintAllowed = allowed;
    }

    public void teleportTo(Vector3f pos) {
        characterControl.warp(pos); // warp() = téléportation Bullet sans collision
    }


    public void setInventoryScreen(InventoryScreen screen) {
        this.inventoryScreen = screen;
    }


    public void toggleInventory() {
        inventoryOpen = !inventoryOpen;
        inputManager.setCursorVisible(inventoryOpen);

        if (inventoryOpen) {
            contextManager.pushContext(com.stellargenesis.client.input.InputContext.INVENTORY);
        } else {
            contextManager.popContext();   // dépile INVENTORY → retour à GAMEPLAY
        }

        if (inventoryScreen != null) {
            inventoryScreen.toggle();
        }

        System.out.println("[PlayerControl] Inventory " + (inventoryOpen ? "OPEN" : "CLOSED"));
    }


    /**
     * Appelé chaque frame depuis simpleUpdate().
     *
     * Le déplacement est calculé RELATIVEMENT à la direction de la caméra.
     * → "Forward" = dans la direction où le joueur regarde
     * → Pas de déplacement absolu (sinon Z irait toujours au nord)
     */
    public void update(float tpf){

        // ──────── Rotation caméra via axes souris ────────
        float lookX = inputBindings.consumeAxis(GameAction.LOOK_X);
        float lookY = inputBindings.consumeAxis(GameAction.LOOK_Y);

        yaw   -= lookX * MOUSE_SENSITIVITY * MOUSE_AXIS_SCALE;
        pitch += lookY * MOUSE_SENSITIVITY * MOUSE_AXIS_SCALE;
        pitch = Math.max(-1.5f, Math.min(1.5f, pitch));

        if (!enabled) return;

        // 1. Mettre à jour la rotation caméra
        cam.lookAtDirection(getCamDirection(), Vector3f.UNIT_Y);

        // 2. Calculer la direction de déplacement
        Vector3f walkDirection = new Vector3f(0, 0, 0);

        // Direction caméra projetée sur le plan horizontal (ignorer Y)
        Vector3f camDir = cam.getDirection().clone().setY(0).normalizeLocal();
        Vector3f camLeft = cam.getLeft().clone().setY(0).normalizeLocal();

        float speed = isSprinting() ? BASE_SPRINT_SPEED * sprintMultiplier : walkSpeed;


        if (inputBindings.isHeld(GameAction.MOVE_FORWARD))  walkDirection.addLocal(camDir);
        if (inputBindings.isHeld(GameAction.MOVE_BACKWARD)) walkDirection.addLocal(camDir.negate());
        if (inputBindings.isHeld(GameAction.MOVE_LEFT))     walkDirection.addLocal(camLeft);
        if (inputBindings.isHeld(GameAction.MOVE_RIGHT))    walkDirection.addLocal(camLeft.negate());

        // Normaliser pour pas aller plus vite en diagonale
        if (walkDirection.lengthSquared() > 0){
            walkDirection.normalizeLocal().multLocal(speed);
        }

        characterControl.setWalkDirection(walkDirection);

        // 3. Positionner la caméra sur le joueur (vue FPS)
        // playerNode.getWorldTranslation() = pieds du joueur
        // On ajoute 1.7 pour les yeux
        Vector3f eyePos = playerNode.getWorldTranslation().add(0, 1.6f, 0);
        cam.setLocation(eyePos);
//        System.out.println("PlayerY=" + playerNode.getWorldTranslation().y);

        if (inventoryOpen && inventoryScreen != null && inventoryScreen.isDragging()) {
            com.jme3.math.Vector2f mouse = inputManager.getCursorPosition();
            inventoryScreen.onMouseMove(mouse.x, mouse.y);
        }
    }

    public boolean isMoving() {
        return inputBindings.isHeld(GameAction.MOVE_FORWARD)
                || inputBindings.isHeld(GameAction.MOVE_BACKWARD)
                || inputBindings.isHeld(GameAction.MOVE_LEFT)
                || inputBindings.isHeld(GameAction.MOVE_RIGHT);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            characterControl.setWalkDirection(Vector3f.ZERO);
        }
    }

    /**
     * Déclenche un saut si le joueur est au sol.
     * L'impulsion appliquée dépend de la gravité de la planète (calculée dans
     * {@link #calculatePhysicsParams()}).
     */
    public void jump() {
        if (!enabled) return;
        if (characterControl.isOnGround()) {
            characterControl.jump();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Nettoie tous les mappings et listeners du joueur.
     * À appeler quand on quitte la partie.
     */
    public void cleanup() {
        enabled = false;
    }
    /**
     * Calcule la direction de la caméra à partir de yaw et pitch.
     *
     * Coordonnées sphériques → cartésiennes :
     *   x = cos(pitch) × sin(yaw)
     *   y = sin(pitch)
     *   z = cos(pitch) × cos(yaw)
     */
    private Vector3f getCamDirection(){
        float x = (float) (Math.cos(pitch) * Math.sin(yaw));
        float y = (float) Math.sin(pitch);
        float z = (float) (Math.cos(pitch) * Math.cos(yaw));
        return new Vector3f(x, y, z).normalizeLocal();
    }

    public void setInventory(Inventory inv) {
        this.inventory = inv;
    }

    // === Getters pour le HUD futur ===
    public Vector3f getPosition() { return playerNode.getWorldTranslation(); }
    public float getGravity() { return gravity; }
    public float getWalkSpeed() { return walkSpeed; }
    public float getJumpHeight() {
        return (jumpImpulse * jumpImpulse) / (2 * gravity);
    }
    public void setSprintMultiplier(float m) { this.sprintMultiplier = m; }
    public boolean isSprinting() {
        return sprintAllowed && inputBindings.isHeld(GameAction.SPRINT);
    }
    public boolean isInventoryOpen() { return inventoryOpen; }
}