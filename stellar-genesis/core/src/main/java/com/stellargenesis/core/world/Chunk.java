package com.stellargenesis.core.world;

import com.stellargenesis.core.world.density.DensityField;

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
 *  DensityField (17×17×17 floats) → champ scalaire continu
 *  Convention : densité >= 0 → matière, densité < 0 → vide
 *  Mémoire par chunk : 17³ × 4 octets = 19 652 octets ≈ 19 Ko
 *  100 chunks chargés ≈ 2 Mo (toujours léger)
 */


public class Chunk {

    public static final int SIZE = 16;
    public static final int DENSITY_SIZE = 17;

    private final ChunkPos position;
    private final DensityField density;
    private boolean dirty;        // true = le mesh doit être recalculé
    private boolean generated;     // true = le terrain a été généré

    public Chunk(ChunkPos position){
        this.position = position;
        this.density = new DensityField(DENSITY_SIZE);
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

    /**
     * Vérifier qu'une coordonnée locale est dans [0..15].
     */
    public boolean inDensityBounds(int lx, int ly, int lz) {
        return lx >= 0 && lx < DENSITY_SIZE
                && ly >= 0 && ly < DENSITY_SIZE
                && lz >= 0 && lz < DENSITY_SIZE;
    }

    public boolean inCubeBounds(int lx, int ly, int lz){
        return lx >= 0 && lx < SIZE
                && ly >= 0 && ly < SIZE
                && lz >= 0 && lz < SIZE;
    }

    /**
     * Le chunk contient-il une iso-surface à mailler ?
     *
     * Si toutes les densités ont le même signe (toutes >= 0 ou toutes < 0),
     * aucun cube ne sera coupé par la surface → rien à mailler → skip.
     */
    public boolean hasSurface(){
        boolean foundPositive = false;
        boolean foundNegative = false;
        for (int z = 0; z < DENSITY_SIZE; z++) {
            for (int y = 0; y < DENSITY_SIZE; y++) {
                for (int x = 0; x < DENSITY_SIZE; x++) {
                    if (density.get(x, y, z) >= 0) foundPositive = true;
                    else foundNegative = true;
                    if (foundPositive && foundNegative) return true;
                }
            }
        }
        return false;
    }

    // === ÉTAT ===

    public boolean isDirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void markClean() { dirty = false; }

    public boolean isGenerated() { return generated; }
    public void markGenerated() { generated = true; }

    // === GETTERS ET SETTERS ===

    public ChunkPos getPosition() { return position; }

    /**
     * Lit la densité à une position locale [0..16].
     * Convention : densité >= 0 → matière, densité < 0 → vide.
     */
    public float getDensity(int lx, int ly, int lz) {
        return density.get(lx, ly, lz);
    }

    /**
     * Modifie la densité à une position locale [0..16].
     * Marque le chunk dirty (le mesh doit être recalculé).
     */
    public void setDensity(int lx, int ly, int lz, float value) {
        density.set(lx, ly, lz, value);
        dirty = true;
    }

    /**
     * Un point est dans la matière si sa densité est >= 0.
     * (équivalent sémantique de l'ancien !isAir)
     */
    public boolean isInside(int lx, int ly, int lz) {
        return getDensity(lx, ly, lz) >= 0;
    }

}
