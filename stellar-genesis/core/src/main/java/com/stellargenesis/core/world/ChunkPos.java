package com.stellargenesis.core.world;

import java.util.Objects;

/**
 * Position d'un chunk dans la grille monde.
 * Immutable — utilisé comme clé de HashMap.
 */
public class ChunkPos {

    public final int x;
    public final int y;
    public final int z;

    public ChunkPos(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Créer un ChunkPos depuis des coordonnées monde.
     */
    public static ChunkPos fromWorld(int wx, int wy, int wz){
        return new ChunkPos(
                Chunk.worldToChunk(wx),
                Chunk.worldToChunk(wy),
                Chunk.worldToChunk(wz)
        );
    }

    /**
     * Distance en chunks (Chebyshev = distance "carrée").
     * Utilisé pour le rayon de rendu.
     */
    public int distanceTo(ChunkPos other){
        return Math.max (
                Math.max(Math.abs(x - other.x), Math.abs(y - other.y)),
                Math.abs(z - other.z)
        );
    }

    @Override
    public String toString() {
        return "Chunk(" + x + ", " + y + ", " + z + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof ChunkPos )) return false;
        ChunkPos other = (ChunkPos) o;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
