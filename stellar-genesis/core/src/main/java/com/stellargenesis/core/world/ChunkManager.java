package com.stellargenesis.core.world;

import com.stellargenesis.core.math.Vec3;
import com.stellargenesis.core.physics.math.AABB;
import com.stellargenesis.core.physics.math.Frustum;
import com.stellargenesis.core.world.density.DensityField;
import com.stellargenesis.core.world.density.DensityFieldGenerator;
import com.stellargenesis.core.world.meshing.ChunkMesh;
import com.stellargenesis.core.world.meshing.ChunkMesher;


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
    private final ConcurrentLinkedQueue<ChunkMeshPair> readyToAttach = new ConcurrentLinkedQueue<>();
    private final DensityFieldGenerator generator;
    private final int renderDistance;

    /**
     * @param generator      le générateur de terrain (bruit, biomes...)
     * @param renderDistance  rayon en chunks (8 = 8×16 = 128 blocs)
     */
    public ChunkManager(DensityFieldGenerator generator, int renderDistance){
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
        java.util.concurrent.ThreadFactory factory = r -> {
            Thread t = new Thread(r, "ChunkGen");
            t.setDaemon(true);
            return t;
        };

        this.genPool = new java.util.concurrent.ThreadPoolExecutor(
                4, 4,                                                   // corePoolSize = maxPoolSize = 4
                0L, java.util.concurrent.TimeUnit.MILLISECONDS,         // keepAliveTime (inutile ici)
                new java.util.concurrent.PriorityBlockingQueue<>(),     // ← LA queue prioritaire
                factory
        );
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
    public void update(int playerWorldX, int playerWorldY, int playerWorldZ, Frustum frustum){
        ChunkPos center = ChunkPos.fromWorld(playerWorldX, playerWorldY, playerWorldZ);

        // --- Étape 1 : Charger les chunks manquants ---
        requestChunksAround(center, frustum);

        // --- Étape 2 : Décharger les chunks hors rayon ---
        unloadDistantChunks(center);
    }

    /**
     * Demande la génération des chunks autour d'un point central.
     * Chaque chunk reçoit une priorité selon :
     *   - sa visibilité dans le frustum
     *   - sa distance au joueur
     */
    private void requestChunksAround(ChunkPos center, Frustum frustum) {
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                for (int dy = -2; dy <= 4; dy++) {
                    ChunkPos pos = new ChunkPos(center.x + dx, center.y + dy, center.z + dz);

                    if (loadedChunks.containsKey(pos)) continue;
                    if (pendingGeneration.containsKey(pos)) continue;

                    // --- Calcul de la priorité ---
                    int priority = computePriority(pos, center, frustum);

                    pendingGeneration.put(pos, Boolean.TRUE);
                    genPool.execute(new PrioritizedChunkTask(priority, () -> generateAsync(pos)));
                }
            }
        }
    }

    /**
     * Calcule la priorité d'un chunk.
     *   plus petit = plus urgent
     *   visibles (0-999) passent avant invisibles (1000+)
     */
    private int computePriority(ChunkPos pos, ChunkPos center, Frustum frustum){
        // 1. Distance au joueur en chunks (Manhattan = rapide, pas besoin de sqrt)
        int distance = Math.abs(pos.x - center.x)
                + Math.abs(pos.y - center.y)
                + Math.abs(pos.z - center.z);

        // 2. Visibilité dans le frustum
        boolean visible;
        if (frustum == null) {
            // Pas de frustum (démarrage) → tout est traité comme visible
            visible = true;
        } else {
            Vec3 min = new Vec3(
                    pos.x * Chunk.SIZE,
                    pos.y * Chunk.SIZE,
                    pos.z * Chunk.SIZE
            );
            Vec3 max = new Vec3(
                    min.x + Chunk.SIZE,
                    min.y + Chunk.SIZE,
                    min.z + Chunk.SIZE
            );
            visible = frustum.intersects(new AABB(min, max));
        }

        // 3. Combinaison : visibles (0-999) avant invisibles (1000+)
        return (visible ? 0 : 1000) + distance;
    }

    /**
     * Génération asynchrone d'un chunk.
     *
     * Exécuté dans un thread du pool, PAS sur le thread principal.
     * C'est pour ça qu'on utilise ConcurrentHashMap :
     * ce code tourne en parallèle du rendu.
     */
    private void generateAsync(ChunkPos pos) {
        try {
            // 1. Créer le chunk
            Chunk chunk = new Chunk(pos);

            // 2. Générer le champ de densité directement DANS le chunk
            //    subtilité : DensityFieldGenerator.generate() retourne un NOUVEAU
            //    DensityField, alors que Chunk en a déjà un en interne.
            //    → voir note ci-dessous
            DensityField generated = generator.generate(pos.x, pos.y, pos.z);

            // → on copie le contenu dans le DensityField du chunk
            //    (ou alternative : on adapte Chunk pour accepter un DensityField externe)
            copyDensity(generated, chunk.getDensityField());

            chunk.markGenerated();
            loadedChunks.put(pos, chunk);

            // 3. Mesher
            ChunkMesh chunkMesh = ChunkMesher.mesh(chunk.getDensityField());

            // 4. Si le mesh n'est pas vide, le pousser TEL QUEL (pas de conversion)
            if (chunkMesh != null && chunkMesh.getVertices().length > 0) {
                readyToAttach.add(new ChunkMeshPair(pos, chunk, chunkMesh));
            }

            // 5. Re-mesher les voisins
            remeshNeighbors(pos);

        } finally {
            pendingGeneration.remove(pos);
        }
    }

    private static void copyDensity(DensityField src, DensityField dst) {
        int size = src.getSize();
        for (int z = 0; z < size; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    dst.set(x, y, z, src.get(x, y, z));
                }
            }
        }
    }

    private void remeshNeighbors(ChunkPos pos) {
        int[][] neighborOffsets = {
                {1,0,0}, {-1,0,0},
                {0,1,0}, {0,-1,0},
                {0,0,1}, {0,0,-1}
        };

        for (int[] offset : neighborOffsets) {
            ChunkPos neighborPos = new ChunkPos(
                    pos.x + offset[0],
                    pos.y + offset[1],
                    pos.z + offset[2]
            );

            Chunk neighbor = loadedChunks.get(neighborPos);
            if (neighbor == null) continue; // pas encore chargé, pas grave

            ChunkMesh chunkMesh = ChunkMesher.mesh(neighbor.getDensityField());
            if (chunkMesh != null && chunkMesh.getVertices().length > 0) {
                readyToAttach.add(new ChunkMeshPair(neighborPos, neighbor, chunkMesh));
            }
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
//    public Chunk getOrGenerate(ChunkPos pos) {
//        return loadedChunks.computeIfAbsent(pos, p -> {
//            Chunk chunk = new Chunk(p);
//            generator.generateChunk(chunk);
//            return chunk;
//        });
//    }

    /**
     * Insérer un chunk directement dans la map (spawn synchrone).
     * Utilisé UNIQUEMENT pour findTerrainHeight au démarrage.
     */
    public void forceInsert(ChunkPos pos, Chunk chunk) {
        loadedChunks.put(pos, chunk);
    }


    public int getRenderDistance() {
        return renderDistance;
    }

    public ConcurrentLinkedQueue<ChunkMeshPair> getReadyQueue() {
        return readyToAttach;
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

    public record ChunkMeshPair(ChunkPos pos, Chunk chunk, ChunkMesh mesh) {}
}
