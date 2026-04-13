package com.stellargenesis.client.audio;

import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;

import java.util.*;

/**
 * Gestionnaire de musique avec playlists par catégorie.
 *
 * Chaque catégorie contient plusieurs pistes qui tournent aléatoirement.
 *
 * Utilisation :
 *   ostManager.registerCategory("exploration", 0.4f,
 *       "Sounds/OST/horizons_of_dust.ogg",
 *       "Sounds/OST/silent_valleys.ogg",
 *       "Sounds/OST/wandering_light.ogg"
 *   );
 *   ostManager.playCategory("exploration");
 *   ostManager.crossfadeTo("combat", 2.0f);
 */
public class OSTManager {

    // --- Playlist d'une catégorie ---
    private static class Category {
        final String name;
        final float defaultVolume;
        final List<String> tracks;
        int lastPlayedIndex = -1;

        Category(String name, float defaultVolume, List<String> tracks) {
            this.name = name;
            this.defaultVolume = defaultVolume;
            this.tracks = tracks;
        }

        /**
         * Choisit une piste aléatoire différente de la précédente.
         * Évite de jouer deux fois la même piste d'affilée.
         */
        String pickNext(Random rng) {
            if (tracks.size() == 1) return tracks.get(0);

            int index;
            do {
                index = rng.nextInt(tracks.size());
            } while (index == lastPlayedIndex);

            lastPlayedIndex = index;
            return tracks.get(index);
        }
    }

    // --- État ---
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final AssetManager assetManager;
    private final Node rootNode;
    private final Random rng = new Random();

    private AudioNode currentNode;
    private String currentCategoryId;
    private String currentTrackPath;

    // Crossfade
    private AudioNode fadingOutNode;
    private float fadingOutVolume;
    private AudioNode fadingInNode;
    private float fadingInTargetVolume;
    private String fadingInCategoryId;
    private float fadeDuration;
    private float fadeTimer;
    private boolean fading = false;

    // Suivi de durée pour enchaîner les pistes
    private float trackTimer = 0f;
    private float trackDuration = 0f;
    private boolean autoAdvance = true;

    private float masterVolume = 1.0f;

    public OSTManager(AssetManager assetManager, Node rootNode) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
    }

    // ========================================================
    //  ENREGISTREMENT DES CATÉGORIES
    // ========================================================

    /**
     * Enregistre une catégorie avec ses pistes.
     *
     * @param categoryId    identifiant unique (ex: "exploration", "combat")
     * @param defaultVolume volume par défaut [0.0 - 1.0]
     * @param trackPaths    chemins des fichiers audio
     */
    public void registerCategory(String categoryId, float defaultVolume, String... trackPaths) {
        if (trackPaths.length == 0) {
            System.err.println("[OSTManager] Catégorie vide : " + categoryId);
            return;
        }
        categories.put(categoryId, new Category(
                categoryId, defaultVolume, Arrays.asList(trackPaths)
        ));
    }

    /**
     * Ajoute une piste à une catégorie existante.
     */
    public void addTrack(String categoryId, String trackPath) {
        Category cat = categories.get(categoryId);
        if (cat == null) {
            System.err.println("[OSTManager] Catégorie inexistante : " + categoryId);
            return;
        }
        cat.tracks.add(trackPath);
    }

    // ========================================================
    //  LECTURE
    // ========================================================

    /**
     * Joue immédiatement une piste aléatoire de la catégorie.
     */
    public void playCategory(String categoryId) {
        Category cat = categories.get(categoryId);
        if (cat == null) {
            System.err.println("[OST] Catégorie introuvable : " + categoryId);
            return;
        }

        System.out.println("[OST] === playCategory('" + categoryId + "') appelé ===");

        stopCurrentNode();

        String path = cat.pickNext(rng);
        System.out.println("[OST] Piste choisie : " + path);

        try {
            currentNode = createAudioNode(path, cat.defaultVolume);
            rootNode.attachChild(currentNode);
            currentNode.play();
            System.out.println("[OST] status=" + currentNode.getStatus());
            System.out.println("[OST] volume=" + currentNode.getVolume());
            System.out.println("[OST] masterVolume=" + masterVolume);


            currentCategoryId = categoryId;
            currentTrackPath = path;
            trackTimer = 0f;

            System.out.println("[OST] Lecture lancée — volume=" + (cat.defaultVolume * masterVolume)
                    + " status=" + currentNode.getStatus());
        } catch (Exception e) {
            System.err.println("[OST] ERREUR chargement : " + path);
            e.printStackTrace();
        }

    }

    /**
     * Transition douce vers une nouvelle catégorie.
     */
    public void crossfadeTo(String categoryId, float duration) {
        if (categoryId.equals(currentCategoryId)) return;

        Category cat = categories.get(categoryId);
        if (cat == null) {
            System.err.println("[OSTManager] Catégorie inconnue : " + categoryId);
            return;
        }

        // L'ancien part en fondu
        fadingOutNode = currentNode;
        if (currentCategoryId != null) {
            Category oldCat = categories.get(currentCategoryId);
            fadingOutVolume = oldCat != null ? oldCat.defaultVolume * masterVolume : 0f;
        }

        // Le nouveau arrive à volume 0
        String path = cat.pickNext(rng);
        fadingInNode = createAudioNode(path, 0f);
        fadingInTargetVolume = cat.defaultVolume * masterVolume;
        fadingInCategoryId = categoryId;

        rootNode.attachChild(fadingInNode);
        fadingInNode.play();

        fadeDuration = duration;
        fadeTimer = 0f;
        fading = true;

        System.out.println("[OST] Crossfade vers " + categoryId + " — piste : " + path);
    }


    /**
     * Passe à la piste suivante dans la même catégorie.
     * Utile si le joueur veut skip une piste.
     */
    public void skipToNext() {
        if (currentCategoryId == null) return;
        Category cat = categories.get(currentCategoryId);
        if (cat == null) return;

        stopCurrentNode();

        String path = cat.pickNext(rng);
        currentNode = createAudioNode(path, cat.defaultVolume);
        currentTrackPath = path;
        trackTimer = 0f;

        rootNode.attachChild(currentNode);
        currentNode.play();
    }

    // ========================================================
    //  UPDATE — appeler dans simpleUpdate(tpf)
    // ========================================================

    public void update(float tpf) {
        // --- Crossfade ---
        if (fading) {
            fadeTimer += tpf;
            float progress = Math.min(fadeTimer / fadeDuration, 1.0f);

            if (fadingOutNode != null) {
                fadingOutNode.setVolume(fadingOutVolume * (1.0f - progress));
            }
            if (fadingInNode != null) {
                fadingInNode.setVolume(fadingInTargetVolume * progress);
            }

            if (progress >= 1.0f) {
                if (fadingOutNode != null) {
                    fadingOutNode.stop();
                    fadingOutNode.removeFromParent();
                }

                currentNode = fadingInNode;
                currentCategoryId = fadingInCategoryId;
                trackTimer = 0f;

                fadingOutNode = null;
                fadingInNode = null;
                fadingInCategoryId = null;
                fading = false;
            }
        }

        // --- Auto-avance : quand une piste finit, jouer la suivante ---
        if (autoAdvance && currentNode != null && currentCategoryId != null) {
            // jME AudioNode en mode Stream n'expose pas facilement la durée
            // On vérifie le statut de lecture
            if (currentNode.getStatus() == com.jme3.audio.AudioSource.Status.Stopped) {
                advanceInCategory();
            }
        }
    }

    private void advanceInCategory() {
        Category cat = categories.get(currentCategoryId);
        if (cat == null) return;

        stopCurrentNode();

        String path = cat.pickNext(rng);
        currentNode = createAudioNode(path, cat.defaultVolume);
        currentTrackPath = path;
        trackTimer = 0f;

        rootNode.attachChild(currentNode);
        currentNode.play();
    }

    // ========================================================
    //  CONTRÔLES
    // ========================================================

    public void stop() {
        stopCurrentNode();
        currentCategoryId = null;
        currentTrackPath = null;
    }

    public void pause() {
        if (currentNode != null) currentNode.pause();
    }

    public void resume() {
        if (currentNode != null) currentNode.play();
    }

    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0f, Math.min(1f, volume));
        if (currentNode != null && currentCategoryId != null) {
            Category cat = categories.get(currentCategoryId);
            if (cat != null) {
                currentNode.setVolume(cat.defaultVolume * masterVolume);
            }
        }
    }

    public void setAutoAdvance(boolean auto) {
        this.autoAdvance = auto;
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public String getCurrentCategory() {
        return currentCategoryId;
    }

    public String getCurrentTrackPath() {
        return currentTrackPath;
    }

    public boolean isPlaying() {
        return currentNode != null && currentCategoryId != null;
    }

    /**
     * Retourne le nombre de pistes dans une catégorie.
     */
    public int getTrackCount(String categoryId) {
        Category cat = categories.get(categoryId);
        return cat != null ? cat.tracks.size() : 0;
    }

    /**
     * Liste toutes les catégories enregistrées.
     */
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(categories.keySet());
    }

    // ========================================================
    //  MÉTHODES PRIVÉES
    // ========================================================

    private AudioNode createAudioNode(String path, float volume) {
        AudioNode node = new AudioNode(assetManager, path, DataType.Stream);
        node.setLooping(false);  // pas de boucle : on enchaîne les pistes
        node.setPositional(false);
        node.setVolume(volume * masterVolume);
        return node;
    }

    private void stopCurrentNode() {
        if (currentNode != null) {
            currentNode.stop();
            currentNode.removeFromParent();
            currentNode = null;
        }
    }
}
