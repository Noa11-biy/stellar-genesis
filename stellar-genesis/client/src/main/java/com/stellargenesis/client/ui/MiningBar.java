package com.stellargenesis.client.ui;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

public class MiningBar {
    private Geometry barBg, barFill;
    private boolean visible = false;

    public void init(Node guiNode, AssetManager am, Camera cam) {
        float cx = cam.getWidth() / 2f - 75;
        float cy = cam.getHeight() / 2f - 30;

        barBg = makeQuad(am, 150, 8, new ColorRGBA(0.1f, 0.1f, 0.1f, 0.8f));
        barBg.setLocalTranslation(cx, cy, 0);

        barFill = makeQuad(am, 150, 8, new ColorRGBA(0.9f, 0.7f, 0.1f, 0.9f));
        barFill.setLocalTranslation(cx, cy, 1);

        hide();
        guiNode.attachChild(barBg);
        guiNode.attachChild(barFill);
    }

    public void show() {
        barBg.setCullHint(Spatial.CullHint.Never);
        barFill.setCullHint(Spatial.CullHint.Never);
        visible = true;
    }

    public void hide() {
        barBg.setCullHint(Spatial.CullHint.Always);
        barFill.setCullHint(Spatial.CullHint.Always);
        visible = false;
    }

    public void update(float progress) {
        // progress entre 0.0 et 1.0
        barFill.setLocalScale(progress, 1, 1);
    }

    private Geometry makeQuad(AssetManager am, float w, float h, ColorRGBA color) {
        Quad quad = new Quad(w, h);
        Geometry geo = new Geometry("bar", quad);
        Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geo.setMaterial(mat);
        geo.setQueueBucket(RenderQueue.Bucket.Gui);
        return geo;
    }
}
