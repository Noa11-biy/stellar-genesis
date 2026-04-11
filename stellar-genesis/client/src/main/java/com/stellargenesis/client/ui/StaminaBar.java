package com.stellargenesis.client.ui;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

public class StaminaBar {
    private Geometry barBg, barFill;

    public void init(Node guiNode, AssetManager am){
        //Fond gris
        barBg = makeQuad(am, 200, 12, new ColorRGBA(0.2f, 0.2f, 0.2f, 0.7f));
        barBg.setLocalTranslation(10, 60, 0);
        guiNode.attachChild(barBg);

        //Remplissage vert
        barFill = makeQuad(am, 200, 12, new ColorRGBA(0.2f, 0.8f, 0.2f, 0.9f));
        barFill.setLocalTranslation(10, 60, 1);
        guiNode.attachChild(barFill);
    }

    public void update(float staminaPercent){
        // entre 0 et 1
        barFill.setLocalScale(staminaPercent, 1, 1);

        // Couleur progressive : vert → jaune → rouge
        ColorRGBA color;
        if (staminaPercent > 0.5f) {
            color = new ColorRGBA(0.2f, 0.8f, 0.2f, 0.9f); // vert
        } else if (staminaPercent > 0.25f) {
            color = new ColorRGBA(0.9f, 0.8f, 0.1f, 0.9f); // jaune
        } else {
            color = new ColorRGBA(0.9f, 0.1f, 0.1f, 0.9f); // rouge
        }
        barFill.getMaterial().setColor("Color", color);
    }

    public Geometry makeQuad(AssetManager am, float w, float h, ColorRGBA color){
        Quad quad = new Quad(w, h);
        Geometry g = new Geometry("bar", quad);
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);

        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        g.setQueueBucket(RenderQueue.Bucket.Gui);
        g.setMaterial(mat);
        return g;
    }
}
