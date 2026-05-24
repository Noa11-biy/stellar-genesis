package com.stellargenesis.client.render;

import com.jme3.math.Matrix4f;
import com.jme3.renderer.Camera;
import com.stellargenesis.core.math.Mat4;
import com.stellargenesis.core.physics.math.Frustum;

/**
 * Pont entre la caméra jMonkeyEngine et notre Frustum maison.
 *
 * Cette classe est volontairement la SEULE qui connaît à la fois
 * jME (Camera, Matrix4f) et notre code maths (Mat4, Frustum).
 *
 * Si un jour on change de moteur 3D, seule cette classe sera à réécrire.
 *
 * Pipeline :
 *   jME Camera ─► Matrix4f view, Matrix4f proj
 *              ─► conversion en Mat4 maison
 *              ─► VP = proj × view
 *              ─► Frustum.fromViewProjection(VP)
 */
public final class FrustumExtractor {

    // Classe utilitaire : pas d'instanciation.
    private FrustumExtractor() {}

    /**
     * Extrait le frustum de vue de la caméra jME courante.
     *
     * À appeler chaque frame (ou chaque fois que la caméra change),
     * typiquement dans simpleUpdate() avant updateVisibleChunks().
     *
     * @param cam la caméra jME (cam dans SimpleApplication)
     * @return un Frustum maison utilisable avec AABB / Vec3
     */
    public static Frustum fromCamera(Camera cam) {
        Mat4 view = toMat4(cam.getViewMatrix());
        Mat4 proj = toMat4(cam.getProjectionMatrix());

        // VP = Projection × View (row-major : à droite = appliqué en premier)
        Mat4 vp = proj.multiply(view);

        return Frustum.fromViewProjection(vp);
    }

    /**
     * Convertit une Matrix4f jME (float, get(row,col)) vers notre Mat4 (double).
     *
     * jME et nous utilisons la même convention d'indexation get(row, col),
     * donc la copie est directe.
     */
    private static Mat4 toMat4(Matrix4f jme) {
        double[][] values = new double[4][4];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                values[row][col] = jme.get(row, col);
            }
        }
        return new Mat4(values);
    }
}
