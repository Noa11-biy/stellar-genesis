package com.stellargenesis.client.audio;

import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.stellargenesis.core.math.MathUtils;

public class SFXManager {

    // --- Ambiance ---
    private AudioNode windSound;
    private AudioNode rumbleSound;      // grondement basse pression / volcanique

    // --- Joueur ---
    private AudioNode footstepSound;
    private AudioNode mineSound;

    // --- État ---
    private float timeElapsed = 0f;
    private float footstepTimer = 0f;
    private float footstepInterval = 0.5f; // secondes entre chaque pas
    private boolean playerMoving = false;
    private boolean playerMining = false;

    // --- Référence ---
    private Node rootNode;

    public SFXManager(AssetManager assetManager, Node rootNode) {
        this.rootNode = rootNode;

        // ===== VENT (boucle continue, volume modulé) =====
        windSound = new AudioNode(assetManager, "Sounds/SFX/white_noise_loop.ogg", DataType.Buffer);
        windSound.setLooping(true);
        windSound.setPositional(false);
        windSound.setVolume(0f);
        rootNode.attachChild(windSound);
        windSound.play();

        // ===== GRONDEMENT ATMOSPHÉRIQUE (planètes denses) =====
        rumbleSound = new AudioNode(assetManager, "Sounds/SFX/rumble_loop.ogg", DataType.Buffer);
        rumbleSound.setLooping(true);
        rumbleSound.setPositional(false);
        rumbleSound.setVolume(0f);
        rootNode.attachChild(rumbleSound);
        rumbleSound.play();

        // ===== PAS (joué ponctuellement) =====
        footstepSound = new AudioNode(assetManager, "Sounds/SFX/footstep.ogg", DataType.Buffer);
        footstepSound.setLooping(false);
        footstepSound.setPositional(false);
        footstepSound.setVolume(0.4f);

        // ===== MINAGE =====
        mineSound = new AudioNode(assetManager, "Sounds/SFX/mine_hit.ogg", DataType.Buffer);
        mineSound.setLooping(false);
        mineSound.setPositional(false);
        mineSound.setVolume(0.6f);
    }

    // =========================================================
    //  UPDATE PRINCIPAL — appelé chaque frame
    // =========================================================
    public void update(float tpf, float pressure, float gravity) {
        timeElapsed += tpf;

        updateWind(tpf, pressure);
        updateRumble(pressure);
        updateFootsteps(tpf, gravity);
    }

    // =========================================================
    //  VENT — bruit Simplex 1D pour les rafales
    // =========================================================
    private void updateWind(float tpf, float pressure) {
        if (pressure <= 0.01f) {
            windSound.setVolume(0f);
            return;
        }

        // Bruit Simplex 1D → rafales naturelles, pas un simple sinus
        // Fréquence lente (0.3) = grandes rafales
        // Fréquence rapide (1.7) = micro-turbulences
        float gustSlow = (float) MathUtils.noise1D(timeElapsed * 0.3);   // -1..1
        float gustFast = (float) MathUtils.noise1D(timeElapsed * 1.7);   // -1..1
        float gust = gustSlow * 0.7f + gustFast * 0.3f;           // combiné
        float gustFactor = 1.0f + gust * 0.5f;                    // 0.5..1.5

        // Volume proportionnel à la pression (atmosphère dense = vent fort)
        // clamp entre 0 et 1
        float volume = (float) MathUtils.clamp(pressure * 0.1f * gustFactor, 0f, 1f);

        windSound.setVolume(volume);
        windSound.setPitch((float) MathUtils.clamp(gustFactor, 0.5f, 2.0f));
    }

    // =========================================================
    //  GRONDEMENT — planètes à haute pression (>2 bar)
    // =========================================================
    private void updateRumble(float pressure) {
        if (pressure < 2.0f) {
            rumbleSound.setVolume(0f);
            return;
        }
        // Monte progressivement de 2 bar à 10 bar
        float intensity = (float) MathUtils.clamp((pressure - 2.0f) / 8.0f, 0f, 0.7f);
        rumbleSound.setVolume(intensity);
    }

    // =========================================================
    //  PAS — intervalle et pitch selon la gravité
    // =========================================================
    private void updateFootsteps(float tpf, float gravity) {
        if (!playerMoving) return;

        footstepTimer += tpf;

        // Gravité faible → pas espacés (on flotte plus longtemps)
        // Gravité forte → pas rapprochés (on retombe vite)
        // Ratio par rapport à g_Terre = 9.81
        float gRatio = gravity / 9.81f;
        float interval = (float) (footstepInterval / MathUtils.clamp(gRatio, 0.3f, 3.0f));

        if (footstepTimer >= interval) {
            footstepTimer = 0f;

            // Pitch : grave sur planète lourde, aigu sur lune
            float pitch = (float) MathUtils.clamp(1.0f / (float) Math.sqrt(gRatio), 0.6f, 1.5f);
            footstepSound.setPitch(pitch);

            // Volume légèrement plus fort si gravité élevée (impact plus lourd)
            footstepSound.setVolume((float) MathUtils.clamp(0.3f + gRatio * 0.15f, 0.2f, 0.8f));

            footstepSound.playInstance(); // joue une instance sans couper la précédente
        }
    }

    // =========================================================
    //  ACTIONS PONCTUELLES
    // =========================================================

    /** Appelé par le système de minage quand le joueur frappe un bloc */
    public void playMineHit(float hardness) {
        // Bloc dur → son grave et fort | Bloc mou → son aigu et faible
        float pitch = (float) MathUtils.clamp(2.0f - hardness / 5.0f, 0.7f, 1.5f);
        float volume = (float) MathUtils.clamp(0.3f + hardness * 0.1f, 0.3f, 1.0f);
        mineSound.setPitch(pitch);
        mineSound.setVolume(volume);
        mineSound.playInstance();
    }

    // =========================================================
    //  SETTERS — appelés par le système joueur
    // =========================================================

    public void setPlayerMoving(boolean moving) {
        this.playerMoving = moving;
        if (!moving) footstepTimer = 0f;
    }

    public void setPlayerMining(boolean mining) {
        this.playerMining = mining;
    }

    // =========================================================
    //  SILENCE TOTAL (espace, menu, etc.)
    // =========================================================
    public void muteAll() {
        windSound.setVolume(0f);
        rumbleSound.setVolume(0f);
    }
}
