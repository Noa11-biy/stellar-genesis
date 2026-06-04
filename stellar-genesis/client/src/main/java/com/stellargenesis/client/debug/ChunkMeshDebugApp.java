package com.stellargenesis.client.debug;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.stellargenesis.client.render.MeshConverter;
import com.stellargenesis.core.world.density.DensityField;
import com.stellargenesis.core.world.density.DensityFieldGenerator;
import com.stellargenesis.core.world.meshing.ChunkMesh;
import com.stellargenesis.core.world.meshing.ChunkMesher;

/**
 * Application de debug : génère UN chunk Marching Cubes et l'affiche
 * dans une fenêtre 3D avec lumière directionnelle.
 *
 * Contrôles :
 *  - ZQSD / WASD : déplacement
 *  - Souris      : regarder autour
 *  - Espace / Shift : monter / descendre
 *  - Échap       : quitter
 */
public class ChunkMeshDebugApp extends SimpleApplication {

    private static final long SEED = 42L;
    private static final int CHUNK_X = 0;
    private static final int CHUNK_Y = 3;  // chunk Y = 3 → blocs y ∈ [48, 64], pile dans la heightmap (baseHeight=64)
    private static final int CHUNK_Z = 0;

    public static void main(String[] args) {
        ChunkMeshDebugApp app = new ChunkMeshDebugApp();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // 1. Génération du DensityField
        DensityFieldGenerator gen = new DensityFieldGenerator(SEED);
        DensityField field = gen.generate(CHUNK_X, CHUNK_Y, CHUNK_Z);

        // 2. Meshing Marching Cubes
        ChunkMesher mesher = new ChunkMesher();
        ChunkMesh chunkMesh = mesher.mesh(field);

        System.out.println("Mesh généré : "
                + chunkMesh.getVertexCount() + " sommets, "
                + chunkMesh.getTriangleCount() + " triangles");

        if (chunkMesh.isEmpty()) {
            System.out.println("⚠️ Mesh vide — le chunk est entièrement air ou entièrement sol.");
            return;
        }

        // 3. Conversion vers jME Mesh
        Mesh jmeMesh = MeshConverter.toJmeMesh(chunkMesh);

        // 4. Création de la Geometry
        Geometry geom = new Geometry("Chunk", jmeMesh);

        // 5. Matériau Lighting (réagit à la lumière → on voit le relief)
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient",  new ColorRGBA(0.3f, 0.3f, 0.3f, 1f));
        mat.setColor("Diffuse",  new ColorRGBA(0.6f, 0.5f, 0.4f, 1f)); // brun-beige
        mat.setColor("Specular", ColorRGBA.Black);                      // mat, pas brillant
        geom.setMaterial(mat);

        // 6. Position dans la scène (centre du chunk à l'origine pour confort visuel)
        geom.setLocalTranslation(
                -CHUNK_X * 16f,
                -CHUNK_Y * 16f,
                -CHUNK_Z * 16f
        );

        rootNode.attachChild(geom);

        // 7. Éclairage
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1f, -0.3f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.9f));
        rootNode.addLight(sun);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(ambient);

        // 8. Caméra : placée pour voir le chunk de 3/4
        cam.setLocation(new Vector3f(25, 25, 25));
        cam.lookAt(new Vector3f(8, 8, 8), Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(20f);

        // 9. Background ciel sombre pour bien voir le mesh
        viewPort.setBackgroundColor(new ColorRGBA(0.1f, 0.15f, 0.2f, 1f));
    }
}
