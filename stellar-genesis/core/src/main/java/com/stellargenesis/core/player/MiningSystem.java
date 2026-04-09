package com.stellargenesis.core.player;


import com.stellargenesis.core.world.BlockType;

/**
 * Système de minage basé sur la physique.
 *
 * Temps de minage :
 *   t_mine = (dureté / efficacité_outil)² × facteur_gravité
 *
 * facteur_gravité :
 *   En gravité forte → plus dur de lever l'outil → plus lent
 *   fg = sqrt(g_locale / g_terre)
 *   Sur Terre  : fg = 1.0
 *   Sur la Lune : fg = 0.41 (minage plus facile)
 *   Sur Jupiter : fg = 1.59 (minage pénible)
 *
 * Endurance consommée par coup :
 *   E_coup = dureté × fg × 2.0
 *   Quand endurance = 0 → efficacité divisée par 3
 *
 * @author Noa Moal
 */

public class MiningSystem {

    private static final double EARTH_GRAVITY = 9.81;
    private static final double STAMINA_COST_FACTOR = 2.0;
    private static final double EXHAUSTION_PENALTY = 3.0;

    private double toolEfficiency;
    private double localGravity;
    private double stamina;
    private double maxStamina;

    private BlockType currentTarget;
    private double miningProgress;
    private double miningTimeRequired;

    public MiningSystem(double toolEfficiency, double localGravity, double maxStamina){
        this.toolEfficiency = toolEfficiency;
        this.localGravity = localGravity;
        this.maxStamina = maxStamina;
        this.stamina = maxStamina;
        this.currentTarget = null;
        this.miningProgress = 0;
        this.miningTimeRequired = 0;
    }

    // === CALCULES PHYSIQUES ===
    /**
     * Facteur de gravité sur le minage.
     * fg = sqrt(g_locale / g_terre)
     *
     * Justification : lever un outil demande un travail W = m × g × h
     * Le temps d'un swing est proportionnel à sqrt(g) (pendule)
     */
    public double getGravityFactor(){
        return Math.sqrt(localGravity / EARTH_GRAVITY);
    }

    /**
     * Temps total pour miner un bloc en secondes.
     * t = (dureté / efficacité)² × fg
     */
    public double calculateMiningTime(BlockType block){
        if (!block.isMinable()) return Double.MAX_VALUE;

        double eff = getEffectiveEfficiency();
        double ratio = block.getHardness() / eff;
        double baseTime = ratio * ratio;

        return baseTime * getGravityFactor();
    }

    /**
     * Efficacité réelle de l'outil.
     * Si épuisé (stamina = 0), divisée par 3.
     */
    public double getEffectiveEfficiency(){
        if (stamina <= 0){
            return toolEfficiency / EXHAUSTION_PENALTY;
        }
        return toolEfficiency;
    }

    /**
     * Endurance consommée par tick de minage.
     * E = dureté × fg × facteur
     */
    public double getStaminaCostPerSecond(BlockType block){
        return block.getHardness() * getGravityFactor() * STAMINA_COST_FACTOR;
    }

    // === BOUCLE DE MINAGE ===

    /**
     * Commencer à miner un nouveau bloc.
     */
    public void startMining(BlockType block){
        if (!block.isMinable()) return;

        currentTarget = block;
        miningProgress = 0;
        miningTimeRequired = calculateMiningTime(block);
    }

    /**
     * Appelé chaque frame pendant que le joueur mine.
     * dt = temps écoulé depuis la dernière frame (secondes).
     *
     * Retourne true si le bloc est cassé.
     */
    public boolean tick(double dt){
        if (currentTarget == null) return false;

        //Consommer l'endurance
        double staminaCost = getStaminaCostPerSecond(currentTarget) * dt;
        stamina = Math.max(0, stamina - staminaCost);

        //Recalculer le temps si épuisé (outil moins efficace)
        miningTimeRequired = calculateMiningTime(currentTarget);

        //Avancer la progression
        miningProgress += dt;

        //Cassé ?
        if (miningProgress >= miningTimeRequired){
            BlockType mined = currentTarget;
            stopMining();
            return true;
        }
        return false;
    }

    /**
     * Arrêter de miner (joueur lâche le clic).
     * La progression est perdue.
     */
    public void stopMining(){
        currentTarget = null;
        miningProgress = 0;
        miningTimeRequired = 0;
    }

    /**
     * Régénération de l'endurance quand on ne mine pas.
     * Récupération : 10 points/seconde (repos)
     * Affecté par la gravité : plus dur de récupérer en g fort
     *   regen = base_regen / fg
     */
    public void regenStamina(double dt){
        double baseRegen = 10.0;
        double regen = baseRegen / getGravityFactor();
        stamina = Math.min(maxStamina, stamina + regen * dt);
    }

    /**
     * Progression actuelle en pourcentage [0, 1].
     */
    public double getProgress(){
        if (miningTimeRequired <= 0) return 0;
        return Math.min(1.0, miningProgress / miningTimeRequired);
    }

    // === SETTERS ===

    public void setToolEfficiency(double eff) { this.toolEfficiency = eff; }
    public void setLocalGravity(double g) { this.localGravity = g; }

    // === GETTERS ===

    public double getStamina() { return stamina; }
    public double getMaxStamina() { return maxStamina; }
    public double getToolEfficiency() { return toolEfficiency; }
    public double getLocalGravity() { return localGravity; }
    public BlockType getCurrentTarget() { return currentTarget; }
    public boolean isMining() { return currentTarget != null; }

}
