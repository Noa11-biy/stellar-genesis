package com.stellargenesis.client.render;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import com.stellargenesis.core.physics.PlanetData;
import com.stellargenesis.core.physics.RayleighScattering;

/**
 * Skybox procédurale.
 *
 * La couleur du ciel dépend de :
 *   - La diffusion de Rayleigh : I(λ) ∝ 1/λ⁴
 *     → Les courtes longueurs d'onde (bleu) diffusent plus
 *   - La température de l'étoile (couleur de la lumière incidente)
 *   - La densité atmosphérique (pression de surface)
 *     → Atmosphère dense = plus de diffusion = ciel plus saturé
 *     → Pas d'atmosphère = ciel noir (comme la Lune)
 */

public class SkyManager {
    private Geometry skyGeom;
    private Material skyMat;
    private ColorRGBA zenithColor;
    private ColorRGBA horizonColor;

    /**
     * Initialise la skybox.
     *
     * On utilise une grande sphère inversée (faces vers l'intérieur)
     * centrée sur la caméra. Le joueur est toujours "à l'intérieur".
     */

    public void init(AssetManager assetManager, Node rootNode, PlanetData planet){
        // Sphère de rayon 500, 32 segments, faces inversées
        Sphere skySphere = new Sphere(32, 32, 500f, false, true);

        skyGeom = new Geometry("Sky", skySphere);

        // Matériau Unshaded = pas affecté par la lumière (LE CIEL)
        skyMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        // Calculer la couleur du ciel
        zenithColor = calculateSkyColor(planet);
        skyMat.setColor("Color", zenithColor);

        // Pas de backface culling (on voit l'intérieur de la sphère)
        skyMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

        skyGeom.setMaterial(skyMat);

        // Rendu en premier (derrière tout le reste)
        skyGeom.setQueueBucket(RenderQueue.Bucket.Sky);

        rootNode.attachChild(skyGeom);
    }

    /**
     * Calcule la couleur du ciel selon la physique atmosphérique.
     *
     * Diffusion de Rayleigh : I_scatter(λ) ∝ (1/λ⁴) × densité_atm
     *
     * λ_bleu  ≈ 450 nm → diffuse beaucoup   → ciel bleu
     * λ_rouge ≈ 650 nm → diffuse peu         → soleil jaune/rouge
     *
     * Si P_surface ≈ 0 (pas d'atmosphère) → ciel noir
     * Si P_surface > 1 bar → ciel très saturé
     * Si étoile rouge (M) → ciel tire vers le rouge/orange
     */
    private ColorRGBA calculateSkyColor(PlanetData planet) {
        // Couleur du ciel à midi = Rayleigh avec soleil au zénith
        float[] rgb = RayleighScattering.computeSkyColor(
                1.0f,  // soleil au zénith
                planet.getSurfacePressure(),
                planet.getStarTemperature()
        );
        return new ColorRGBA(rgb[0], rgb[1], rgb[2], 1f);
    }

    /**
     * Appelé chaque frame : la skybox suit la caméra.
     * Sans ça, le joueur pourrait "sortir" du ciel en se déplaçant.
     */
    public void update(com.jme3.math.Vector3f cameraPos){
        skyGeom.setLocalTranslation(cameraPos);
    }

    /**
     * Change la couleur du ciel (utilisé plus tard pour le cycle jour/nuit).
     */
    public void setSkyColor(ColorRGBA color){
        skyMat.setColor("Color", color);
    }

    public ColorRGBA getZenithColor(){
        return zenithColor;
    }

}
