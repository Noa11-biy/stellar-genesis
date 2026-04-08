package com.stellargenesis.core.world;

/**
 * Un chunk = 16×16×16 voxels.
 *
 * Coordonnées :
 *   - Monde    : (wx, wy, wz) position absolue d'un bloc
 *   - Chunk    : (cx, cy, cz) identifiant du chunk
 *   - Locale   : (lx, ly, lz) position dans le chunk [0..15]
 *
 * Conversions :
 *   cx = wx >> 4          (division entière par 16)
 *   lx = wx & 0xF         (modulo 16 via masque binaire)
 *   wx = cx × 16 + lx     (reconstruction)
 *
 * Stockage :
 *   short[][][] voxels → ID du BlockType (0 = air)
 *   Mémoire par chunk : 16×16×16 × 2 octets = 8 192 octets = 8 Ko
 *   100 chunks chargés = 800 Ko (très léger)
 */


public class Chunk {

    public static final int SIZE = 16;

    private final ChunkPos position;
    private final short[][][] voxels;
    private boolean dirty;        // true = le mesh doit être recalculé
    private boolean generated;     // true = le terrain a été généré

    public Chunk(ChunkPos position){
        this.position = position;
        this.voxels = new short[SIZE][SIZE][SIZE];
        this.dirty = false;
        this.generated = false;
    }

    // === CONVERSION DE COORDONNÉES ===

    /**
     * Monde → Chunk.
     * Division entière par 16 avec >> 4.
     *
     * Pourquoi >> 4 et pas / 16 ?
     *   >> 4 fonctionne correctement pour les négatifs.
     *   -1 >> 4 = -1  (chunk -1)
     *   -1 / 16 = 0   (FAUX, on veut chunk -1)
     *
     * Attention : en Java >> est arithmétique (conserve le signe).
     */
    public static int worldToChunk(int worldCoord){
        return worldCoord >> 4;
    }

    /**
     * Monde → Local dans le chunk [0..15].
     * Masque binaire & 0xF = garder les 4 bits de poids faible.
     *
     * Exemples :
     *   worldToLocal(0)  = 0
     *   worldToLocal(15) = 15
     *   worldToLocal(16) = 0   (nouveau chunk)
     *   worldToLocal(17) = 1
     *   worldToLocal(-1) = 15  (dernier bloc du chunk -1)
     */
    public static int toWorldCoord(int chunkCoord, int localCoord){
        return chunkCoord * SIZE + localCoord;
    }

    // === ACCÈS AUX VOXELS ===

    /**
     * Lire le type de bloc à une position locale.
     * Retourne 0 (AIR) si hors limites.
     */
    public short getBlock(int lx, int ly, int lz){
        if(inBounds(lx, ly, lz)) return 0;
        return voxels[lx][ly][lz];
    }

    /**
     * Placer un bloc à une position locale.
     * Marque le chunk comme dirty pour remaillage.
     */
    public void setBlock(int lx, int ly, int lz, short blockId){
        if (!inBounds(lx, ly, lz)) return;
        if (voxels[lx][ly][lz] == blockId) return;

        voxels[lx][ly][lz] = blockId;
        dirty = true;
    }

    /**
     * Vérifier si un bloc est de l'air.
     */
    public boolean isAir(int lx, int ly, int lz){
        return  getBlock(lx, ly, lz) == 0;
    }

    /**
     * Vérifier qu'une coordonnée locale est dans [0..15].
     */
    public boolean inBounds(int lx, int ly, int lz){
        return lx >= 0 && lx < SIZE
                && ly >= 0 && ly < SIZE
                && lz >= 0 && lz < SIZE;
    }

    // === COMPTAGE ===

    /**
     * Compter les blocs non-air dans le chunk.
     * Utile pour savoir si le chunk est vide (skip rendu).
     */
    

}
