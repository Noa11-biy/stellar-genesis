package com.stellargenesis.core.world.density;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DensityFieldTest {

    @Test
    void testConstructorStoresSize(){
        assertEquals(17, new DensityField(17).getSize());
        DensityField field2 = new DensityField(33);
        assertEquals(33, field2.getSize());
    }

    @Test
    void testGetSetRoundTrip(){
        DensityField field = new DensityField(17);

        field.set(3, 5, 2, 1.5f);

        assertEquals(1.5f, field.get(3, 5, 2), 1e-6f);
    }

    @Test
    void testInitialValuesZero(){
        DensityField field = new DensityField(17);
        int size = field.getSize();

        for (int z = 0; z < size ; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    assertEquals(0f, field.get(x, y, z), 1e-6f);
                }
            }
        }
    }

    @Test
    void testOutOfBoundsThrows() {
        DensityField field = new DensityField(17);

        // Coordonnées négatives
        assertThrows(IndexOutOfBoundsException.class, () -> field.get(-1, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> field.get(0, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> field.get(0, 0, -1));

        // Coordonnées >= size (size = 17, donc max valide = 16)
        assertThrows(IndexOutOfBoundsException.class, () -> field.get(17, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> field.get(0, 17, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> field.get(0, 0, 17));

        // Pareil pour set
        assertThrows(IndexOutOfBoundsException.class, () -> field.set(-1, 0, 0, 1f));
        assertThrows(IndexOutOfBoundsException.class, () -> field.set(17, 0, 0, 1f));
    }


    @Test
    void testFillAppliesFunctionEverywhere() {
        DensityField field = new DensityField(17);

        field.fill((x, y, z) -> (float)(x + y + z));

        int size = field.getSize();
        for (int z = 0; z < size; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    assertEquals((float)(x + y + z), field.get(x, y, z), 1e-6f);
                }
            }
        }
    }


    @Test
    void testIndexIsUnique() {
        DensityField field = new DensityField(17);
        int size = field.getSize();

        // ARRANGE + ACT : remplir avec une fonction injective
        field.fill((x, y, z) -> (float)(x + y * 1000 + z * 1000000));

        // ASSERT : relire et vérifier
        for (int z = 0; z < size; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float expected = (float)(x + y * 1000 + z * 1000000);
                    float actual = field.get(x, y, z);
                    assertEquals(expected, actual, 1e-6f,
                            "Collision détectée à (" + x + "," + y + "," + z + ")");
                }
            }
        }
    }


    @Test
    void testInvalidSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> new DensityField(0));
        assertThrows(IllegalArgumentException.class, () -> new DensityField(-5));
        assertThrows(IllegalArgumentException.class, () -> new DensityField(-1));
    }
}
