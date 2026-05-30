package com.stellargenesis.core.world.density;

@FunctionalInterface
public interface DensityFunction {
    float sample(int x, int y, int z);
}
