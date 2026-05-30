package com.stellargenesis.core.world.density;

public class SphereDensity implements DensityFunction{

    private final float centerX, centerY, centerZ;
    private final float radius;

   public SphereDensity(float centerX, float centerY, float centerZ, float radius){
       if (radius <= 0){
           throw new IllegalArgumentException("Le rayon doit être supérieur à 0, reçu :" + radius);
       }
       this.centerX = centerX;
       this.centerY = centerY;
       this.centerZ = centerZ;
       this.radius = radius;
   }


    @Override
    public float sample(int x, int y, int z) {
       float dx = x - centerX;
       float dy = y - centerY;
       float dz = z - centerZ;
       return (float)Math.sqrt((dx*dx) + (dy*dy) + (dz*dz)) - radius;
    }
}
