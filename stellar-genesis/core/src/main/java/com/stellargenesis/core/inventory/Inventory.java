package com.stellargenesis.core.inventory;

import com.stellargenesis.core.world.BlockType;

/**
 * Inventaire du joueur limité par le poids.
 *
 * Capacité max en Newtons :
 *   W_max = m_joueur × g_locale × facteur_portage
 *
 * facteur_portage = 0.5 (un humain porte ~50% de son poids)
 * Joueur de 80kg sur Terre : W_max = 80 × 9.81 × 0.5 = 392 N
 * Même joueur sur la Lune  : W_max = 80 × 1.62 × 0.5 = 65 N
 *   → mais les items pèsent aussi moins lourd !
 *
 * @author Noa Moal
 */

public class Inventory {

    private final ItemStack[] slots;
    private final int size;
    private final double playerMass;
    private final double carryFactor;
    private double localGravity;


    public Inventory(int size, double playerMass, double localGravity){
        this.size = size;
        this.slots = new ItemStack[size];
        this.playerMass = playerMass;
        this.carryFactor = 0.5;
        this.localGravity = localGravity;
    }

    // === POIDS ===

    /**
     * Poids max portable en Newtons.
     * W_max = m_joueur × g × facteur
     */
    public double getMaxWeight(){
        return playerMass * localGravity * carryFactor;
    }

    /**
     * Poids actuel porté en Newtons.
     * Somme de chaque stack : m_stack × g
     */
    public double getCurrentWeight(){
        double total = 0;
        for (ItemStack stack : slots){
            if (stack != null && !stack.isEmpty()){
                total += stack.getWeight(localGravity);
            }
        }
        return total;
    }

    /**
     * Poids restant disponible en Newtons.
     */
    public double getRemainingWeight(){
        return getMaxWeight() - getCurrentWeight();
    }

    // === AJOUT ===

    /**
     * Ajouter des items. Vérifie le poids avant.
     * Retourne le nombre d'items qui n'ont pas pu rentrer.
     */
    public int addItem(BlockType type, int quantity) {
        for (int i = 0; i < quantity; i++) {
            double oneItemWeight = type.getBlockMass() * ItemStack.ITEM_VOLUME * localGravity;

            // Utiliser les méthodes existantes au lieu de champs inexistants
            if (getCurrentWeight() + oneItemWeight > getMaxWeight()) {
                return quantity - i;
            }

            // Chercher un stack existant via la méthode interne
            int stackIndex = findStackOf(type);
            if (stackIndex != -1) {
                slots[stackIndex].add(1);
            } else {
                // Chercher un slot vide
                int emptyIndex = findEmptySlot();
                if (emptyIndex == -1) {
                    return quantity - i; // plus de slots
                }
                slots[emptyIndex] = new ItemStack(type, 1);
            }
        }
        return 0;
    }

    // === RETRAIT ===
    /**
     * Retirer des items d'un type donné.
     * Retourne le nombre réellement retiré.
     */
    public int removeItem(BlockType type, int amount){
        int toRemove = amount;

        for (int i = 0; i < size && toRemove >0; i++) {
            if (slots[i] != null && slots[i].getType() == type){
                int removed = slots[i].remove(toRemove);
                toRemove -= removed;

                // Nettoyer le slot vide
                if(slots[i].isEmpty()){
                    slots[i] = null;
                }
            }
        }
        return amount - toRemove;
    }


    /**
     * Compter le total d'un type dans l'inventaire.
     */
    public int countItem(BlockType type){
        int count = 0;
        for (ItemStack stack : slots){
            if (stack != null && stack.getType() == type){
                count += stack.getQuantity();
            }
        }
        return count;
    }

    /**
     * Changer la gravité (changement de planète).
     * Le poids max ET le poids des items changent.
     */
    public void setLocalGravity(double newGravity){
        this.localGravity = newGravity;
    }

    // === RECHERCHE INTERNE ===

    private int findStackOf(BlockType type){
        for (int i = 0; i < size; i++) {
            if (slots[i] != null
            && slots[i].getType() == type
            && !slots[i].isFull()) {
                return i;
            }
        }
        return -1;
    }

    private int findEmptySlot(){
        for (int i = 0; i < size; i++) {
            if (slots[i] == null) return i;
        }
        return -1;
    }

    // === GETTERS ===
    public ItemStack getSlot(int index) { return slots[index]; }
    public int getSize() { return size; }
    public double getLocalGravity() { return localGravity; }
    public double getPlayerMass() { return playerMass; }
}
