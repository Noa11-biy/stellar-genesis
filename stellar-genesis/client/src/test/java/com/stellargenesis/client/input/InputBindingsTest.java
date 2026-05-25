package com.stellargenesis.client.input;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link InputBindings}.
 *
 * <p>Utilise Mockito pour simuler {@link InputManager} de jME, qui est trop
 * lourd à instancier dans un test unitaire (besoin d'un contexte OpenGL).
 *
 * <p>Stratégie : on capture le {@link ActionListener} que {@code InputBindings}
 * enregistre auprès de jME, puis on simule des événements en l'appelant
 * directement.
 */
class InputBindingsTest {

    private InputManager mockInputManager;
    private InputContextManager contextManager;
    private InputBindings bindings;
    private ActionListener capturedListener;

    @BeforeEach
    void setUp() {
        mockInputManager = mock(InputManager.class);
        // hasMapping retourne false par défaut → simule "pas encore enregistré"
        when(mockInputManager.hasMapping(anyString())).thenReturn(true);

        contextManager = new InputContextManager();
        bindings = new InputBindings(mockInputManager, contextManager);
    }

    /**
     * Capture le listener jME que {@code InputBindings} a enregistré, pour
     * pouvoir simuler des événements directement.
     */
    private ActionListener captureListener() {
        if (capturedListener == null) {
            ArgumentCaptor<ActionListener> captor = ArgumentCaptor.forClass(ActionListener.class);
            verify(mockInputManager, atLeastOnce()).addListener(captor.capture(), any(String[].class));
            capturedListener = captor.getValue();
        }
        return capturedListener;
    }

    // ═══════════════════════════════════════════════
    //  Construction & validation
    // ═══════════════════════════════════════════════

    @Test
    void constructeur_refuseNull() {
        assertThrows(NullPointerException.class,
                () -> new InputBindings(null, contextManager));
        assertThrows(NullPointerException.class,
                () -> new InputBindings(mockInputManager, null));
    }

    @Test
    void bind_refuseContextesVides() {
        assertThrows(IllegalArgumentException.class,
                () -> bindings.bind(GameAction.JUMP, ActionType.TRIGGER, KeyInput.KEY_SPACE));
    }

    @Test
    void bind_refuseAxisPourLInstant() {
        assertThrows(UnsupportedOperationException.class,
                () -> bindings.bind(GameAction.LOOK_X, ActionType.AXIS,
                        KeyInput.KEY_M, InputContext.GAMEPLAY));
    }

    // ═══════════════════════════════════════════════
    //  Enregistrement côté jME
    // ═══════════════════════════════════════════════

    @Test
    void bind_enregistreLeMappingJme() {
        bindings.bind(GameAction.MOVE_FORWARD, ActionType.HOLD,
                KeyInput.KEY_W, InputContext.GAMEPLAY);

        // Vérifie qu'un mapping a été ajouté
        verify(mockInputManager).addMapping(eq("MOVE_FORWARD_17"), any());
        // Vérifie qu'un listener a été enregistré sur ce mapping
        verify(mockInputManager).addListener(any(ActionListener.class), eq("MOVE_FORWARD_17"));
    }

    // ═══════════════════════════════════════════════
    //  HOLD — état de la touche
    // ═══════════════════════════════════════════════

    @Test
    void hold_etatInitialEstFalse() {
        bindings.bind(GameAction.MOVE_FORWARD, ActionType.HOLD,
                KeyInput.KEY_W, InputContext.GAMEPLAY);
        contextManager.pushContext(InputContext.GAMEPLAY);

        assertFalse(bindings.isHeld(GameAction.MOVE_FORWARD));
    }

    @Test
    void hold_passeATrueQuandPresseDansLeBonContexte() {
        bindings.bind(GameAction.MOVE_FORWARD, ActionType.HOLD,
                KeyInput.KEY_W, InputContext.GAMEPLAY);
        contextManager.pushContext(InputContext.GAMEPLAY);

        // Simule press
        captureListener().onAction("MOVE_FORWARD_17", true, 0f);

        assertTrue(bindings.isHeld(GameAction.MOVE_FORWARD));
    }

    @Test
    void hold_resteFalseSiContexteInactif() {
        bindings.bind(GameAction.MOVE_FORWARD, ActionType.HOLD,
                KeyInput.KEY_W, InputContext.GAMEPLAY);
        contextManager.pushContext(InputContext.INVENTORY);  // mauvais contexte

        captureListener().onAction("MOVE_FORWARD_17", true, 0f);

        assertFalse(bindings.isHeld(GameAction.MOVE_FORWARD));
    }

    @Test
    void hold_releaseToujoursTraite_memesiContexteInactif() {
        // C'est la règle "release inconditionnel"
        bindings.bind(GameAction.MOVE_FORWARD, ActionType.HOLD,
                KeyInput.KEY_W, InputContext.GAMEPLAY);
        contextManager.pushContext(InputContext.GAMEPLAY);

        // Press en GAMEPLAY : isHeld devient true
        captureListener().onAction("MOVE_FORWARD_17", true, 0f);
        assertTrue(bindings.isHeld(GameAction.MOVE_FORWARD));

        // On bascule vers INVENTORY (contexte qui n'a pas MOVE_FORWARD)
        contextManager.pushContext(InputContext.INVENTORY);

        // Release : doit quand même remettre isHeld à false
        captureListener().onAction("MOVE_FORWARD_17", false, 0f);
        assertFalse(bindings.isHeld(GameAction.MOVE_FORWARD),
                "Le release doit toujours être traité, sinon la touche reste coincée");
    }

    @Test
    void hold_actionMultiContextes() {
        // MOVE_FORWARD est actif en GAMEPLAY ET FREECAM
        bindings.bind(GameAction.MOVE_FORWARD, ActionType.HOLD,
                KeyInput.KEY_W, InputContext.GAMEPLAY, InputContext.FREECAM);

        contextManager.pushContext(InputContext.FREECAM);
        captureListener().onAction("MOVE_FORWARD_17", true, 0f);
        assertTrue(bindings.isHeld(GameAction.MOVE_FORWARD));
    }

    // ═══════════════════════════════════════════════
    //  TRIGGER — callback
    // ═══════════════════════════════════════════════

    @Test
    void trigger_callbackAppeleQuandActif() {
        bindings.bind(GameAction.JUMP, ActionType.TRIGGER,
                KeyInput.KEY_SPACE, InputContext.GAMEPLAY);

        boolean[] called = {false};
        bindings.onTrigger(GameAction.JUMP, () -> called[0] = true);

        contextManager.pushContext(InputContext.GAMEPLAY);
        captureListener().onAction("JUMP_57", true, 0f);

        assertTrue(called[0], "Le callback JUMP aurait dû être appelé");
    }

    @Test
    void trigger_callbackPasAppeleSiContexteInactif() {
        bindings.bind(GameAction.JUMP, ActionType.TRIGGER,
                KeyInput.KEY_SPACE, InputContext.GAMEPLAY);

        boolean[] called = {false};
        bindings.onTrigger(GameAction.JUMP, () -> called[0] = true);

        contextManager.pushContext(InputContext.INVENTORY);  // mauvais contexte
        captureListener().onAction("JUMP_57", true, 0f);

        assertFalse(called[0]);
    }

    @Test
    void trigger_callbackPasAppeleSurRelease() {
        // TRIGGER ne se déclenche que sur press, pas sur release
        bindings.bind(GameAction.JUMP, ActionType.TRIGGER,
                KeyInput.KEY_SPACE, InputContext.GAMEPLAY);

        int[] callCount = {0};
        bindings.onTrigger(GameAction.JUMP, () -> callCount[0]++);

        contextManager.pushContext(InputContext.GAMEPLAY);
        captureListener().onAction("JUMP_57", true, 0f);   // press
        captureListener().onAction("JUMP_57", false, 0f);  // release

        assertEquals(1, callCount[0], "TRIGGER doit s'appeler une seule fois (press uniquement)");
    }

    @Test
    void trigger_memeToucheActionsDifferentesParContexte() {
        // Clic gauche : MINE en GAMEPLAY, INVENTORY_CLICK en INVENTORY
        bindings.bind(GameAction.MINE, ActionType.TRIGGER,
                KeyInput.KEY_M, InputContext.GAMEPLAY);
        bindings.bind(GameAction.INVENTORY_CLICK, ActionType.TRIGGER,
                KeyInput.KEY_M, InputContext.INVENTORY);

        int[] mineCalls = {0};
        int[] invCalls = {0};
        bindings.onTrigger(GameAction.MINE, () -> mineCalls[0]++);
        bindings.onTrigger(GameAction.INVENTORY_CLICK, () -> invCalls[0]++);

        // En GAMEPLAY : MINE doit se déclencher
        contextManager.pushContext(InputContext.GAMEPLAY);
        captureListener().onAction("MINE_50", true, 0f);
        assertEquals(1, mineCalls[0]);
        assertEquals(0, invCalls[0]);

        // En INVENTORY : INVENTORY_CLICK doit se déclencher
        contextManager.pushContext(InputContext.INVENTORY);
        captureListener().onAction("INVENTORY_CLICK_50", true, 0f);
        assertEquals(1, mineCalls[0]);
        assertEquals(1, invCalls[0]);
    }

    // ═══════════════════════════════════════════════
    //  Contexte DEBUG (toujours actif)
    // ═══════════════════════════════════════════════

    @Test
    void debug_toujoursActifMemeSansPile() {
        bindings.bind(GameAction.TOGGLE_DEBUG_HUD, ActionType.TRIGGER,
                KeyInput.KEY_F1, InputContext.DEBUG);

        boolean[] called = {false};
        bindings.onTrigger(GameAction.TOGGLE_DEBUG_HUD, () -> called[0] = true);

        // Aucun contexte poussé, mais DEBUG est implicitement actif
        captureListener().onAction("TOGGLE_DEBUG_HUD_59", true, 0f);

        assertTrue(called[0], "DEBUG doit fonctionner même sans contexte poussé");
    }

    // ═══════════════════════════════════════════════
    //  Cleanup
    // ═══════════════════════════════════════════════

    @Test
    void cleanup_supprimeTousLesMappings() {
        bindings.bind(GameAction.JUMP, ActionType.TRIGGER,
                KeyInput.KEY_SPACE, InputContext.GAMEPLAY);
        bindings.bind(GameAction.MOVE_FORWARD, ActionType.HOLD,
                KeyInput.KEY_W, InputContext.GAMEPLAY);

        bindings.cleanup();

        verify(mockInputManager).deleteMapping("JUMP_57");
        verify(mockInputManager).deleteMapping("MOVE_FORWARD_17");
        verify(mockInputManager).removeListener(any(ActionListener.class));
    }

    @Test
    void cleanup_resetIsHeld() {
        bindings.bind(GameAction.MOVE_FORWARD, ActionType.HOLD,
                KeyInput.KEY_W, InputContext.GAMEPLAY);
        contextManager.pushContext(InputContext.GAMEPLAY);
        captureListener().onAction("MOVE_FORWARD_17", true, 0f);

        bindings.cleanup();

        assertFalse(bindings.isHeld(GameAction.MOVE_FORWARD));
    }
}
