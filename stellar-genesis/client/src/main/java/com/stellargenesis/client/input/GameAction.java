package com.stellargenesis.client.input;

/**
 * Toutes les actions possibles du joueur dans le jeu.
 *
 * Une action est un VERBE LOGIQUE (ex: "sauter"), indépendant de la touche
 * physique qui le déclenche. C'est ce qui permet le rebind clavier :
 * l'utilisateur change la touche, mais l'action reste la même.
 *
 * Chaque action est associée à :
 *  - Un {@link ActionType} (HOLD, TRIGGER, AXIS) qui définit son comportement
 *  - Un ou plusieurs {@link InputContext} où elle est active
 *
 * Cette séparation action ↔ touche est le cœur du pattern "Command" appliqué
 * aux inputs : on découple ce que le joueur VEUT faire de COMMENT il le déclenche.
 */
public enum GameAction {

    // ===============================================
    //  Mouvement (HOLD) — actifs en GAMEPLAY et FREECAM
    // ===============================================
    MOVE_FORWARD,
    MOVE_BACKWARD,
    MOVE_LEFT,
    MOVE_RIGHT,
    SPRINT,
    MOVE_UP,        // monter en FREECAM (Espace tenu)
    MOVE_DOWN,      // descendre en FREECAM (Shift tenu)

    // ===============================================
    //  Actions gameplay (TRIGGER)
    // ===============================================
    JUMP,           // saut, GAMEPLAY uniquement (impulsion unique)
    MINE,           // clic gauche, GAMEPLAY
    PLACE_BLOCK,    // clic droit, GAMEPLAY

    // ===============================================
    //  UI (TRIGGER)
    // ===============================================
    TOGGLE_INVENTORY,   // TAB en GAMEPLAY
    INVENTORY_CLICK,    // clic gauche en INVENTORY
    CLOSE_INVENTORY,    // Échap en INVENTORY
    PAUSE,              // Échap en GAMEPLAY
    RESUME,             // Échap en PAUSE

    // ===============================================
    //  Caméra (AXIS) — actifs en GAMEPLAY et FREECAM
    // ===============================================
    LOOK_X,         // mouvement souris horizontal
    LOOK_Y,         // mouvement souris vertical
    CURSOR_X,       // axe X souris en contexte INVENTORY (et autres UI)
    CURSOR_Y,

    // ===============================================CH
    //  Debug (TRIGGER) — contexte DEBUG toujours actif
    // ===============================================
    TOGGLE_DEBUG_HUD,       // F1
    TOGGLE_FREECAM,         // F2
    TOGGLE_FRUSTUM_DEBUG,   // F3
}