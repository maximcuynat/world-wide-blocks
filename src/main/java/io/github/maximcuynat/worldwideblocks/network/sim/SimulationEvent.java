package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;

/**
 * Ce que la simulation signale au jeu, qui en fait un message. La simulation constate ; le module des
 * messages décide qui prévenir et à quelle fréquence (une fois par minute pour un même problème).
 */
public sealed interface SimulationEvent {

    /** Port par lequel une copie d'une trame diffusée est revenue à un switch qu'elle avait déjà traversé. */
    record LoopPoint(DeviceId device, int port) {
    }

    /**
     * Une boucle dans un réseau : une trame est revenue à un switch qu'elle avait déjà traversé, donc il
     * existe deux chemins entre eux. Les copies ont été supprimées. Un seul événement par réseau et par
     * tick, même si plusieurs switchs l'ont vue.
     *
     * @param network clé du réseau ({@code NetworkGraph#networkKey}), stable tant que le réseau ne change pas
     * @param points  switchs et ports où les copies sont revenues, dans l'ordre de leur détection
     */
    record LoopDetected(DeviceId network, List<LoopPoint> points) implements SimulationEvent {
        public LoopDetected {
            points = List.copyOf(points);
        }
    }

    /** La file d'attente était pleine : des trames ont été perdues. Au plus un par tick. */
    record QueueSaturated() implements SimulationEvent {
    }
}
