package com.stellargenesis.core.inventory;

import com.stellargenesis.core.world.BlockType;

/**
 * Un stack d'items dans l'inventaire.
 *
 * Masse totale = masse_unitaire × quantité
 * Poids ressenti = masse_totale × gravité_locale
 *
 * @author Noa Moal
 */

public class ItemStack {

    private BlockType type;
    private int quantity;
    private final int maxStack;

    public ItemStack(BlockType type, int quantity){
        this.type = type;
        this.quantity  = quantity;
        this.maxStack = 64;
    }


    // === GETTERS ===
    public BlockType getType() { return type; }
    public int getQuantity() { return quantity; }
    public int getMaxStack() { return maxStack; }

    /**
     * Masse totale du stack en kg.
     * m_total = densité × 1m³ × quantité
     *
     * En réalité un "item" miné n'est pas 1m³ entier.
     * On applique un facteur de compaction : 1 item = 0.01 m³
     * Donc : m_item = densité × 0.01
     */
    public double getTotalMass(){
        return type.getBlockMass() * 0.01 * quantity;
    }

    /**
     * Masse d'un seul item en kg.
     */
    public double getUnitMass(){
        return type.getBlockMass() * 0.01;
    }

    /**
     * Poids ressenti en Newtons sous une gravité donnée.
     * W = m × g
     */
    public double getWeight(double gravity) {
        return getTotalMass() * gravity;
    }

    /**
     * Ajouter des items. Retourne le surplus qui ne rentre pas.
     */
    public int add(int amount) {
        int space = maxStack - quantity;
        int toAdd = Math.min(amount, space);
        quantity += toAdd;
        return amount - toAdd;
    }

    /**
     * Retirer des items. Retourne le nombre réellement retiré.
     */
    public int remove(int amount){
        int toRemove = Math.min(amount, quantity);
        quantity -= toRemove;
        return toRemove;
    }

    public boolean isEmpty(){
        return quantity <= 0;
    }

    public boolean isFull(){
        return quantity >= maxStack;
    }
}
