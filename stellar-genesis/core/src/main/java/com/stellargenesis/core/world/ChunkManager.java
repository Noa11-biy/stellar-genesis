package com.stellargenesis.core.world;

import java.util.Map;
import java.util.concurrent.*;

/**
 * ChunkManager — Gère le cycle de vie des chunks.
 *
 * Responsabilités :
 *   1. Charger les chunks dans le rayon de rendu du joueur
 *   2. Décharger ceux qui sont trop loin
 *   3. Générer le terrain de manière asynchrone (pas de freeze)
 *
 * Fonctionnement :
 *   À chaque frame (ou tick), on regarde où est le joueur,
 *   on calcule dans quel chunk il se trouve, puis :
 *     - Pour chaque chunk dans le rayon : s'il manque → on le génère
 *     - Pour chaque chunk chargé hors rayon → on le décharge
 *
 * Pourquoi ConcurrentHashMap ?
 *   Parce que les chunks sont générés dans des threads séparés
 *   (ExecutorService) et insérés dans la map depuis ces threads.
 *   Une HashMap normale crasherait avec des accès concurrents.
 *
 * Pourquoi un ExecutorService ?
 *   Générer un chunk (bruit, biomes, minerais) prend du temps.
 *   Si on le fait sur le thread principal → le jeu freeze.
 *   Avec un pool de 4 threads, on génère 4 chunks en parallèle
 *   sans bloquer le rendu.
 */
public class ChunkManager {

    private final ConcurrentHashMap<ChunkPos, Chunk> loadedChunks;
    private final ExecutorService genPool;
    private final ConcurrentHashMap<ChunkPos, Boolean> pendingGeneration;
    private final WorldGenerator generator;
    private final int renderDistance;

    /**
     * @param generator      le générateur de terrain (bruit, biomes...)
     * @param renderDistance  rayon en chunks (8 = 8×16 = 128 blocs)
     */
    public ChunkManager(WorldGenerator generator, int renderDistance){
        this.generator = generator;
        this.renderDistance = renderDistance;
        this.loadedChunks = new ConcurrentHashMap<>();
        this.pendingGeneration = new ConcurrentHashMap<>();

        /*
         * Pool de 4 threads pour la génération.
         *
         * Pourquoi 4 ?
         *   - Correspond à un CPU quad-core typique
         *   - Plus de threads = plus de contention sur la map
         *   - Moins = génération trop lente quand le joueur bouge vite
         *
         * Les threads sont daemon → ils meurent quand le jeu se ferme,
         * pas besoin de les arrêter manuellement.
         */
        this.genPool = Executors.newFixedThreadPool(4, r -> {
            Thread t =new Thread(r, "ChunkGen");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Appelé à chaque tick du jeu avec la position du joueur en blocs.
     *
     * Étapes :
     *   1. Trouver le chunk du joueur
     *   2. Pour chaque chunk dans le cube de rendu :
     *      - S'il n'est pas chargé et pas en cours de génération → lancer la génération
     *   3. Décharger les chunks trop loin
     */
    public void update(int playerWorldX, int playerWorldY, int playerWorldZ){
        ChunkPos center = ChunkPos.fromWorld(playerWorldX, playerWorldY, playerWorldZ);

        // --- Étape 1 : Charger les chunks manquants ---
        requestChunksAround(center);

        // --- Étape 2 : Décharger les chunks hors rayon ---
        unloadDistantChunks(center);
    }

    /**
     * Parcourir le cube de rendu autour du joueur.
     *
     * Le cube va de (center - renderDistance) à (center + renderDistance)
     * sur les 3 axes. Pour renderDistance=8, ça fait 17³ = 4913 positions
     * à vérifier. Mais la plupart sont déjà chargées → le check est rapide
     * grâce à ConcurrentHashMap.containsKey() en O(1).
     */
    private void requestChunksAround(ChunkPos center){
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dy = -renderDistance; dy <= renderDistance; dy++) {
                for (int dz = -renderDistance; dz < renderDistance; dz++) {
                    ChunkPos pos = new ChunkPos(
                            center.x + dx,
                            center.y + dy,
                            center.z + dz
                    );

                    // Déjà chargé ? → rien à faire
                    if (loadedChunks.containsKey(pos)) continue;

                    // Déjà en cours de génération ? → pas de doublon
                    if (pendingGeneration.containsKey(pos)) continue;

                    // Lancer la génération asynchrone
                    pendingGeneration.put(pos, Boolean.TRUE);
                    genPool.submit(() -> generateAsync(pos));
                }
            }
        }
    }

    /**
     * Génération asynchrone d'un chunk.
     *
     * Exécuté dans un thread du pool, PAS sur le thread principal.
     * C'est pour ça qu'on utilise ConcurrentHashMap :
     * ce code tourne en parallèle du rendu.
     */
    private void generateAsync(ChunkPos pos){
        try{
            Chunk chunk = new Chunk(pos);
            generator.generateChunk(chunk);
            chunk.markDirty(); // Le mesh doit être construit

            loadedChunks.put(pos, chunk);
        } finally {
            // Toujours retirer de pending, même en cas d'erreur
            pendingGeneration.remove(pos);
        }
    }

    /**
     * Décharger les chunks trop loin du joueur.
     *
     * On parcourt tous les chunks chargés et on retire ceux
     * dont la distance Chebyshev au joueur dépasse renderDistance + 2.
     *
     * Pourquoi +2 ?
     *   Marge de sécurité pour éviter le "pop-in" :
     *   un chunk qui sort du rayon de rendu n'est pas immédiatement
     *   déchargé, il reste 2 chunks de plus. Ça évite de recharger
     *   un chunk si le joueur fait des allers-retours à la frontière.
     */
    private void unloadDistantChunks(ChunkPos center){
        int unloadDistance = renderDistance + 2;

        loadedChunks.entrySet().removeIf(chunkPosChunkEntry -> {
            return chunkPosChunkEntry.getKey().distanceTo(center) > unloadDistance;
        });
    }

    // === ACCÈS AUX CHUNKS ===

    /**
     * Récupérer un chunk chargé. Retourne null si pas encore chargé.
     */
    public Chunk getChunk(ChunkPos pos){
        return loadedChunks.get(pos);
    }

    /**
     * Récupérer un chunk depuis des coordonnées monde.
     */
    public Chunk getChunkAt(int wx, int wy, int wz){
        return loadedChunks.get(ChunkPos.fromWorld(wx, wy, wz));
    }

    /**
     * Lire un bloc dans le monde.
     * Trouve le bon chunk puis lit la position locale.
     * Retourne 0 (air) si le chunk n'est pas chargé.
     */
    public short getBlock(int wx, int wy, int wz){
        Chunk chunk = getChunkAt(wx, wy, wz);
        if (chunk == null) return 0;

        int lx = wx & 0xF;     // modulo 16 par masque binaire
        int ly = wy & 0xF;
        int lz = wz & 0xF;
        return chunk.getBlock(lx, ly, lz);
    }

    /**
     * Placer un bloc dans le monde.
     * Trouve le bon chunk et modifie la position locale.
     */
    public void setBlock(int wx, int wy, int wz, short blockId){
        Chunk chunk = getChunkAt(wx, wy, wz);
        if (chunk == null) return;

        int lx = wx & 0xF;
        int ly = wy & 0xF;
        int lz = wz & 0xF;
        chunk.setBlock(lx, ly, lz, blockId);
    }

    /**
     * Récupérer les chunks dirty (mesh à reconstruire).
     * Appelé par le renderer pour savoir quels meshes mettre à jour.
     */
    public Map<ChunkPos, Chunk> getLoadedChunks(){
        return loadedChunks;
    }

    /**
     * Nombre de chunks actuellement en mémoire.
     * Utile pour le debug / HUD.
     */
    public int getLoadedCount() {
        return loadedChunks.size();
    }

    /**
     * Nombre de chunks en cours de génération.
     */
    public int getPendingCount(){
        return  pendingGeneration.size();
    }

    /**
     * Récupère un chunk s'il est chargé, sinon le génère.
     */
    public Chunk getOrGenerate(ChunkPos pos) {
        return loadedChunks.computeIfAbsent(pos, p -> {
            Chunk chunk = new Chunk(p);
            generator.generateChunk(chunk);
            return chunk;
        });
    }

    public int getRenderDistance() {
        return renderDistance;
    }

    /**
     * Arrêter proprement le pool de génération.
     * Appelé quand le jeu se ferme.
     */
    public void shutdown(){
        genPool.shutdown();
        try{
            if (!genPool.awaitTermination(5, TimeUnit.SECONDS)){
                genPool.shutdownNow();
            }
        }catch (InterruptedException e){
            genPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
