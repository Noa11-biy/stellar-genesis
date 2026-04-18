package com.stellargenesis.client.screens;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.stellargenesis.client.StellarGenesisApp;

import java.util.Random;

/**
 * Écran titre style Astroneer :
 * - Menu calé à GAUCHE (marge ~5% depuis le bord)
 * - Côté droit libre pour un asset 3D / image d'ambiance
 * - Splash text jaune incliné qui pulse
 * - 4 boutons seulement
 */
public class TitleScreenState extends AbstractAppState {

    // ═══ COULEURS ═══
    private static final ColorRGBA BG_COLOR     = new ColorRGBA(0.08f, 0.10f, 0.16f, 1f);
    private static final ColorRGBA ACCENT       = new ColorRGBA(0.55f, 0.85f, 0.95f, 1f);
    private static final ColorRGBA TEXT_DIM     = new ColorRGBA(0.6f, 0.65f, 0.7f, 1f);
    private static final ColorRGBA BTN_BG       = new ColorRGBA(0.18f, 0.20f, 0.28f, 0.9f);
    private static final ColorRGBA BTN_HOVER    = new ColorRGBA(0.25f, 0.28f, 0.38f, 0.95f);
    private static final ColorRGBA BTN_BORDER   = new ColorRGBA(0.4f, 0.65f, 0.85f, 0.8f);
    private static final ColorRGBA SPLASH_COLOR = new ColorRGBA(1f, 0.9f, 0.2f, 1f);
    private static final ColorRGBA WHITE        = ColorRGBA.White.clone();
    private static final ColorRGBA TITLE_COLOR     = ACCENT;
    private static final ColorRGBA SUBTITLE_COLOR  = TEXT_DIM;
    private static final ColorRGBA SEPARATOR_COLOR = ACCENT.clone().mult(0.5f);

    // ═══ SPLASHS ═══
    private static final String[] SPLASHES = {
            "F = G·M·m/r² !",
            "Also try Factorio!",
            "Also try Minecraft!",
            "Masse volumique > 9000 !",
            "Delta-v is all you need!",
            "Thermodynamiquement viable !",
            "Entropie croissante depuis 2025",
            "Pas de friction dans le vide !",
            "J'ai une chiasse terrible",
            "η = 1 - Tc/Th",
            "Procéduralement généré !",
            "E = mc² (environ)",
            "100% physiquement cohérent*",
            "* conditions générales applicables",
            "Rendement de Carnot maximal !",
            "Attention : trou noir en approche",
            "La gravité, c'est relatif",
            "Constante de Boltzmann approuvée",
            "Open Simplex > Perlin",
            "Made in BUT Informatique",
            "Iron smelting intensifies...",
            "One more conveyor belt...",
            "Projet de fin d'année btw",
            "Surviving on alien dirt since 2025",
            "Now with 100% more physics!",
            "σT⁴ goes brrrr",
            "May the Δv be with you",
    };

    private SimpleApplication app;
    private Node menuNode;
    private BitmapFont font;
    private float screenW, screenH;

    // Splash animation
    private BitmapText splashText;
    private float splashTime = 0;

    // Hover
    private String hoveredButton = null;
    private String lastHovered = null;

    // ═══ MARGE GAUCHE ═══
    // Tout le menu est dans la zone [marginLeft ... marginLeft + menuWidth]
    private float marginLeft;
    private float menuWidth;

    private Node playMenuNode;  // sous-menu "Jouer"
    private boolean inPlayMenu = false;
    private float timeSinceInit = 0f;
    boolean showingPlayMenu = false;

    @Override
    public void initialize(AppStateManager stateManager, Application application) {
        super.initialize(stateManager, application);
        this.app = (SimpleApplication) application;
        this.font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        this.screenW = app.getCamera().getWidth();
        this.screenH = app.getCamera().getHeight();

        System.out.println("=== TitleScreen INIT ===");
        System.out.println("Screen: " + screenW + "x" + screenH);

        this.marginLeft = screenW * 0.05f;
        this.menuWidth = screenW * 0.35f;

        menuNode = new Node("TitleMenu");

        buildBackground();
        buildTitle();
        buildSplash();
        buildButtons();
        buildFooter();
        buildPlayMenu();
        playMenuNode.setCullHint(Spatial.CullHint.Always);

        app.getGuiNode().attachChild(menuNode);

        System.out.println("=== menuNode children: " + menuNode.getChildren().size());
        System.out.println("=== guiNode children: " + app.getGuiNode().getChildren().size());

        // Input souris
        InputManager im = app.getInputManager();
        im.setCursorVisible(true);
        im.addMapping("click", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        im.addListener(clickListener, "click");
    }

    // ═══ FOND ═══
    private void buildBackground() {
        Geometry bg = new Geometry("bg", new Quad(screenW, screenH));
        Material mat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", BG_COLOR);
        bg.setMaterial(mat);
        bg.setLocalTranslation(0, 0, -10);
        menuNode.attachChild(bg);

        // ═══ Panneau semi-transparent derrière le menu (style Astroneer) ═══
        // Légèrement plus clair que le fond pour délimiter la zone menu
        float panelW = menuWidth + marginLeft + screenW * 0.03f;
        Geometry panel = new Geometry("menuPanel", new Quad(panelW, screenH));
        Material panelMat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        panelMat.setColor("Color", new ColorRGBA(0.06f, 0.08f, 0.14f, 0.5f));
        panelMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        panel.setMaterial(panelMat);
        panel.setLocalTranslation(0, 0, -9);
        menuNode.attachChild(panel);
    }

    // ═══ TITRE — calé à gauche ═══
    private void buildTitle() {
        // "STELLAR" en gros
        float titleSize = screenH * 0.07f;
        BitmapText line1 = makeText("S T E L L A R", titleSize, ACCENT);
        line1.setLocalTranslation(marginLeft, screenH * 0.88f, 1);
        menuNode.attachChild(line1);

        // "GENESIS" en-dessous
        BitmapText line2 = makeText("G E N E S I S", titleSize, ACCENT);
        line2.setLocalTranslation(marginLeft, screenH * 0.88f - titleSize * 1.1f, 1);
        menuNode.attachChild(line2);

        // Séparateur
        float sepY = screenH * 0.88f - titleSize * 2.4f;
        float sepW = menuWidth * 0.85f;
        Geometry sep = new Geometry("sep", new Quad(sepW, 2));
        Material sepMat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        sepMat.setColor("Color", ACCENT.mult(0.5f));
        sep.setMaterial(sepMat);
        sep.setLocalTranslation(marginLeft, sepY, 1);
        menuNode.attachChild(sep);

        // Sous-titre
        float subSize = screenH * 0.022f;
        BitmapText sub = makeText("Survive  ·  Automate  ·  Conquer the Stars", subSize, TEXT_DIM);
        sub.setLocalTranslation(marginLeft, sepY - subSize * 1.5f, 1);
        menuNode.attachChild(sub);
    }

    // ═══ SPLASH TEXT ═══
    private void buildSplash() {
        String splash = SPLASHES[new Random().nextInt(SPLASHES.length)];
        float splashSize = screenH * 0.018f;
        splashText = makeText(splash, splashSize, SPLASH_COLOR);

        // Positionné à droite du titre, un peu plus haut
        float splashX = marginLeft + menuWidth * 0.65f;
        float splashY = screenH * 0.72f;
        splashText.setLocalTranslation(splashX, splashY, 2);
        // Rotation inclinée ~15°
        splashText.rotate(0, 0, 10 * FastMath.DEG_TO_RAD);
        menuNode.attachChild(splashText);
    }

    // ═══ BOUTONS — calés à gauche ═══
    private void buildButtons() {
        float btnW = menuWidth * 0.9f;
        float btnH = screenH * 0.06f;
        float spacing = btnH * 1.4f;
        float btnX = marginLeft;

        // Zone de boutons commence sous le sous-titre
        float topY = screenH * 0.58f;

        // --- 2 grands boutons ---
        String[][] mainBtns = {
                {"solo", "Nouvelle Partie"},
                {"multiplayer", "Multijoueur"},
        };
        for (int i = 0; i < mainBtns.length; i++) {
            float y = topY - i * spacing;
            createButton(mainBtns[i][0], mainBtns[i][1], btnX, y, btnW, btnH);
        }

        // --- 2 petits boutons côte à côte ---
        float smallY = topY - mainBtns.length * spacing - spacing * 0.3f;
        float smallW = btnW * 0.47f;
        float gap = btnW * 0.06f;

        createButton("settings", "Paramètres", btnX, smallY, smallW, btnH);
        createButton("quit", "Quitter", btnX + smallW + gap, smallY, smallW, btnH);
    }

    // ═══ FOOTER ═══
    private void buildFooter() {
        float footerSize = screenH * 0.018f;
        BitmapText version = makeText("v0.1-alpha", footerSize, TEXT_DIM.mult(0.6f));
        version.setLocalTranslation(marginLeft, screenH * 0.04f, 1);
        menuNode.attachChild(version);

        BitmapText credit = makeText("Projet Solo", footerSize, TEXT_DIM.mult(0.6f));
        credit.setLocalTranslation(
                screenW - credit.getLineWidth() - screenW * 0.03f,
                screenH * 0.04f, 1);
        menuNode.attachChild(credit);
    }

    // ═══ UTILITAIRES ═══

    private BitmapText makeText(String text, float size, ColorRGBA color) {
        BitmapText t = new BitmapText(font);
        t.setText(text);
        t.setSize(size);
        t.setColor(color);
        return t;
    }

    private void createButton(String id, String label, float x, float y,
                              float w, float h) {
        Node btn = new Node("btn_" + id);

        // Fond
        Geometry bg = new Geometry("bg", new Quad(w, h));
        Material mat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", BTN_BG);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        bg.setMaterial(mat);
        btn.attachChild(bg);

        // Bordure gauche (accent)
        float borderW = w * 0.012f;
        Geometry border = new Geometry("border", new Quad(borderW, h));
        Material bMat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        bMat.setColor("Color", BTN_BORDER);
        border.setMaterial(bMat);
        btn.attachChild(border);

        // Texte centré
        float textSize = h * 0.45f;
        BitmapText text = makeText(label, textSize, WHITE);
        float textX = (w - text.getLineWidth()) / 2f;
        float textY = (h + textSize) / 2f;
        text.setLocalTranslation(textX, textY, 1);
        btn.attachChild(text);

        btn.setLocalTranslation(x, y, 0);
        menuNode.attachChild(btn);
    }

    private void buildPlayMenu() {
        playMenuNode = new Node("playMenu");

        float centerX = screenW * 0.5f;
        float topY = screenH * 0.7f;

        // Titre du sous-menu
        float titleSize = screenH * 0.035f;
        BitmapText title = makeText("JOUER", titleSize, TITLE_COLOR);
        title.setLocalTranslation(centerX - title.getLineWidth() / 2f, topY, 1);
        playMenuNode.attachChild(title);

        // Ligne décorative sous le titre
        float lineY = topY - titleSize * 0.8f;
        Geometry line = makeLineH(centerX - screenW * 0.2f, lineY, screenW * 0.4f, SEPARATOR_COLOR);
        playMenuNode.attachChild(line);

        // ── Les deux cartes côte à côte (style NMS) ──
        float cardW = screenW * 0.22f;
        float cardH = screenH * 0.3f;
        float gap = screenW * 0.03f;
        float cardsStartX = centerX - cardW - gap / 2f;
        float cardsY = lineY - screenH * 0.05f;

        // Carte "Nouveau Monde"
        Node newWorldCard = makeCard(
                "btn_newworld",
                cardsStartX, cardsY,
                cardW, cardH,
                "NOUVEAU MONDE",
                "Générer une planète",
                new ColorRGBA(0.15f, 0.25f, 0.5f, 0.85f),  // bleu foncé
                true  // actif
        );
        playMenuNode.attachChild(newWorldCard);

        // Carte "Continuer"
        Node continueCard = makeCard(
                "btn_continue",
                cardsStartX + cardW + gap, cardsY,
                cardW, cardH,
                "CONTINUER",
                "Aucune sauvegarde",
                new ColorRGBA(0.2f, 0.2f, 0.2f, 0.6f),  // grisé
                false  // inactif
        );
        playMenuNode.attachChild(continueCard);

        // Bouton retour en bas à gauche
        float backSize = screenH * 0.022f;
        BitmapText backText = makeText("← RETOUR", backSize, SUBTITLE_COLOR);
        Node backBtn = new Node("btn_back");
        backBtn.attachChild(backText);
        backBtn.setLocalTranslation(screenW * 0.05f, screenH * 0.12f, 1);
        playMenuNode.attachChild(backBtn);

        app.getGuiNode().attachChild(playMenuNode);
    }

    private Node makeCard(String btnName, float x, float y, float w, float h,
                          String label, String subtitle, ColorRGBA bgColor, boolean active) {
        Node card = new Node(btnName);

        // Fond de la carte
        Geometry bg = makeQuad(w, h, bgColor);
        bg.setLocalTranslation(0, -h, 0);
        card.attachChild(bg);

        // Bordure (ligne du haut)
        ColorRGBA borderColor = active
                ? new ColorRGBA(0.4f, 0.7f, 1f, 0.9f)
                : new ColorRGBA(0.4f, 0.4f, 0.4f, 0.5f);
        Geometry border = makeQuad(w, h * 0.008f, borderColor);
        border.setLocalTranslation(0, -h * 0.008f, 1);
        card.attachChild(border);

        // Texte principal centré
        float labelSize = h * 0.1f;
        ColorRGBA labelColor = active ? TITLE_COLOR : new ColorRGBA(0.5f, 0.5f, 0.5f, 0.7f);
        BitmapText labelText = makeText(label, labelSize, labelColor);
        float labelX = (w - labelText.getLineWidth()) / 2f;
        float labelY = -h * 0.45f;
        labelText.setLocalTranslation(labelX, labelY, 1);
        card.attachChild(labelText);

        // Sous-titre centré
        float subSize = h * 0.055f;
        ColorRGBA subColor = active ? SUBTITLE_COLOR : new ColorRGBA(0.4f, 0.4f, 0.4f, 0.5f);
        BitmapText subText = makeText(subtitle, subSize, subColor);
        float subX = (w - subText.getLineWidth()) / 2f;
        float subY = labelY - labelSize * 1.2f;
        subText.setLocalTranslation(subX, subY, 1);
        card.attachChild(subText);

        // Position de la carte dans le monde
        card.setLocalTranslation(x, y, 1);

        return card;
    }

    private Geometry makeQuad(float w, float h, ColorRGBA color) {
        Geometry g = new Geometry("quad", new Quad(w, h));
        Material mat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        g.setMaterial(mat);
        return g;
    }

    private Geometry makeLineH(float x, float y, float width, ColorRGBA color) {
        Geometry line = makeQuad(width, 2f, color);
        line.setLocalTranslation(x, y, 1);
        return line;
    }

    // ═══ UPDATE — splash pulse + hover ═══
    @Override
    public void update(float tpf) {
        super.update(tpf);
        timeSinceInit += tpf;
        if (!isEnabled()) return;
        splashTime += tpf;

        float scale = 1f + 0.08f * FastMath.sin(splashTime * 3.5f);
        splashText.setLocalScale(scale);

        // Hover — pickButton SANS logs
        hoveredButton = pickButtonSilent();
        if (hoveredButton != null && !hoveredButton.equals(lastHovered)) {
            updateHover(hoveredButton, true);
            if (lastHovered != null) updateHover(lastHovered, false);
        } else if (hoveredButton == null && lastHovered != null) {
            updateHover(lastHovered, false);
        }
        lastHovered = hoveredButton;
    }

    private void updateHover(String btnId, boolean hovered) {
        Node btn = (Node) menuNode.getChild("btn_" + btnId);
        if (btn == null && playMenuNode != null) {
            btn = (Node) playMenuNode.getChild("btn_" + btnId);
        }
        if (btn == null) return;
        Geometry bg = (Geometry) btn.getChild("bg");
        if (bg != null) {
            bg.getMaterial().setColor("Color", hovered ? BTN_HOVER : BTN_BG);
        }
    }

    private String pickButton() {
        Vector2f cursor = app.getInputManager().getCursorPosition();
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(new Vector3f(cursor.x, cursor.y, 10), new Vector3f(0, 0, -1));

        if (showingPlayMenu) {
            playMenuNode.collideWith(ray, results);
        } else {
            menuNode.collideWith(ray, results);
        }

        if (results.size() > 0) {
            Spatial hit = results.getClosestCollision().getGeometry();
            while (hit != null) {
                String n = hit.getName();
                if (n != null && n.startsWith("btn_")) {
                    System.out.println("[CLICK] Found: " + n + " → '" + n.substring(4) + "'");
                    return n.substring(4);
                }
                hit = hit.getParent();
            }
        }
        System.out.println("[CLICK] Nothing found. showingPlayMenu=" + showingPlayMenu);
        return null;
    }


    // Version silencieuse pour le hover (pas de println)
    private String pickButtonSilent() {
        Vector2f cursor = app.getInputManager().getCursorPosition();
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(new Vector3f(cursor.x, cursor.y, 10), new Vector3f(0, 0, -1));

        if (showingPlayMenu) {
            playMenuNode.collideWith(ray, results);
        } else {
            menuNode.collideWith(ray, results);
        }

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

    // ═══ CLICK ═══
    private final ActionListener clickListener = (name, isPressed, tpf) -> {
        if (!isPressed || !isEnabled()) return;
        if (timeSinceInit < 0.5f) return;
        String btn = pickButton();
        if (btn == null) return;
        System.out.println("[TitleScreen] Clic sur: " + btn);
        System.out.println("[DEBUG] btn = '" + btn + "' showingPlayMenu=" + showingPlayMenu);

        switch (btn) {
            case "solo":
                menuNode.setCullHint(Spatial.CullHint.Always);
                playMenuNode.setCullHint(Spatial.CullHint.Inherit);
                showingPlayMenu = true;
                break;
            case "multiplayer":
                System.out.println("Multijoueur — pas encore implémenté");
                break;
            case "settings":
                System.out.println("Paramètres — pas encore implémenté");
                break;
            case "quit":
                app.stop();
                break;
            case "newworld":
                System.out.println(">>> NOUVEAU MONDE CLIQUÉ <<<");
                app.getStateManager().detach(this);      // ← déclenche cleanup()
                ((StellarGenesisApp) app).initGame();    // ← PAS de detachAllChildren()
                break;
            case "continue":
                System.out.println("Continuer — pas de sauvegarde");
                break;
            case "back":
                playMenuNode.setCullHint(Spatial.CullHint.Always);
                menuNode.setCullHint(Spatial.CullHint.Inherit);
                showingPlayMenu = false;
                break;
        }
    };

    @Override
    public void cleanup() {
        super.cleanup();

        // Détacher les deux nœuds du menu
        if (menuNode != null) {
            menuNode.removeFromParent();
        }
        if (playMenuNode != null) {
            playMenuNode.removeFromParent();
        }

        // Nettoyer les inputs
        app.getInputManager().removeListener(clickListener);
        if (app.getInputManager().hasMapping("click")) {
            app.getInputManager().deleteMapping("click");
        }

        System.out.println("[TitleScreen] cleanup — guiNode children: "
                + app.getGuiNode().getChildren().size());
    }
}
