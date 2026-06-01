package com.stellargenesis.core.world.mesh;

import com.stellargenesis.core.world.density.DensityField;
import com.stellargenesis.core.world.meshing.ChunkMesh;
import com.stellargenesis.core.world.meshing.ChunkMesher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MarchingCubesMesherTest {

    @Test
    void testSphereProducesNonEmptyMesh() {
        // ARRANGE : créer un champ 17³ contenant une sphère de rayon 5 au centre
        DensityField field = new DensityField(17);
        float cx = 8, cy = 8, cz = 8;
        float radius = 5;

        field.fill((x, y, z) -> {
            float dx = x - cx;
            float dy = y - cy;
            float dz = z - cz;
            return (float) Math.sqrt(dx*dx + dy*dy + dz*dz) - radius;
        });

        // ACT : générer le mesh
        ChunkMesher mesher = new ChunkMesher();
        ChunkMesh mesh = mesher.mesh(field);

        // ASSERT : le mesh ne doit pas être vide
        assertFalse(mesh.isEmpty(), "Le mesh d'une sphère ne doit pas être vide");
        assertTrue(mesh.getTriangleCount() > 100,
                "Une sphère de rayon 5 devrait avoir au moins 100 triangles, eu: "
                        + mesh.getTriangleCount());

        System.out.println("Sphère générée : "
                + mesh.getVertexCount() + " sommets, "
                + mesh.getTriangleCount() + " triangles");
    }

    @Test
    void testEmptyFieldProducesEmptyMesh() {
        // ARRANGE : champ entièrement positif (tout dehors)
        DensityField field = new DensityField(17);
        field.fill((x, y, z) -> 1.0f);

        // ACT
        ChunkMesh mesh = new ChunkMesher().mesh(field);

        // ASSERT
        assertTrue(mesh.isEmpty(), "Un champ entièrement dehors doit produire un mesh vide");
    }

    @Test
    void testFullFieldProducesEmptyMesh() {
        // ARRANGE : champ entièrement négatif (tout dedans)
        DensityField field = new DensityField(17);
        field.fill((x, y, z) -> -1.0f);

        // ACT
        ChunkMesh mesh = new ChunkMesher().mesh(field);

        // ASSERT
        assertTrue(mesh.isEmpty(), "Un champ entièrement dedans doit produire un mesh vide");
    }
}
