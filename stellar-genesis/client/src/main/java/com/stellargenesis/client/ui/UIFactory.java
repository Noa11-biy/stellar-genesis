package com.stellargenesis.client.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;


/**
 * Fabrique centralisée pour les éléments d'interface.
 *
 * === POURQUOI UNE FABRIQUE ? ===
 * jMonkeyEngine n'a PAS de système UI type "bouton cliquable" intégré.
 * Le guiNode est un simple Node 2D en pixels, (0,0) = coin bas-gauche.
 *
 * On doit tout construire manuellement :
 *   - Un Quad (rectangle) pour le fond du bouton
 *   - Un BitmapText centré dessus pour le label
 *   - Un Material "Unshaded" (pas d'éclairage, couleur plate)
 *
 * En centralisant ici, si tu changes le style visuel → UN seul fichier.
 * C'est le pattern Factory — tu le verras en cours de Design Patterns.
 */

public class UIFactory {

    /** Fond de bouton normal : bleu nuit semi-transparent */
    public static final ColorRGBA BTN_NORMAL = new ColorRGBA(0.06f, 0.08f, 0.18f, 0.80f);

    /** Fond de bouton survolé : bleu plus clair */
    public static final ColorRGBA BTN_HOVER = new ColorRGBA(0.10f, 0.18f, 0.35f, 0.92f);

    /** Accent principale : cyan */
    public static final ColorRGBA ACCENT = new ColorRGBA(0.30f, 0.78f, 1.0f, 1.0f);

    /** Accent secondaire : or pâle */
    public static final ColorRGBA ACCENT_GOLD = new ColorRGBA(1.0f, 0.85f, 0.4f, 1.0f);

    /** Texte Principal : blanc légèrement bleuté */
    public static final ColorRGBA TEXT_BRIGHT = new ColorRGBA(0.92f, 0.95f, 1.0f, 1.0f);

    /** Texte secondaire : gris-bleu */
    public static final ColorRGBA TEXT_DIM = new ColorRGBA(0.45f, 0.52f, 0.62f, 1.0f);

    /** Bordure fine */
    public static final ColorRGBA BORDER_COLOR = new ColorRGBA(0.30f, 0.78f, 1.0f, 0.4f);

    // ======================================
    //  ÉTAT INTERNE
    // ======================================
    private final AssetManager assetManager;
    private final BitmapFont font;

    public UIFactory(AssetManager assetManager){
        this.assetManager = assetManager;
        // Police par défaut de jME
        // Une police custom (.fnt) générée par Hiero ou BMFont
        this.font = assetManager.loadFont("Interface/Fonts/Default.fnt");

    }

    // ======================================
    //  BOUTON
    // ======================================

    /**
     * Crée un bouton = Node contenant :
     *   1. Geometry "btn_xxx_bg"   → rectangle de fond
     *   2. Geometry "btn_xxx_border" → contour fin
     *   3. BitmapText              → label centré
     *
     * Le Node s'appelle "btn_xxx" — c'est ce nom qu'on utilise
     * pour identifier quel bouton a été cliqué (ray picking).
     *
     * @param id     identifiant unique (ex: "solo", "settings")
     * @param label  texte affiché sur le bouton
     * @param w      largeur en pixels
     * @param h      hauteur en pixels
     * @param x      position X du coin bas-gauche
     * @param y      position Y du coin bas-gauche
     */
    public Node createButton(String id, String label, float w, float h, float x, float y){

        String nodeName = "btn_" + id;
        Node btn = new Node(nodeName);

        // --- 1. FOND ---
        // Quad = rectangle 2D (largeur, hauteur)
        Geometry bg = new Geometry(nodeName + "_bg", new Quad(w, h));
        Material bgMat = makeUnshadedMat(BTN_NORMAL);
        bg.setMaterial(bgMat);
        btn.attachChild(bg);

        // --- 2. BORDURE GAUCHE (accent visuel) ---
        // Une fine barre colorée à gauche du bouton, 3px de large
        float borderWidth = 3f;
        Geometry border = new Geometry(nodeName + "_border", new Quad(borderWidth, h));
        border.setMaterial(makeUnshadedMat(ACCENT));
        border.setLocalTranslation(-borderWidth, 0, 0.5f);
        btn.attachChild(border);

        // --- 3. TEXTE CENTRÉ ---
        BitmapText text = new BitmapText(font, false);
        text.setSize(h * 0.38f);
        text.setColor(TEXT_BRIGHT);
        text.setText(label);

        // Centrage : le texte se dessine vers le HAUT à partir de sa position.
        // Pour centrer verticalement : y = (hauteur_bouton + hauteur_texte) / 2
        float textW = text.getLineWidth();
        float textH = text.getLineHeight();
        text.setLocalTranslation(
                (w - textW) / 2f,
                (h + textH) /2f,
                1f
        );
        btn.attachChild(text);

        // --- 4. POSITIONNER LE TOUT ---
        btn.setLocalTranslation(x, y, 0);

        return btn;
    }

    // ======================================
    //  TEXT
    // ======================================

    /**
     * Crée un texte libre positionné en pixels.
     */
    public BitmapText createText(String content, float size, ColorRGBA color,
                                 float x, float y) {
        BitmapText t = new BitmapText(font, false);
        t.setSize(size);
        t.setColor(color);
        t.setText(content);
        t.setLocalTranslation(x, y, 0);
        return t;
    }

    // ======================================
    //  SÉPARATEUR
    // ======================================

    /**
     * Ligne horizontale fine (2px) pour séparer les sections
     */
    public Geometry createSeparator(float width, float x, float y){
        Geometry sep = new Geometry("separator", new Quad(width, 2f));
        sep.setMaterial(makeUnshadedMat(BORDER_COLOR));
        sep.setLocalTranslation(x, y, 0);
        return sep;
    }

    // ======================================
    //  PANNEAU DE FOND (rectangle semi-transparent)
    // ======================================

    /**
     * Rectangle sombre derrière un groupe de boutons
     * Donne de la lisibilité si le fond est un image
     */
    public Geometry createPanel(float w, float h, float x, float y){
        Geometry panel = new Geometry("panel", new Quad(w, h));
        panel.setMaterial(makeUnshadedMat(
                new ColorRGBA(0.02f, 0.03f, 0.08f, 0.65f)
        ));
        panel.setLocalTranslation(x, y, -0.5f);
        return panel;
    }

    // ======================================
    //  UTILITAIRES
    // ======================================

    /**
     * Crée un Material "Unshaded" avec une couleur.
     *
     * "Unshaded" = pas de calcul d'éclairage.
     * C'est ce qu'on veut pour de l'UI 2D (couleur plate).
     * On active le BlendMode.Alpha pour que la transparence fonctionne.
     */
    private Material makeUnshadedMat(ColorRGBA color) {
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        return mat;
    }

    public BitmapFont getFont(){
        return font;
    }

}
