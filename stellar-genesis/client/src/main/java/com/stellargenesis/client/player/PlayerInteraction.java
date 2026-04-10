package com.stellargenesis.client.player;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.stellargenesis.core.inventory.Inventory;
import com.stellargenesis.core.player.MiningSystem;
import com.stellargenesis.core.world.BlockType;
import com.stellargenesis.core.world.Chunk;
import com.stellargenesis.core.world.ChunkManager;
import com.stellargenesis.core.world.ChunkPos;

/**
 * Gère l'interaction joueur ↔ monde :
 * - Raycasting pour détecter le bloc visé
 * - Minage (clic gauche maintenu)
 * - Placement de bloc (clic droit) — plus tard
 *
 * Raycasting :
 *   On tire un rayon depuis la position caméra dans la direction
 *   où le joueur regarde. jME teste les collisions avec les meshes
 *   de chunks. Le point de contact nous donne les coordonnées monde,
 *   qu'on convertit en coordonnées voxel.
 *
 * Astuce pour trouver le bon voxel :
 *   Le point de collision est sur la FACE du bloc.
 *   On recule légèrement dans la direction du rayon pour être
 *   à l'intérieur du bloc touché (pas le bloc voisin).
 */

public class PlayerInteraction {

    private static final float REACH_DISTANCE = 5.0f; // Portée de minage en mètre

    private Camera cam;
    private Node worldNode;                 // noeud parent des chunks rendus
    private ChunkManager chunkManager;
    private MiningSystem miningSystem;
    private Inventory inventory;

    // État
    private boolean mouseHeld = false;      // clic gauche maintenu
    private Vector3f targetBlockPos = null; // position monde du bloc visé
    private BlockType targetBlockType = null;

    public PlayerInteraction(Camera cam, Node worldNode, InputManager inputManager, ChunkManager chunkManager,
                             MiningSystem miningSystem, Inventory inventory){
        this.cam = cam;
        this.worldNode = worldNode;
        this.chunkManager = chunkManager;
        this.miningSystem = miningSystem;
        this.inventory = inventory;

        setupInput(inputManager);
    }

    private void setupInput(InputManager inputManager){
        inputManager.addMapping("Mine", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Place", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            if (name.equals("Mine")){
                mouseHeld = isPressed;
                if (!isPressed){
                    miningSystem.stopMining();
                    targetBlockPos = null;
                    targetBlockType = null;
                }
            }
        }, "Mine", "Place");
    }

    /**
     * Appelé chaque frame depuis simpleUpdate().
     *
     * Logique :
     * 1. Si clic maintenu → raycast pour trouver le bloc
     * 2. Si on vise un nouveau bloc → recommencer le minage
     * 3. Sinon → continuer le minage en cours
     * 4. Si le bloc casse → le supprimer du chunk + drop
     * 5. Si pas de clic → régénérer l'endurance
     */
    public void update(float tpf){
        if (mouseHeld) {
            // --- RAYCAST ---
            RaycastResult hit = raycast();

            if (hit != null) {
                // Vérifier si on vise toujours le même bloc
                if (targetBlockPos == null || !targetBlockPos.equals(hit.blockWorldPos)) {
                    // Nouveau bloc -> recommencer
                    targetBlockPos = hit.blockWorldPos;
                    targetBlockType = hit.blockType;
                    miningSystem.startMining(hit.blockType);
                }

                // --- TICK MINAGE ---
                boolean broken = miningSystem.tick(tpf);

                if (broken) {
                    onBlockBroken(hit);
                }
            } else {
                // On ne vise rien arrêter
                miningSystem.stopMining();
                targetBlockPos = null;
                targetBlockType = null;
            }
        }else {
            // Pas de minage regen endurance
            miningSystem.regenStamina(tpf);
        }
    }

    /**
     * Raycast depuis la caméra.
     *
     * Retourne le bloc visé ou null si rien à portée.
     *
     * Pour trouver le voxel exact :
     * - Le point de collision est sur la surface du mesh
     * - On décale de 0.1 dans la direction du rayon pour entrer dans le bloc
     * - On arrondit vers le bas (floor) pour avoir les coords entières du voxel
     */
    private RaycastResult raycast() {
        // Créer le rayon depuis la caméra
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());

        // Tester les collisions avec les meshes de chunks
        CollisionResults results = new CollisionResults();
        worldNode.collideWith(ray, results);

        if (results.size() == 0) return null;

        // Prendre le hit le plus proche
        CollisionResult closest = results.getClosestCollision();

        if (closest.getDistance() > REACH_DISTANCE) return null;

        // Point de contact sur la face du bloc
        Vector3f contactPoint = closest.getContactPoint();

        // Décaler légèrement DANS le bloc (direction du rayon)
        // pour être sûr d'identifier le bon voxel
        Vector3f inside = contactPoint.add(cam.getDirection().mult(0.1f));

        // Coordonnées voxel (arrondi vers le bas)
        int bx = (int) Math.floor(inside.x);
        int by = (int) Math.floor(inside.y);
        int bz = (int) Math.floor(inside.z);

        // Récupérer le bloc dans le chunk correspondant
        ChunkPos chunkPos = ChunkPos.fromWorld(bx, by, bz);
        Chunk chunk = chunkManager.getChunk(chunkPos);

        if (chunk == null) return null;

        int localX = bx & 15;  // équivalent à bx % 16 pour positifs
        int localY = by & 15;
        int localZ = bz & 15;

        short voxelId = chunk.getBlock(localX, localY, localZ);

        if (voxelId == 0) return null; // air

        BlockType blockType = BlockType.fromId(voxelId);

        // Calculer aussi la face touchée (pour placement futur)
        Vector3f normal = closest.getContactNormal();
        Vector3f adjacentPos = contactPoint.add(normal.mult(0.5f));

        RaycastResult result = new RaycastResult();
        result.blockWorldPos = new Vector3f(bx, by, bz);
        result.blockType = blockType;
        result.chunk = chunk;
        result.localX = localX;
        result.localY = localY;
        result.localZ = localZ;
        result.hitNormal = normal;
        result.adjacentBlockPos = new Vector3f(
                (int) Math.floor(adjacentPos.x),
                (int) Math.floor(adjacentPos.y),
                (int) Math.floor(adjacentPos.z)
        );

        return result;
    }

    /**
     * Appelé quand un bloc est cassé.
     * 1. Supprimer le voxel du chunk (mettre à 0 = air)
     * 2. Marquer le chunk comme dirty (re-mailler)
     * 3. Ajouter l'item à l'inventaire
     */
    private void onBlockBroken(RaycastResult hit){
        // 1. Supprimer le bloc
        hit.chunk.setBlock(hit.localX, hit.localY, hit.localZ, (short) 0);

        // 2. Drop → inventaire

        BlockType dropType = BlockType.fromId(hit.blockType.getDropId());
        if (dropType != BlockType.AIR) {
           int remaining = inventory.addItem(dropType, 1);
            if (remaining > 0) {
                System.out.println("Inventaire plein !");
                // TODO: drop l'item au sol
            }
        }

        // 3. Reset
        targetBlockPos = null;
        targetBlockType = null;
    }


    // === Getters pour le HUD ===
    public double getMiningProgress() { return miningSystem.getProgress(); }
    public boolean isMining() { return miningSystem.isMining(); }
    public BlockType getTargetBlock() { return targetBlockType; }
    public double getStamina() { return miningSystem.getStamina(); }
    public double getMaxStamina() { return miningSystem.getMaxStamina(); }

    /**
     * Données du raycast.
     */
    public static class RaycastResult {
        public Vector3f blockWorldPos;
        public BlockType blockType;
        public Chunk chunk;
        public int localX, localY, localZ;
        public Vector3f hitNormal;
        public Vector3f adjacentBlockPos;  // pour le placement
    }

}
