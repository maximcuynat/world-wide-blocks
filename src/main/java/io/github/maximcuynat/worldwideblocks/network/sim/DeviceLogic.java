package io.github.maximcuynat.worldwideblocks.network.sim;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.network.protocol.EthernetFrame;
import io.github.maximcuynat.worldwideblocks.network.switching.MacTable;

/**
 * Comportement d'un appareil dans la simulation : ce qu'il fait quand un câble est branché ou retiré,
 * quand une trame arrive et quand une minuterie échoit. Une instance par appareil, recréée si
 * l'appareil est remplacé ; son état n'est pas sauvegardé, comme la mémoire d'un vrai appareil
 * qu'on redémarre.
 */
public interface DeviceLogic {

    /** L'appareil entre dans la simulation (posé, ou serveur démarré). */
    default void onStart(DeviceContext context) {
    }

    void onLinkUp(DeviceContext context, int port);

    void onLinkDown(DeviceContext context, int port);

    void onFrame(DeviceContext context, int port, EthernetFrame frame);

    default void onTimer(DeviceContext context, Object key) {
    }

    /**
     * Vrai pour un appareil qui relaie des trames entre ses ports (switch, box) : la simulation
     * vérifie qu'une même trame ne lui revient pas deux fois.
     */
    default boolean bridges() {
        return false;
    }

    /** MAC apprises derrière chaque port, pour un appareil qui commute (indicateur) ; vide sinon. */
    default List<MacTable.Entry> learnedMacs() {
        return List.of();
    }
}
