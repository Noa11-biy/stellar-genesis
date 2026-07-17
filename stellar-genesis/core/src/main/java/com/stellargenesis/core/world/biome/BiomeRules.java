package com.stellargenesis.core.world.biome;

import com.stellargenesis.core.math.MathUtils;
import com.stellargenesis.core.math.Vec3;


public final class BiomeRules {

    private final float cliffThreshold;  // dot en dessous duquel = falaise
    private final float lowEnd;          // t en dessous duquel = deep
    private final float midEnd;          // ... = low
    private final float highEnd;         // ... = mid
    private final float peakEnd;         // ... = high

    public BiomeRules(float cliffThreshold, float lowEnd,
                      float midEnd, float highEnd, float peakEnd) {
        this.cliffThreshold = cliffThreshold;
        this.lowEnd = lowEnd;
        this.midEnd = midEnd;
        this.highEnd = highEnd;
        this.peakEnd = peakEnd;
    }

    // Un preset par défaut
    public static BiomeRules earthLike() {
        return new BiomeRules(0.5f, 0.10f, 0.30f, 0.80f, 0.92f);
    }

    public Vec3 colorFor(Vec3 position, Vec3 planetCenter, Vec3 normal,
                         float radius, float amplitude, ColorPalette palette) {
        Vec3 toPoint = position.sub(planetCenter);
        float altitude = (float) toPoint.length();

        float t = (altitude - (radius - amplitude)) / (2 * amplitude);
        t = (float) MathUtils.clamp(t, 0, 1);

        Vec3 verticaleLocale = toPoint.normalize();
        float dot = (float) normal.dot(verticaleLocale);

        if (dot < cliffThreshold) return palette.cliffColor;

        if (t < lowEnd) return palette.deepColor;
        if (t < midEnd) return palette.lowColor;
        if (t < highEnd) return palette.midColor;
        if (t < peakEnd) return palette.highColor;
        return palette.peakColor;
    }

    public Vec3 colorForFlat(float worldY, Vec3 normal,
                             float baseHeight, float amplitude, ColorPalette palette) {
        // t ∈ [0,1] : position verticale dans la bande [base-amp, base+amp]
        float t = (worldY - (baseHeight - amplitude)) / (2 * amplitude);
        t = (float) MathUtils.clamp(t, 0, 1);

        // verticale locale = toujours vers le haut sur terrain plat
        Vec3 verticale = new Vec3(0, 1, 0);
        float dot = (float) normal.dot(verticale);

        if (dot < cliffThreshold) return palette.cliffColor;
        if (t < lowEnd)  return palette.deepColor;
        if (t < midEnd)  return palette.lowColor;
        if (t < highEnd) return palette.midColor;
        if (t < peakEnd) return palette.highColor;
        return palette.peakColor;
    }

}
