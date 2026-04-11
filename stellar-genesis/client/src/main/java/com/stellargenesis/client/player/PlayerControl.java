package com.stellargenesis.client.player;

import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.event.*;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

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
    private boolean sprintAllowed = true;
    private StaminaSystem staminaSystem;


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

    // === État du joueur ===
    private boolean forward, backward, left, right;
    private boolean sprinting;
    private float yaw = 0;      // rotation horizontale (gauche/droite)
    private float pitch = 0;    // rotation verticale (haut/bas)

    /**
     * @param gravity gravité de surface en m/s² (ex: 9.81 pour Terre, 3.72 pour Mars)
     */
    public PlayerControl(Node rootNode, BulletAppState bulletState,
                         Camera cam, InputManager inputManager, float gravity, float spawnY, StaminaSystem staminaSystem){
        this.cam = cam;
        this.inputManager = inputManager;
        this.gravity = gravity;
        this.staminaSystem = staminaSystem;

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

        // Enregistrer les touches
        setupKeys();
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

    /**
     * Enregistrement des touches ZQSD + Espace + Shift + Souris
     */
    private void setupKeys(){
        // Mappings clavier
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Sprint", new KeyTrigger(KeyInput.KEY_LSHIFT));

        // Listener pour les touches (pressed/released)
        inputManager.addListener(actionListener,
                "Forward", "Backward", "Left", "Right", "Jump", "Sprint");

        // === SOURIS — RawInputListener (capture les deltas bruts) ===
        inputManager.addRawInputListener(new RawInputListener() {
            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {
                float dx = evt.getDX();
                float dy = evt.getDY();
                yaw   -= dx * MOUSE_SENSITIVITY * 0.002f;
                pitch += dy * MOUSE_SENSITIVITY * 0.002f;
                pitch = Math.max(-1.5f, Math.min(1.5f, pitch));
                inputManager.setCursorVisible(false);
            }

            @Override public void beginInput() {}
            @Override public void endInput() {}
            @Override public void onJoyAxisEvent(JoyAxisEvent evt) {}
            @Override public void onJoyButtonEvent(JoyButtonEvent evt) {}
            @Override public void onMouseButtonEvent(MouseButtonEvent evt) {}
            @Override public void onKeyEvent(KeyInputEvent evt) {}
            @Override public void onTouchEvent(TouchEvent evt) {}
        });
    }

    /**
     * ActionListener : détecte appui ET relâchement des touches.
     * isPressed=true quand on appuie, false quand on relâche.
     */
    private final ActionListener actionListener = (name, isPressed, tpf) -> {
        switch (name){
            case "Forward": forward = isPressed; break;
            case "Backward": backward = isPressed; break;
            case "Left": left = isPressed; break;
            case "Right": right = isPressed; break;
            case "Sprint": sprinting = isPressed; break;
            case "Jump":
                if (isPressed && characterControl.isOnGround()) {
                    if (staminaSystem.tryJump()) {
                        characterControl.jump();
                    }
                }
                break;
        }
    };

    /**
    * AnalogListener : reçoit la valeur du mouvement de souris.
    * value = combien la souris a bougé ce frame.
     */
    private final AnalogListener analogListener = (name, value, tpf) -> {
        switch (name){
            case "MouseLeft" : yaw += value * MOUSE_SENSITIVITY; break;
            case "MouseRight" : yaw -= value * MOUSE_SENSITIVITY; break;
            case "MouseUp" : pitch += value * MOUSE_SENSITIVITY; break;
            case "MouseDown" : pitch -= value * MOUSE_SENSITIVITY; break;
        }
        // Limiter le pitch pour pas retourner la caméra
        pitch = Math.max(-1.5f, Math.min(1.5f, pitch)); // ~±85°
    };

    public void setSprintAllowed(boolean allowed) {
        this.sprintAllowed = allowed;
        if (!allowed && sprinting) sprinting = false;
    }


    /**
     * Appelé chaque frame depuis simpleUpdate().
     *
     * Le déplacement est calculé RELATIVEMENT à la direction de la caméra.
     * → "Forward" = dans la direction où le joueur regarde
     * → Pas de déplacement absolu (sinon Z irait toujours au nord)
     */
    public void update(float tpf){
        // 1. Mettre à jour la rotation caméra
        cam.lookAtDirection(getCamDirection(), Vector3f.UNIT_Y);

        // 2. Calculer la direction de déplacement
        Vector3f walkDirection = new Vector3f(0, 0, 0);

        // Direction caméra projetée sur le plan horizontal (ignorer Y)
        Vector3f camDir = cam.getDirection().clone().setY(0).normalizeLocal();
        Vector3f camLeft = cam.getLeft().clone().setY(0).normalizeLocal();

        float speed = sprinting ? BASE_SPRINT_SPEED * sprintMultiplier : walkSpeed;


        if (forward) walkDirection.addLocal(camDir);
        if (backward) walkDirection.addLocal(camDir.negate());
        if (left) walkDirection.addLocal(camLeft);
        if (right) walkDirection.addLocal(camLeft.negate());

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
        System.out.println("PlayerY=" + playerNode.getWorldTranslation().y);
    }

    public boolean isMoving() {
        return forward || backward || left || right;
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

    // === Getters pour le HUD futur ===
    public Vector3f getPosition() { return playerNode.getWorldTranslation(); }
    public float getGravity() { return gravity; }
    public float getWalkSpeed() { return walkSpeed; }
    public float getJumpHeight() {
        return (jumpImpulse * jumpImpulse) / (2 * gravity);
    }
    public void setSprintMultiplier(float m) { this.sprintMultiplier = m; }
    public boolean isSprinting() { return sprinting; }
}