package com.stellargenesis.client.input;

/**
 * Type de comportement d'une action.
 *
 * Détermine comment l'input physique est interprété et exposé au code client :
 *
 * <ul>
 *   <li>{@link #TRIGGER} : déclenchement unique au moment où la touche est
 *       pressée. Une seule notification, peu importe combien de temps la
 *       touche reste enfoncée. Ex : JUMP, MINE, TOGGLE_INVENTORY.</li>
 *
 *   <li>{@link #HOLD} : état booléen "appuyée / pas appuyée". Le code client
 *       interroge à chaque frame via {@code isHeld(action)}. Ex : MOVE_FORWARD,
 *       SPRINT.</li>
 *
 *   <li>{@link #AXIS} : valeur continue (delta) émise à chaque mouvement.
 *       Typiquement pour la souris (delta X/Y) ou un stick analogique futur.
 *       Le code client reçoit la valeur via un callback. Ex : LOOK_X.</li>
 * </ul>
 *
 * Cette typologie correspond aux trois modes d'interaction fondamentaux
 * en game design d'inputs. Aucune action ne sort de ces trois cas.
 */
public enum ActionType {
    TRIGGER,
    HOLD,
    AXIS,
    BUTTON
}