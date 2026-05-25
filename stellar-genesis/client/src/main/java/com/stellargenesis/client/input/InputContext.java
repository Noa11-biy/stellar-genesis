package com.stellargenesis.client.input;

/**
 * Contexte d'input actif à un instant donné.
 *
 * Un seul contexte "principal" est actif à la fois (géré par une pile dans
 * {@link InputContextManager}), à l'exception de {@link #DEBUG} qui est
 * toujours actif en parallèle.
 *
 * Pourquoi une pile ? Parce que les contextes peuvent s'empiler logiquement :
 *   GAMEPLAY → ouvre inventaire → INVENTORY
 *   GAMEPLAY → ouvre menu pause → PAUSE
 *   GAMEPLAY → active freecam → FREECAM
 * Et quand on ferme l'inventaire, on revient AUTOMATIQUEMENT au contexte
 * précédent (GAMEPLAY) sans avoir à le préciser.
 *
 * Règle : une action n'est déclenchée QUE si son contexte est au sommet
 * de la pile (ou si c'est DEBUG, qui est globalement actif).
 */
public enum InputContext {

    /** Écran titre, options, menus hors-jeu. */
    MENU,

    /** En jeu normal, caméra FPS attachée au joueur. */
    GAMEPLAY,

    /** Inventaire ouvert, curseur visible, gameplay figé. */
    INVENTORY,

    /** Menu pause superposé au jeu. */
    PAUSE,

    /** Caméra libre détachée du joueur (mode spectateur debug). */
    FREECAM,

    /**
     * Contexte SPÉCIAL : toujours actif en parallèle des autres.
     * Sert aux touches de debug (F1, F2, F3) qui doivent fonctionner
     * peu importe ce que le joueur est en train de faire.
     */
    DEBUG,
}
