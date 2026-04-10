package com.stellargenesis.client.ui;

import com.jme3.app.Application;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.stellargenesis.core.physics.PlanetData;

public class GameHUD {

    private BitmapText hudText;

    /**
     * Initialise le HUD.
     * @param app       l'application jME (pour récupérer la font par défaut)
     * @param guiNode   le noeud GUI de jME (superposé à la scène 3D)
     */
    public void init(Application app, Node guiNode) {
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        hudText = new BitmapText(font, false);
        hudText.setSize(font.getCharSet().getRenderedSize());
        hudText.setColor(ColorRGBA.White);
        hudText.setLocalTranslation(10, app.getCamera().getHeight() - 10, 0);
        guiNode.attachChild(hudText);
    }
    

    /**
     * Met à jour le texte affiché chaque frame.
     */
    public void update(Vector3f playerPos, PlanetData planet) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Position: %.1f, %.1f, %.1f\n",
                playerPos.x, playerPos.y, playerPos.z));
        sb.append(String.format("Gravité: %.2f m/s² (%.2f g)\n",
                planet.getSurfaceGravity(),
                planet.getSurfaceGravity() / 9.81));
        sb.append(String.format("Température: %.0f K (%.0f °C)\n",
                planet.getSurfaceTemperature(),
                planet.getSurfaceTemperature() - 273.15));
        sb.append(String.format("Pression: %.3f bar\n",
                planet.getSurfacePressure()));
        sb.append(String.format("V_escape: %.0f m/s\n",
                planet.getEscapeVelocity()));
        hudText.setText(sb.toString());
    }
}
