package dev.netcraft.network;

import java.util.List;

/**
 * Appareil connu du moteur réseau. Il est enregistré à la pose du bloc et existe même quand
 * son chunk n'est pas chargé : le moteur sait ainsi quels ports il a sans charger le monde.
 *
 * <p>Ce record décrit ce que le bloc déclare (type et ports). Il est reconstruit depuis le bloc puis
 * comparé à celui du moteur : il ne porte donc pas les adresses MAC, que le graphe attribue et garde à part.
 *
 * @param deviceType identifiant du bloc, par exemple {@code netcraft:pc_tower}
 * @param ports      type de chaque port, dans l'ordre des numéros de port
 */
public record NetworkNode(DeviceId id, String deviceType, List<PortKind> ports) {

    public NetworkNode {
        ports = List.copyOf(ports);
    }

    /** Appareil dont tous les ports sont des prises Ethernet. */
    public NetworkNode(DeviceId id, String deviceType, int ethernetPorts) {
        this(id, deviceType, PortKind.ethernet(ethernetPorts));
    }

    public int portCount() {
        return this.ports.size();
    }

    public boolean hasPort(int index) {
        return index >= 0 && index < this.ports.size();
    }

    public PortKind portKind(int index) {
        return this.ports.get(index);
    }

    public PortRef port(int index) {
        return new PortRef(this.id, index);
    }
}
