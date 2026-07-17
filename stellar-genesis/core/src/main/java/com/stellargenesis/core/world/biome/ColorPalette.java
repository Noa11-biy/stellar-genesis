package com.stellargenesis.core.world.biome;

import com.stellargenesis.core.math.Vec3;

public final class ColorPalette {
    public final Vec3 deepColor;
    public final Vec3 lowColor;
    public final Vec3 midColor;
    public final Vec3 highColor;
    public final Vec3 peakColor;
    public final Vec3 cliffColor;

    public ColorPalette(Vec3 deepColor, Vec3 lowColor, Vec3 midColor, Vec3 highColor, Vec3 peakColor, Vec3 cliffColor){
        this.deepColor = deepColor;
        this.lowColor = lowColor;
        this.midColor = midColor;
        this.highColor = highColor;
        this.peakColor = peakColor;
        this.cliffColor = cliffColor;
    }

    public static ColorPalette earthLike(){
        return new ColorPalette(
                new Vec3(0.76f, 0.70f, 0.50f), // deep  → sable clair
                new Vec3(0.55f, 0.55f, 0.50f), // low   → herbe sèche / roche claire
                new Vec3(0.30f, 0.50f, 0.20f), // mid   → herbe verte
                new Vec3(0.45f, 0.38f, 0.30f), // high  → roche brune
                new Vec3(0.95f, 0.95f, 0.98f), // peak  → neige
                new Vec3(0.38f, 0.34f, 0.30f)// cliff → roche grise foncée
        );
    }
}
