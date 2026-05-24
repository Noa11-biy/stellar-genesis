package com.stellargenesis.core.world;

/**
 * Wrapper qui associe une priorité à une tâche de génération de chunk.
 *
 * Pourquoi ?
 *   PriorityBlockingQueue exige des éléments Comparable.
 *   Runnable ne l'est pas. Donc on encapsule.
 *
 * Convention :
 *   priorité plus PETITE = plus URGENT
 *   (comme dans une file d'attente : numéro 1 passe avant numéro 10)
 *
 * Barème typique :
 *   0-50    : chunks visibles, proches du joueur
 *   50-200  : chunks visibles, loin du joueur
 *   1000+   : chunks hors frustum (invisibles)
 */
public class PrioritizedChunkTask implements Runnable, Comparable<PrioritizedChunkTask> {

    private final int priority;
    private final Runnable task;

    public PrioritizedChunkTask(int priority, Runnable task) {
        this.priority = priority;
        this.task = task;
    }

    @Override
    public void run() {
        task.run();
    }

    @Override
    public int compareTo(PrioritizedChunkTask other) {
        // Comparaison d'entiers : retourne négatif si this < other
        // → this passe avant other dans la queue
        return Integer.compare(this.priority, other.priority);
    }

    public int getPriority() {
        return priority;
    }
}
