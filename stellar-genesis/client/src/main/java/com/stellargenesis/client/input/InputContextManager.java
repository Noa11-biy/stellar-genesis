package com.stellargenesis.client.input;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Gère la pile des contextes d'input actifs.
 *
 * <h2>Principe</h2>
 * Le jeu peut se trouver dans différents <b>états</b> (gameplay, inventaire,
 * pause, freecam...) qui définissent quelles actions sont valides. Ces états
 * sont modélisés par une pile :
 * <ul>
 *   <li>{@link #pushContext(InputContext)} : entrer dans un nouvel état</li>
 *   <li>{@link #popContext()} : sortir de l'état courant, retour au précédent</li>
 *   <li>{@link #currentContext()} : état actuellement au sommet</li>
 * </ul>
 *
 * <h2>Le cas spécial DEBUG</h2>
 * {@link InputContext#DEBUG} n'est <b>jamais</b> dans la pile. Il est
 * considéré comme <b>toujours actif en parallèle</b> du contexte principal.
 * Cela permet aux touches de debug (F1, F2, F3) de fonctionner peu importe
 * l'état du jeu.
 *
 * <h2>Exemple d'utilisation</h2>
 * <pre>
 *   manager.pushContext(GAMEPLAY);   // entre en jeu
 *   manager.pushContext(INVENTORY);  // ouvre inventaire
 *   manager.isActive(GAMEPLAY);      // false (INVENTORY est au-dessus)
 *   manager.isActive(INVENTORY);     // true
 *   manager.isActive(DEBUG);         // true (toujours)
 *   manager.popContext();            // ferme inventaire
 *   manager.isActive(GAMEPLAY);      // true (retour automatique)
 * </pre>
 */
public class InputContextManager {

    /** Pile des contextes empilés. Le sommet ({@code peek()}) est le contexte actif. */
    private final Deque<InputContext> stack = new ArrayDeque<>();

    /**
     * Empile un nouveau contexte au sommet. Le contexte précédent est
     * conservé en dessous et sera restauré au prochain {@link #popContext()}.
     *
     * @param context contexte à empiler (non null, et ne doit pas être {@link InputContext#DEBUG})
     * @throws NullPointerException si {@code context} est null
     * @throws IllegalArgumentException si {@code context} est {@link InputContext#DEBUG}
     */
    public void pushContext(InputContext context) {
        Objects.requireNonNull(context, "context ne peut pas être null");
        if (context == InputContext.DEBUG) {
            throw new IllegalArgumentException(
                    "DEBUG ne s'empile pas : il est toujours actif en parallèle.");
        }
        stack.push(context);
    }

    /**
     * Dépile le contexte courant. Le contexte précédent (s'il existe)
     * redevient automatiquement actif.
     *
     * @return le contexte qui vient d'être retiré
     * @throws IllegalStateException si la pile est vide (fail fast pour détecter les bugs)
     */
    public InputContext popContext() {
        if (stack.isEmpty()) {
            throw new IllegalStateException(
                    "popContext() appelé sur une pile vide — bug logique côté appelant.");
        }
        return stack.pop();
    }

    /**
     * @return le contexte au sommet de la pile, ou {@code null} si la pile est vide
     */
    public InputContext currentContext() {
        return stack.peek();
    }

    /**
     * Indique si un contexte est actuellement actif.
     *
     * <ul>
     *   <li>{@link InputContext#DEBUG} est <b>toujours</b> actif.</li>
     *   <li>Tout autre contexte est actif s'il est au <b>sommet</b> de la pile.</li>
     * </ul>
     *
     * @param context contexte à tester (non null)
     * @return true si le contexte est actif
     */
    public boolean isActive(InputContext context) {
        Objects.requireNonNull(context, "context ne peut pas être null");
        if (context == InputContext.DEBUG) {
            return true;
        }
        return stack.peek() == context;
    }

    /**
     * Vide complètement la pile. Utile lors d'un retour au menu principal
     * ou d'un changement radical d'état.
     */
    public void clear() {
        stack.clear();
    }

    /**
     * @return nombre de contextes actuellement empilés (hors DEBUG)
     */
    public int depth() {
        return stack.size();
    }
}
