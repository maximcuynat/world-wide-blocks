package dev.netcraft.network.sim;

import dev.netcraft.network.DeviceId;

/**
 * Ce que la simulation signale au jeu, qui en fait un message. Chaque événement est limité en
 * fréquence par la simulation (voir {@link SimulationLimits#reportIntervalTicks()}).
 */
public sealed interface SimulationEvent {

    /**
     * Une trame est revenue à un switch qu'elle avait déjà traversé : il existe deux chemins entre
     * eux, donc une boucle. La copie a été supprimée.
     *
     * @param port port par lequel la copie est revenue
     */
    record LoopDetected(DeviceId device, int port) implements SimulationEvent {
    }

    /** La file d'attente était pleine : des trames ont été perdues. */
    record QueueSaturated() implements SimulationEvent {
    }
}
