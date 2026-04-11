package com.stellargenesis.client.player;

public class StaminaSystem {

    private double current;
    private double max;
    private double localGravity;

    private static final double EARTH_G = 9.81;
    private static final double DRAIN_WALK = 2.0;
    private static final double DRAIN_SPRINT = 8.0;
    private static final double COST_JUMP = 15.0;
    private static final double REGEN_RATE = 5.0;

    public enum Activity {
        IDLE, WALK, SPRINT
    }

    public StaminaSystem(double max, double localGravity) {
        this.max = max;
        this.current = max;
        this.localGravity = localGravity;
    }

    private double gravityRatio() {
        return localGravity / EARTH_G;
    }

    public void update(double dt, Activity activity) {
        switch (activity) {
            case IDLE:
                double regenMultiplier = Math.max(0.2, 1.0 - gravityRatio() * 0.3);
                current += REGEN_RATE * regenMultiplier * dt;
                break;
            case WALK:
                current -= DRAIN_WALK * gravityRatio() * dt;
                break;
            case SPRINT:
                current -= DRAIN_SPRINT * gravityRatio() * dt;
                break;
        }
        current = Math.max(0, Math.min(max, current));
    }

    public boolean tryJump() {
        double cost = COST_JUMP * gravityRatio();
        if (current >= cost) {
            current -= cost;
            return true;
        }
        return false;
    }

    public boolean canSprint() { return current > 1.0; }
    public boolean isExhausted() { return current <= 0; }
    public void setLocalGravity(double g) { this.localGravity = g; }
    public double getCurrent() { return current; }
    public double getMax() { return max; }
    public double getPercent() { return current / max; }
}
