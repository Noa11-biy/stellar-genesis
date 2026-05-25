package com.stellargenesis.client.input;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputContextManagerTest {

    private InputContextManager manager;

    @BeforeEach
    void setUp() {
        manager = new InputContextManager();
    }

    // ─────────────────────────────────────────────
    //  État initial
    // ─────────────────────────────────────────────

    @Test
    void pileVideAuDepart() {
        assertNull(manager.currentContext());
        assertEquals(0, manager.depth());
    }

    @Test
    void debugToujoursActifMemeSiPileVide() {
        assertTrue(manager.isActive(InputContext.DEBUG));
    }

    // ─────────────────────────────────────────────
    //  Push / pop basiques
    // ─────────────────────────────────────────────

    @Test
    void pushRendContexteActif() {
        manager.pushContext(InputContext.GAMEPLAY);

        assertEquals(InputContext.GAMEPLAY, manager.currentContext());
        assertTrue(manager.isActive(InputContext.GAMEPLAY));
        assertEquals(1, manager.depth());
    }

    @Test
    void popRetireLeSommet() {
        manager.pushContext(InputContext.GAMEPLAY);
        InputContext popped = manager.popContext();

        assertEquals(InputContext.GAMEPLAY, popped);
        assertNull(manager.currentContext());
    }

    // ─────────────────────────────────────────────
    //  Empilement (cas central)
    // ─────────────────────────────────────────────

    @Test
    void contexteEmpileMasqueLePrecedent() {
        manager.pushContext(InputContext.GAMEPLAY);
        manager.pushContext(InputContext.INVENTORY);

        // INVENTORY est au sommet, GAMEPLAY est masqué
        assertTrue(manager.isActive(InputContext.INVENTORY));
        assertFalse(manager.isActive(InputContext.GAMEPLAY));
    }

    @Test
    void popRestaureLeContextePrecedent() {
        manager.pushContext(InputContext.GAMEPLAY);
        manager.pushContext(InputContext.INVENTORY);
        manager.popContext();

        // Retour automatique à GAMEPLAY
        assertTrue(manager.isActive(InputContext.GAMEPLAY));
        assertFalse(manager.isActive(InputContext.INVENTORY));
    }

    @Test
    void empilementMultipleEtDepilementOrdonne() {
        manager.pushContext(InputContext.MENU);
        manager.pushContext(InputContext.GAMEPLAY);
        manager.pushContext(InputContext.PAUSE);

        assertEquals(InputContext.PAUSE, manager.popContext());
        assertEquals(InputContext.GAMEPLAY, manager.popContext());
        assertEquals(InputContext.MENU, manager.popContext());
        assertNull(manager.currentContext());
    }

    // ─────────────────────────────────────────────
    //  DEBUG cas spécial
    // ─────────────────────────────────────────────

    @Test
    void debugRestActifEnPresenceDAutresContextes() {
        manager.pushContext(InputContext.GAMEPLAY);
        assertTrue(manager.isActive(InputContext.DEBUG));

        manager.pushContext(InputContext.INVENTORY);
        assertTrue(manager.isActive(InputContext.DEBUG));
    }

    @Test
    void pushDebugInterdit() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.pushContext(InputContext.DEBUG));
    }

    // ─────────────────────────────────────────────
    //  Erreurs / robustesse
    // ─────────────────────────────────────────────

    @Test
    void popSurPileVideLeveException() {
        assertThrows(IllegalStateException.class,
                () -> manager.popContext());
    }

    @Test
    void pushNullLeveException() {
        assertThrows(NullPointerException.class,
                () -> manager.pushContext(null));
    }

    @Test
    void isActiveNullLeveException() {
        assertThrows(NullPointerException.class,
                () -> manager.isActive(null));
    }

    // ─────────────────────────────────────────────
    //  clear()
    // ─────────────────────────────────────────────

    @Test
    void clearVideToutLaPile() {
        manager.pushContext(InputContext.GAMEPLAY);
        manager.pushContext(InputContext.INVENTORY);

        manager.clear();

        assertNull(manager.currentContext());
        assertEquals(0, manager.depth());
        // DEBUG reste actif après clear
        assertTrue(manager.isActive(InputContext.DEBUG));
    }
}
