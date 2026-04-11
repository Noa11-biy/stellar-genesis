package com.stellargenesis.client.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

public class Crosshair {

    private Camera cam;
    private Node guiNode;
    private AssetManager assetManager;

    public Crosshair(){

    }

    public void initCrosshair(AssetManager assetManager, Node guiNode, Camera cam) {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText cross = new BitmapText(font);
        cross.setText("+");
        cross.setSize(font.getCharSet().getRenderedSize() * 2);
        cross.setColor(ColorRGBA.White);
        cross.setLocalTranslation(
                (cam.getWidth() - cross.getLineWidth()) / 2f,
                (cam.getHeight() + cross.getLineHeight()) / 2f, 0);
        guiNode.attachChild(cross);
    }

}
