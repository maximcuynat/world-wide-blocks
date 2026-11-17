package dev.netcraft.network.sim;

import dev.netcraft.network.PortRef;
import dev.netcraft.network.protocol.EthernetFrame;

/**
 * Point d'écoute : chaque trame qui entre dans un appareil ou en sort y passe. Il ne fait rien pour
 * l'instant ; il servira à l'analyseur de trames en jeu, et les tests s'en servent pour observer les
 * échanges.
 *
 * <p>Il ne doit pas modifier le réseau : il observe seulement.
 */
@FunctionalInterface
public interface FrameTap {
    FrameTap NONE = (now, port, direction, frame) -> {
    };

    /**
     * @param now       temps de jeu, en ticks
     * @param port      port de l'appareil par lequel passe la trame
     */
    void onFrame(long now, PortRef port, FrameDirection direction, EthernetFrame frame);
}
