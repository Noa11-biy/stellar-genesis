package com.stellargenesis.client.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.stellargenesis.core.inventory.Inventory;
import com.stellargenesis.core.inventory.ItemStack;

public class InventoryScreen {

    // === Layout ===
    private static final int COLUMNS = 9;
    private static final float SLOT_SIZE = 50f;
    private static final float SLOT_MARGIN = 4f;
    private static final float PADDING = 20f;

    // === Références jME ===
    private Node inventoryNode;
    private Node guiNode;
    private AssetManager assetManager;
    private BitmapFont font;
    private Camera cam;

    // === État ===
    private boolean visible = false;
    private int slotCount;

    // === Éléments graphiques par slot ===
    private Geometry[] slotBackgrounds;
    private BitmapText[] slotNames;
    private BitmapText[] slotQuantities;

    // === Fond semi-transparent ===
    private Geometry backgroundPanel;

    // === État sélection ===
    private int selectedSlot = -1;
    private Geometry selectionBorder;
    private static final float SELECTED_SCALE = 1.15f;

    // === Drag & Drop ===
    private int dragSourceSlot = -1;
    private boolean dragging = false;
    private Node dragNode;

    // === Positions calculées (pour réutiliser dans getSlotAt, onClick, etc.) ===
    private float panelX, panelY, panelWidth, panelHeight;
    private float gridStartX, gridStartY;
    private int rows;

    public InventoryScreen(AssetManager assetManager, Node guiNode, Camera cam) {
        this.assetManager = assetManager;
        this.guiNode = guiNode;
        this.cam = cam;
        this.font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        inventoryNode = new Node("InventoryScreen");
    }

    public void build(int slotCount) {
        this.slotCount = slotCount;
        inventoryNode.detachAllChildren();

        rows = (int) Math.ceil((double) slotCount / COLUMNS);

        float gridWidth = COLUMNS * (SLOT_SIZE + SLOT_MARGIN) - SLOT_MARGIN;
        float gridHeight = rows * (SLOT_SIZE + SLOT_MARGIN) - SLOT_MARGIN;

        panelWidth = gridWidth + PADDING * 2;
        panelHeight = gridHeight + PADDING * 2;

        panelX = (cam.getWidth() - panelWidth) / 2f;
        panelY = (cam.getHeight() - panelHeight) / 2f;

        // gridStartX/Y = coin bas-gauche de la grille dans le GUI
        gridStartX = panelX + PADDING;
        gridStartY = panelY + PADDING;

        // --- Fond du panneau ---
        backgroundPanel = createColoredQuad(panelWidth, panelHeight,
                new ColorRGBA(0.1f, 0.1f, 0.15f, 0.85f));
        backgroundPanel.setLocalTranslation(panelX, panelY, 0);
        inventoryNode.attachChild(backgroundPanel);

        // --- Slots ---
        slotBackgrounds = new Geometry[slotCount];
        slotNames = new BitmapText[slotCount];
        slotQuantities = new BitmapText[slotCount];

        for (int i = 0; i < slotCount; i++) {
            int col = i % COLUMNS;
            // Ligne 0 en haut → inverser
            int row = rows - 1 - (i / COLUMNS);

            float x = gridStartX + col * (SLOT_SIZE + SLOT_MARGIN);
            float y = gridStartY + row * (SLOT_SIZE + SLOT_MARGIN);

            slotBackgrounds[i] = createColoredQuad(SLOT_SIZE, SLOT_SIZE,
                    new ColorRGBA(0.25f, 0.25f, 0.3f, 1f));
            slotBackgrounds[i].setLocalTranslation(x, y, 1);
            inventoryNode.attachChild(slotBackgrounds[i]);

            slotNames[i] = new BitmapText(font, false);
            slotNames[i].setSize(11f);
            slotNames[i].setColor(ColorRGBA.White);
            slotNames[i].setLocalTranslation(x + 4, y + SLOT_SIZE - 6, 2);
            inventoryNode.attachChild(slotNames[i]);

            slotQuantities[i] = new BitmapText(font, false);
            slotQuantities[i].setSize(11f);
            slotQuantities[i].setColor(ColorRGBA.Yellow);
            slotQuantities[i].setLocalTranslation(x + 4, y + 16, 2);
            inventoryNode.attachChild(slotQuantities[i]);
        }

        buildSelectionBorder();
    }

    /**
     * Met à jour le contenu affiché depuis l'inventaire logique.
     */
    public void refresh(Inventory inventory) {
        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = inventory.getSlot(i);

            if (stack != null && !stack.isEmpty()) {
                String name = stack.getType().name();
                if (name.length() > 5) name = name.substring(0, 5);
                slotNames[i].setText(name);
                slotQuantities[i].setText("x" + stack.getQuantity());
                slotBackgrounds[i].getMaterial().setColor("Color",
                        new ColorRGBA(0.3f, 0.35f, 0.4f, 1f));
            } else {
                slotNames[i].setText("");
                slotQuantities[i].setText("");
                slotBackgrounds[i].getMaterial().setColor("Color",
                        new ColorRGBA(0.25f, 0.25f, 0.3f, 1f));
            }
        }
    }

    public void toggle() {
        visible = !visible;
        if (visible) {
            guiNode.attachChild(inventoryNode);
        } else {
            cancelDrag();
            deselectSlot();
            inventoryNode.removeFromParent();
        }
    }

    public boolean isVisible() { return visible; }

    // ========================================================================
    //  CLICK / SÉLECTION
    // ========================================================================

    public int onClick(float clickX, float clickY, Inventory inventory) {
        if (!visible || inventory == null) return -1;

        int slot = getSlotAt(clickX, clickY);
        if (slot < 0) {
            deselectSlot();
            return -1;
        }

        ItemStack stack = inventory.getSlot(slot);
        if (stack != null && !stack.isEmpty()) {
            float[] pos = getSlotScreenPos(slot);
            selectSlot(slot, pos[0], pos[1]);
            return slot;
        } else {
            deselectSlot();
            return -1;
        }
    }

    // ========================================================================
    //  DRAG & DROP
    // ========================================================================

    public void onMouseDown(float x, float y, Inventory inventory) {
        if (!visible || inventory == null) return;

        int slot = getSlotAt(x, y);
        if (slot < 0) return;

        ItemStack stack = inventory.getSlot(slot);
        if (stack == null || stack.isEmpty()) return;

        dragging = true;
        dragSourceSlot = slot;

        if (dragNode == null) {
            dragNode = new Node("DragNode");
            inventoryNode.attachChild(dragNode);
        }
        dragNode.detachAllChildren();

        // Icône flottante
        Geometry icon = createColoredQuad(SLOT_SIZE * 0.8f, SLOT_SIZE * 0.8f,
                getItemColor(stack));
        icon.setLocalTranslation(0, 0, 3);
        dragNode.attachChild(icon);

        // Label
        BitmapText label = new BitmapText(font, false);
        label.setText(stack.getType().name());
        label.setSize(12f);
        label.setColor(ColorRGBA.White);
        label.setLocalTranslation(SLOT_SIZE * 0.85f, SLOT_SIZE * 0.5f, 3);
        dragNode.attachChild(label);

        // Positionner sous la souris
        dragNode.setLocalTranslation(x - SLOT_SIZE * 0.4f,
                y - SLOT_SIZE * 0.4f, 0);
    }

    public void onMouseMove(float x, float y) {
        if (!dragging || dragNode == null) return;
        dragNode.setLocalTranslation(x - SLOT_SIZE * 0.4f,
                y - SLOT_SIZE * 0.4f, 0);
    }

    public void onMouseUp(float x, float y, Inventory inventory) {
        if (!dragging || inventory == null) {
            cancelDrag();
            return;
        }

        int targetSlot = getSlotAt(x, y);

        if (targetSlot >= 0 && targetSlot != dragSourceSlot) {
            inventory.swapSlots(dragSourceSlot, targetSlot);
            System.out.println("[Inventaire] Swap slot " + dragSourceSlot
                    + " <-> slot " + targetSlot);
        } else {
            System.out.println("[Inventaire] Drag annulé");
        }

        cancelDrag();
        refresh(inventory);
    }

    private void cancelDrag() {
        dragging = false;
        dragSourceSlot = -1;
        if (dragNode != null) {
            dragNode.detachAllChildren();
        }
    }

    public boolean isDragging() { return dragging; }

    // ========================================================================
    //  POSITIONNEMENT DES SLOTS (méthode unique, réutilisée partout)
    // ========================================================================

    /**
     * Retourne la position écran [x, y] du coin bas-gauche du slot i.
     * Ligne 0 de l'inventaire = en HAUT visuellement.
     */
    private float[] getSlotScreenPos(int i) {
        int col = i % COLUMNS;
        int row = rows - 1 - (i / COLUMNS);
        float x = gridStartX + col * (SLOT_SIZE + SLOT_MARGIN);
        float y = gridStartY + row * (SLOT_SIZE + SLOT_MARGIN);
        return new float[]{x, y};
    }

    /**
     * Retourne l'index du slot sous la position écran (x, y).
     * Retourne -1 si aucun slot touché.
     */
    private int getSlotAt(float x, float y) {
        for (int i = 0; i < slotCount; i++) {
            float[] pos = getSlotScreenPos(i);
            if (x >= pos[0] && x <= pos[0] + SLOT_SIZE &&
                    y >= pos[1] && y <= pos[1] + SLOT_SIZE) {
                return i;
            }
        }
        return -1;
    }

    // ========================================================================
    //  SÉLECTION VISUELLE
    // ========================================================================

    private void buildSelectionBorder() {
        float borderSize = SLOT_SIZE * SELECTED_SCALE + 4f;
        selectionBorder = createColoredQuad(borderSize, borderSize,
                new ColorRGBA(1f, 0.85f, 0.2f, 0.9f));
        selectionBorder.setCullHint(Spatial.CullHint.Always);
        inventoryNode.attachChild(selectionBorder);
    }

    private void selectSlot(int index, float slotX, float slotY) {
        if (selectedSlot >= 0 && selectedSlot < slotCount) {
            // Remettre l'ancien slot à sa taille normale
            float[] oldPos = getSlotScreenPos(selectedSlot);
            slotBackgrounds[selectedSlot].setLocalScale(1f);
            slotBackgrounds[selectedSlot].setLocalTranslation(oldPos[0], oldPos[1], 1f);
        }

        selectedSlot = index;

        float offset = SLOT_SIZE * (SELECTED_SCALE - 1f) / 2f;
        slotBackgrounds[index].setLocalScale(SELECTED_SCALE);
        slotBackgrounds[index].setLocalTranslation(
                slotX - offset, slotY - offset, 2f);

        float borderSize = SLOT_SIZE * SELECTED_SCALE + 4f;
        selectionBorder.setLocalTranslation(
                slotX - offset - 2f, slotY - offset - 2f, 1f);
        selectionBorder.setCullHint(Spatial.CullHint.Inherit);
    }

    private void deselectSlot() {
        if (selectedSlot >= 0 && selectedSlot < slotCount) {
            float[] pos = getSlotScreenPos(selectedSlot);
            slotBackgrounds[selectedSlot].setLocalScale(1f);
            slotBackgrounds[selectedSlot].setLocalTranslation(pos[0], pos[1], 1f);
        }
        selectedSlot = -1;
        if (selectionBorder != null) {
            selectionBorder.setCullHint(Spatial.CullHint.Always);
        }
    }

    public int getSelectedSlot() { return selectedSlot; }

    // ========================================================================
    //  UTILITAIRES
    // ========================================================================

    private Geometry createColoredQuad(float width, float height, ColorRGBA color) {
        Quad quad = new Quad(width, height);
        Geometry geom = new Geometry("quad", quad);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(
                com.jme3.material.RenderState.BlendMode.Alpha);
        geom.setMaterial(mat);
        geom.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Gui);
        return geom;
    }

    private ColorRGBA getItemColor(ItemStack stack) {
        if (stack == null) return ColorRGBA.DarkGray;
        String name = stack.getType().name().toLowerCase();
        if (name.contains("iron"))    return new ColorRGBA(0.6f, 0.6f, 0.6f, 1f);
        if (name.contains("copper"))  return new ColorRGBA(0.8f, 0.4f, 0.1f, 1f);
        if (name.contains("coal"))    return new ColorRGBA(0.2f, 0.2f, 0.2f, 1f);
        if (name.contains("gold"))    return new ColorRGBA(1f, 0.84f, 0f, 1f);
        if (name.contains("silicon")) return new ColorRGBA(0.5f, 0.5f, 0.7f, 1f);
        return new ColorRGBA(0.4f, 0.7f, 0.3f, 1f);
    }
}
