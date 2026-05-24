package com.stellargenesis.client.render;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

/**
 * Outil de debug : capture le frustum d'une caméra à un instant T
 * et le dessine en wireframe dans la scène.
 *
 * Usage :
 *   FrustumDebugRenderer debug = new FrustumDebugRenderer(assetManager, rootNode);
 *   debug.capture(cam);   // fige et dessine
 *   debug.clear();        // efface
 *   debug.toggle(cam);    // alterne entre les deux
 */
public class FrustumDebugRenderer {

    private final AssetManager assetManager;
    private final Node parentNode;
    private Geometry frustumGeom;  // null = pas de frustum dessiné

    public FrustumDebugRenderer(AssetManager assetManager, Node parentNode) {
        this.assetManager = assetManager;
        this.parentNode = parentNode;
    }

    /** Alterne entre capturer et effacer. */
    public void toggle(Camera cam) {
        if (frustumGeom == null) {
            capture(cam);
        } else {
            clear();
        }
    }

    /** Capture le frustum actuel de la caméra et le dessine. */
    public void capture(Camera cam) {
        clear();  // au cas où

        Vector3f[] corners = computeCorners(cam);
        Mesh mesh = buildWireframeMesh(corners);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Yellow);
        mat.getAdditionalRenderState().setWireframe(true);

        frustumGeom = new Geometry("FrustumDebug", mesh);
        frustumGeom.setMaterial(mat);
        parentNode.attachChild(frustumGeom);
    }

    /** Efface le frustum dessiné. */
    public void clear() {
        if (frustumGeom != null) {
            parentNode.detachChild(frustumGeom);
            frustumGeom = null;
        }
    }

    // ─────────────────────────────────────────────
    //  CALCUL DES 8 SOMMETS
    // ─────────────────────────────────────────────

    private Vector3f[] computeCorners(Camera cam) {
        Vector3f pos     = cam.getLocation().clone();
        Vector3f forward = cam.getDirection().clone().normalizeLocal();
        Vector3f up      = cam.getUp().clone().normalizeLocal();
        Vector3f right   = forward.cross(up).normalizeLocal();

        float near = cam.getFrustumNear();
        float far  = cam.getFrustumFar();
        float fov  = (float) Math.toRadians(45);  // ⚠ à ajuster si besoin
        float aspect = (float) cam.getWidth() / cam.getHeight();

        float hhNear = (float) (near * Math.tan(fov / 2));
        float hwNear = hhNear * aspect;
        float hhFar  = (float) (far * Math.tan(fov / 2));
        float hwFar  = hhFar * aspect;

        Vector3f centerNear = pos.add(forward.mult(near));
        Vector3f centerFar  = pos.add(forward.mult(far));

        Vector3f[] c = new Vector3f[8];
        // Near plane : 0=NTL, 1=NTR, 2=NBL, 3=NBR
        c[0] = centerNear.add(up.mult(hhNear)).subtract(right.mult(hwNear));
        c[1] = centerNear.add(up.mult(hhNear)).add(right.mult(hwNear));
        c[2] = centerNear.subtract(up.mult(hhNear)).subtract(right.mult(hwNear));
        c[3] = centerNear.subtract(up.mult(hhNear)).add(right.mult(hwNear));
        // Far plane : 4=FTL, 5=FTR, 6=FBL, 7=FBR
        c[4] = centerFar.add(up.mult(hhFar)).subtract(right.mult(hwFar));
        c[5] = centerFar.add(up.mult(hhFar)).add(right.mult(hwFar));
        c[6] = centerFar.subtract(up.mult(hhFar)).subtract(right.mult(hwFar));
        c[7] = centerFar.subtract(up.mult(hhFar)).add(right.mult(hwFar));

        return c;
    }

    // ─────────────────────────────────────────────
    //  CONSTRUCTION DU MESH FILAIRE
    // ─────────────────────────────────────────────

    private Mesh buildWireframeMesh(Vector3f[] corners) {
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Lines);

        // Positions : 8 sommets × 3 floats
        float[] positions = new float[24];
        for (int i = 0; i < 8; i++) {
            positions[i*3]   = corners[i].x;
            positions[i*3+1] = corners[i].y;
            positions[i*3+2] = corners[i].z;
        }

        // Indices des 12 arêtes (chaque arête = 2 sommets)
        short[] indices = {
                // Near plane (rectangle)
                0,1,  1,3,  3,2,  2,0,
                // Far plane (rectangle)
                4,5,  5,7,  7,6,  6,4,
                // Rails near ↔ far
                0,4,  1,5,  2,6,  3,7
        };

        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
        mesh.setBuffer(VertexBuffer.Type.Index, 2, BufferUtils.createShortBuffer(indices));
        mesh.updateBound();

        return mesh;
    }
}
