package dev.netcraft.network;

import java.util.List;

/**
 * Bloc qui est un appareil réseau. Ses ports (nombre et type) sont fixes pour un type d'appareil :
 * ils ne dépendent ni de l'état du bloc ni de sa configuration.
 */
public interface NetworkDevice {

    /** Type de chaque port, dans l'ordre des numéros de port. */
    List<PortKind> ports();

    default int portCount() {
        return this.ports().size();
    }
}
