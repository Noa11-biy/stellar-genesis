package com.stellargenesis.client.screens;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.stellargenesis.client.StellarGenesisApp;

/**
 * Écran de pause affiché par-dessus le jeu.
 *
 * === PRINCIPE ===
 * Contrairement à l'écran titre qui REMPLACE le jeu, la pause se SUPERPOSE.
 * Le monde 3D reste visible en arrière-plan, mais figé.
 *
 * === GESTION DE LA PAUSE ===
 * On désactive (setEnabled(false)) les AppStates du gameplay — pas détachés !
 * Comme ça on peut les réactiver en reprenant, sans tout recréer.
 */
public class PauseScreenState extends AbstractAppState {

    private static final String CLICK_MAPPING = "pause_click";

    private static final ColorRGBA OVERLAY_BG  = new ColorRGBA(0f, 0f, 0f, 0.6f);
    private static final ColorRGBA BTN_BG      = new ColorRGBA(0.18f, 0.20f, 0.28f, 0.95f);
    private static final ColorRGBA BTN_BORDER  = new ColorRGBA(0.4f, 0.65f, 0.85f, 0.8f);
    private static final ColorRGBA ACCENT      = new ColorRGBA(0.55f, 0.85f, 0.95f, 1f);

    private SimpleApplication app;
    private Node pauseRoot;  // parent de tout le menu pause

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;

        pauseRoot = new Node("PauseRoot");

        buildUI();

        // Input clic
        app.getInputManager().addMapping(CLICK_MAPPING,
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(clickListener, CLICK_MAPPING);

        // Afficher le curseur pendant la pause
        app.getInputManager().setCursorVisible(true);

        this.app.getGuiNode().attachChild(pauseRoot);

        System.out.println("[Pause] Affiché.");

        StellarGenesisApp sga = (StellarGenesisApp) app;
        if (sga.getPlayerControl() != null) {
            sga.getPlayerControl().setEnabled(false);
        }
        if (sga.getPlayerInteraction() != null) {
            sga.getPlayerInteraction().setEnabled(false);
        }
        if (sga.getBulletAppState() != null) {
            sga.getBulletAppState().setEnabled(false);
        }

    }

    private void buildUI() {
        int w = app.getCamera().getWidth();
        int h = app.getCamera().getHeight();

        // --- Overlay sombre plein écran ---
        Geometry overlay = makeQuad("overlay", w, h, OVERLAY_BG);
        overlay.setLocalTranslation(0, 0, 0);
        pauseRoot.attachChild(overlay);

        // --- Titre "PAUSE" ---
        BitmapFont font = app.getAssetManager()
                .loadFont("Interface/Fonts/Default.fnt");
        BitmapText title = new BitmapText(font);
        title.setText("PAUSE");
        title.setSize(font.getCharSet().getRenderedSize() * 3f);
        title.setColor(ACCENT);
        title.setLocalTranslation(
                (w - title.getLineWidth()) / 2f,
                h * 0.7f,
                1f
        );
        pauseRoot.attachChild(title);

        // --- Boutons ---
        float btnW = 300f;
        float btnH = 60f;
        float cx = (w - btnW) / 2f;

        makeButton("resume",   "Reprendre",     cx, h * 0.5f,  btnW, btnH, font);
        makeButton("mainmenu", "Menu Principal", cx, h * 0.38f, btnW, btnH, font);
    }

    private void makeButton(String id, String label, float x, float y,
                            float w, float h, BitmapFont font) {
        // Fond
        Geometry bg = makeQuad("btn_" + id, w, h, BTN_BG);
        bg.setLocalTranslation(x, y, 1f);
        pauseRoot.attachChild(bg);

        // Bordure (4 fines quads)
        float b = 2f;
        attachBorder(x, y, w, h, b);

        // Texte centré
        BitmapText txt = new BitmapText(font);
        txt.setText(label);
        txt.setSize(font.getCharSet().getRenderedSize() * 1.3f);
        txt.setColor(ColorRGBA.White);
        txt.setLocalTranslation(
                x + (w - txt.getLineWidth()) / 2f,
                y + (h + txt.getLineHeight()) / 2f,
                2f
        );
        pauseRoot.attachChild(txt);
    }

    private void attachBorder(float x, float y, float w, float h, float b) {
        pauseRoot.attachChild(posQuad(makeQuad("border", w, b, BTN_BORDER), x, y));
        pauseRoot.attachChild(posQuad(makeQuad("border", w, b, BTN_BORDER), x, y + h - b));
        pauseRoot.attachChild(posQuad(makeQuad("border", b, h, BTN_BORDER), x, y));
        pauseRoot.attachChild(posQuad(makeQuad("border", b, h, BTN_BORDER), x + w - b, y));
    }

    private Geometry posQuad(Geometry g, float x, float y) {
        g.setLocalTranslation(x, y, 1.5f);
        return g;
    }

    private Geometry makeQuad(String name, float w, float h, ColorRGBA color) {
        Geometry g = new Geometry(name, new Quad(w, h));
        Material m = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", color);
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        g.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Gui);
        g.setMaterial(m);
        return g;
    }

    private String pickButton() {
        Vector2f mouse = app.getInputManager().getCursorPosition();
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(new Vector3f(mouse.x, mouse.y, 10f),
                new Vector3f(0, 0, -1));
        pauseRoot.collideWith(ray, results);

        if (results.size() > 0) {
            Spatial hit = results.getClosestCollision().getGeometry();
            while (hit != null) {
                String n = hit.getName();
                if (n != null && n.startsWith("btn_")) {
                    return n.substring(4);
                }
                hit = hit.getParent();
            }
        }
        return null;
    }

    private final ActionListener clickListener = (name, isPressed, tpf) -> {
        if (!isPressed || !isEnabled()) return;
        String btn = pickButton();
        if (btn == null) return;

        System.out.println("[Pause] Clic: " + btn);

        switch (btn) {
            case "resume":
                ((StellarGenesisApp) app).resumeGame();
                break;
            case "mainmenu":
                ((StellarGenesisApp) app).backToTitle();
                break;
        }
    };

    @Override
    public void cleanup() {
        super.cleanup();
        if (pauseRoot != null) {
            pauseRoot.removeFromParent();
            pauseRoot = null;
        }
        app.getInputManager().removeListener(clickListener);
        if (app.getInputManager().hasMapping(CLICK_MAPPING)) {
            app.getInputManager().deleteMapping(CLICK_MAPPING);
        }
        System.out.println("[Pause] Nettoyé.");
    }
}
