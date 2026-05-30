package com.stellargenesis.core.world.density;

public class DensityField {

    private int size = 17;
    private final float[] data;

    public DensityField(int size){
        if(size <= 0){
            throw new IllegalArgumentException("la densité ne doit pas être inférieur ou égale à 0, reçus :" + size);
        }
        this.size = size;
        data = new float[size * size * size];
    }

    public int getSize() {
        return size;
    }

    public float get(int x, int y, int z){
        checkBounds(x, y, z);
        return data[index(x, y, z)];
    }

    public void set(int x, int y, int z, float value){
        checkBounds(x, y, z);
        data[index(x, y, z)] = value;
    }

    public void fill(DensityFunction fn){
        for (int z = 0; z < size; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    data[index(x, y, z)] = fn.sample(x, y, z);
                }
            }
        }
    }

    private int index(int x, int y, int z){
        return x + y*size + z*size*size;
    }

    private void checkBounds(int x, int y, int z) {
        if (x < 0 || x >= size || y < 0 || y >= size || z < 0 || z >= size) {
            throw new IndexOutOfBoundsException(
                    "Coordonnées hors bornes : (" + x + "," + y + "," + z +
                            ") pour size=" + size
            );
        }
    }
}
