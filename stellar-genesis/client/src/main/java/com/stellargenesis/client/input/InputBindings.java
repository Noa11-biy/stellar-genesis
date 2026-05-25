package com.stellargenesis.client.input;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.*;
import com.jme3.math.Vector2f;

import java.util.*;

/**
 * Système central de binding des inputs.
 *
 * <h2>Rôle</h2>
 * Fait le pont entre :
 * <ul>
 *   <li>jME {@link InputManager} (bas niveau : touches physiques)</li>
 *   <li>Le code de jeu (haut niveau : actions logiques type {@code JUMP})</li>
 * </ul>
 *
 * <h2>Fonctionnalités</h2>
 * <ul>
 *   <li><b>Binding action ↔ touche</b> : associe une {@link GameAction} à une
 *       touche physique, dans un ou plusieurs {@link InputContext}.</li>
 *   <li><b>Filtrage contextuel</b> : seules les actions dont le contexte est
 *       actif sont déclenchées.</li>
 *   <li><b>État HOLD</b> : maintient un booléen "touche pressée" interrogeable
 *       via {@link #isHeld(GameAction)}.</li>
 *   <li><b>Callbacks TRIGGER</b> : appelle un {@link Runnable} enregistré
 *       quand l'action est déclenchée (impulsion unique).</li>
 *   <li><b>Release inconditionnel</b> : les releases sont toujours traités,
 *       même si le contexte n'est plus actif, pour éviter les "touches
 *       fantômes" coincées à {@code true}.</li>
 * </ul>
 *
 * <h2>Exemple d'utilisation</h2>
 * <pre>
 *   InputBindings bindings = new InputBindings(inputManager, contextManager);
 *
 *   // Bind d'une action HOLD
 *   bindings.bind(GameAction.MOVE_FORWARD, ActionType.HOLD,
 *                 KeyInput.KEY_W, InputContext.GAMEPLAY, InputContext.FREECAM);
 *
 *   // Bind d'une action TRIGGER avec callback
 *   bindings.bind(GameAction.JUMP, ActionType.TRIGGER,
 *                 KeyInput.KEY_SPACE, InputContext.GAMEPLAY);
 *   bindings.onTrigger(GameAction.JUMP, () -> player.jump());
 *
 *   // Dans la boucle de jeu
 *   if (bindings.isHeld(GameAction.MOVE_FORWARD)) {
 *       player.moveForward(tpf);
 *   }
 * </pre>
 */
public class InputBindings {

    // ─────────────────────────────────────────────
    //  Classe interne : un binding
    // ─────────────────────────────────────────────

    /**
     * Représente une association action ↔ touche dans un ensemble de contextes.
     * Immuable.
     */
    private static final class Binding {
        final GameAction action;
        final ActionType type;
        final Set<InputContext> contexts;
        /** Nom unique pour jME (ex : "MOVE_FORWARD_17" ou "LOOK_X_AXIS0_NEG"). */
        final String mappingName;
        /** +1 ou -1, utilisé uniquement pour AXIS. Ignoré pour HOLD/TRIGGER. */
        final float axisSign;

        Binding(GameAction action, ActionType type, Set<InputContext> contexts,
                String mappingName, float axisSign) {
            this.action = action;
            this.type = type;
            this.contexts = Collections.unmodifiableSet(EnumSet.copyOf(contexts));
            this.mappingName = mappingName;
            this.axisSign = axisSign;
        }
    }

    // ─────────────────────────────────────────────
    //  Champs
    // ─────────────────────────────────────────────

    private final InputManager inputManager;
    private final InputContextManager contextManager;

    /** Tous les bindings enregistrés. */
    private final List<Binding> bindings = new ArrayList<>();

    /** État "touche maintenue" pour chaque action HOLD. */
    private final EnumMap<GameAction, Boolean> heldStates = new EnumMap<>(GameAction.class);

    /** Callbacks pour les actions TRIGGER. */
    private final EnumMap<GameAction, Runnable> triggerCallbacks = new EnumMap<>(GameAction.class);

    /** Valeurs accumulées pour les actions AXIS, consommées à chaque lecture. */
    private final EnumMap<GameAction, Float> axisValues = new EnumMap<>(GameAction.class);

    private final Map<GameAction, Runnable> pressCallbacks = new EnumMap<>(GameAction.class);
    private final Map<GameAction, Runnable> releaseCallbacks = new EnumMap<>(GameAction.class);

    /** Listener jME unique, dispatch interne. */
    private final ActionListener jmeListener = this::onJmeAction;

    /** Listener jME pour les axes (souris). */
    private final AnalogListener jmeAnalogListener = this::onJmeAnalog;

    // ─────────────────────────────────────────────
    //  Construction
    // ─────────────────────────────────────────────

    public InputBindings(InputManager inputManager, InputContextManager contextManager) {
        this.inputManager = Objects.requireNonNull(inputManager);
        this.contextManager = Objects.requireNonNull(contextManager);
    }

    // ─────────────────────────────────────────────
    //  API publique — binding
    // ─────────────────────────────────────────────

    /**
     * Enregistre un binding action ↔ touche dans un ou plusieurs contextes.
     *
     * @param action    action logique à déclencher
     * @param type      HOLD ou TRIGGER (AXIS pas encore supporté, étape 6)
     * @param keyCode   code touche jME (ex : {@link KeyInput#KEY_W})
     * @param contexts  contextes où l'action est valide (au moins un)
     */
    public void bind(GameAction action, ActionType type, int keyCode, InputContext... contexts) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(type);
        if (contexts == null || contexts.length == 0) {
            throw new IllegalArgumentException("Au moins un contexte est requis pour " + action);
        }
        if (type == ActionType.AXIS) {
            throw new UnsupportedOperationException("AXIS sera supporté à l'étape 6");
        }

        Set<InputContext> ctxSet = EnumSet.copyOf(Arrays.asList(contexts));
        String mappingName = action.name() + "_" + keyCode;
        Binding binding = new Binding(action, type, ctxSet, mappingName, 1f);
        bindings.add(binding);

// Enregistrer le mapping côté jME
        inputManager.addMapping(mappingName, new KeyTrigger(keyCode));
        inputManager.addListener(jmeListener, mappingName);

        // Initialiser l'état HOLD
        if (type == ActionType.HOLD) {
            heldStates.put(action, false);
        }
    }

    /**
     * Enregistre un binding sur un axe souris bidirectionnel.
     * Crée DEUX mappings jME en interne (négatif et positif).
     *
     * @param action     action à déclencher (doit être de type AXIS)
     * @param mouseAxis  MouseInput.AXIS_X ou AXIS_Y
     * @param contexts   contextes dans lesquels l'axe est actif
     */
    public void bindAxis(GameAction action, int mouseAxis, InputContext... contexts) {
        Set<InputContext> ctxSet = EnumSet.copyOf(Arrays.asList(contexts));

        // Mapping négatif (ex: souris vers la gauche)
        String negName = action.name() + "_AXIS" + mouseAxis + "_NEG";
        inputManager.addMapping(negName, new MouseAxisTrigger(mouseAxis, true));
        inputManager.addListener(jmeAnalogListener, negName);
        bindings.add(new Binding(action, ActionType.AXIS, ctxSet, negName, -1f));

        // Mapping positif (ex: souris vers la droite)
        String posName = action.name() + "_AXIS" + mouseAxis + "_POS";
        inputManager.addMapping(posName, new MouseAxisTrigger(mouseAxis, false));
        inputManager.addListener(jmeAnalogListener, posName);
        bindings.add(new Binding(action, ActionType.AXIS, ctxSet, posName, 1f));
    }

    /**
     * Enregistre un binding action ↔ bouton souris.
     *
     * @param action     action logique (HOLD ou TRIGGER, pas AXIS)
     * @param type       HOLD ou TRIGGER
     * @param mouseButton MouseInput.BUTTON_LEFT, BUTTON_RIGHT ou BUTTON_MIDDLE
     * @param contexts   contextes où l'action est valide
     */
    public void bindMouseButton(GameAction action, ActionType type,
                                int mouseButton, InputContext... contexts) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(type);
        if (contexts == null || contexts.length == 0) {
            throw new IllegalArgumentException("Au moins un contexte est requis pour " + action);
        }
        if (type == ActionType.AXIS) {
            throw new UnsupportedOperationException("AXIS non supporté pour les boutons souris");
        }

        // BUTTON et HOLD/TRIGGER sont tous valides ici
        Set<InputContext> ctxSet = EnumSet.copyOf(Arrays.asList(contexts));
        String mappingName = action.name() + "_MOUSE" + mouseButton;
        Binding binding = new Binding(action, type, ctxSet, mappingName, 1f);
        bindings.add(binding);

        inputManager.addMapping(mappingName, new MouseButtonTrigger(mouseButton));
        inputManager.addListener(jmeListener, mappingName);

        if (type == ActionType.HOLD) {
            heldStates.put(action, false);
        }
    }

    /**
     * Lit et remet à zéro la valeur accumulée d'un axe.
     * À appeler une fois par frame depuis le code de jeu.
     *
     * @param action action AXIS
     * @return valeur accumulée depuis le dernier appel (peut être négative)
     */
    public float consumeAxis(GameAction action) {
        Float value = axisValues.remove(action);
        return value != null ? value : 0f;
    }

    /**
     * Enregistre un callback à appeler quand une action TRIGGER est déclenchée.
     *
     * @param action   action de type TRIGGER
     * @param callback code à exécuter
     */
    public void onTrigger(GameAction action, Runnable callback) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(callback);
        triggerCallbacks.put(action, callback);
    }

    // ─────────────────────────────────────────────
    //  API publique — interrogation d'état
    // ─────────────────────────────────────────────

    /**
     * Indique si une action HOLD est actuellement maintenue.
     *
     * @param action action HOLD
     * @return true si la touche associée est pressée ET le contexte est actif
     */
    public boolean isHeld(GameAction action) {
        Boolean state = heldStates.get(action);
        return state != null && state;
    }

    /**
     * Retourne la position actuelle du curseur souris (en pixels, origine bas-gauche).
     * Renvoie une copie pour éviter les surprises (jME réutilise l'instance interne).
     *
     * @return position du curseur, jamais null
     */
    public Vector2f getCursorPosition() {
        return inputManager.getCursorPosition().clone();
    }

    public void onPress(GameAction action, Runnable callback) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(callback);
        pressCallbacks.put(action, callback);
    }

    public void onRelease(GameAction action, Runnable callback) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(callback);
        releaseCallbacks.put(action, callback);
    }

    // ─────────────────────────────────────────────
    //  Nettoyage
    // ─────────────────────────────────────────────

    /**
     * Supprime tous les bindings côté jME. À appeler lors du retour au menu
     * ou du changement de scène pour éviter les fuites de listeners.
     */
    public void cleanup() {
        for (Binding b : bindings) {
            if (inputManager.hasMapping(b.mappingName)) {
                inputManager.deleteMapping(b.mappingName);
            }
        }
        inputManager.removeListener(jmeListener);
        inputManager.removeListener(jmeAnalogListener);
        bindings.clear();
        heldStates.clear();
        triggerCallbacks.clear();
        pressCallbacks.clear();
        releaseCallbacks.clear();
    }

    // ─────────────────────────────────────────────
    //  Logique interne — dispatch jME
    // ─────────────────────────────────────────────

    /**
     * Callback unique reçu de jME. Dispatche vers le bon binding.
     */
    private void onJmeAction(String name, boolean isPressed, float tpf) {
        for (Binding b : bindings) {
            if (b.mappingName.equals(name)) {
                handleBinding(b, isPressed);
            }
        }
    }

    /**
     * Méthode de dispatch pour les events analogiques jME (souris).
     * Accumule la valeur dans axisValues, signée selon la convention du binding.
     */
    private void onJmeAnalog(String name, float value, float tpf) {
        for (Binding b : bindings) {
            if (!b.mappingName.equals(name)) continue;
            if (b.type != ActionType.AXIS) continue;
            if (!isAnyContextActive(b.contexts)) continue;

            // Accumuler la valeur signée
            float current = axisValues.getOrDefault(b.action, 0f);
            axisValues.put(b.action, current + value * b.axisSign);
        }
    }


    /**
     * Traite un événement (press ou release) pour un binding donné.
     *
     * <p><b>Règle "release inconditionnel"</b> : les releases sont toujours
     * traités pour éviter les touches fantômes. Les press sont filtrés par
     * contexte.
     */
    private void handleBinding(Binding b, boolean isPressed) {
        if (b.type == ActionType.HOLD) {
            if (isPressed) {
                // Press : filtré par contexte
                if (isAnyContextActive(b.contexts)) {
                    heldStates.put(b.action, true);
                }
            } else {
                // Release : inconditionnel
                heldStates.put(b.action, false);
            }
        } else if (b.type == ActionType.TRIGGER) {
            // TRIGGER : seulement sur le press, filtré par contexte
            if (isPressed && isAnyContextActive(b.contexts)) {
                Runnable callback = triggerCallbacks.get(b.action);
                if (callback != null) {
                    callback.run();
                }
            }
        } else if (b.type == ActionType.BUTTON) {
            // BUTTON : press filtré par contexte, release inconditionnel
            if (isPressed) {
                if (isAnyContextActive(b.contexts)) {
                    Runnable cb = pressCallbacks.get(b.action);
                    if (cb != null) cb.run();
                }
            } else {
                Runnable cb = releaseCallbacks.get(b.action);
                if (cb != null) cb.run();
            }
        }
    }

    /**
     * @return true si au moins un des contextes du binding est actif
     */
    private boolean isAnyContextActive(Set<InputContext> contexts) {
        for (InputContext ctx : contexts) {
            if (contextManager.isActive(ctx)) {
                return true;
            }
        }
        return false;
    }
}
