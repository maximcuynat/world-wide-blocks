package dev.netcraft.network;

import java.util.List;
import java.util.Objects;

/**
 * Appareil connu du moteur réseau. Il est enregistré à la pose du bloc et existe même quand
 * son chunk n'est pas chargé : le moteur sait ainsi quels ports il a et comment il se comporte
 * sans charger le monde.
 *
 * <p>Ce record décrit ce que le bloc déclare (type, rôle et ports). Il est reconstruit depuis le bloc
 * puis comparé à celui du moteur : il ne porte donc pas les adresses MAC, que le graphe attribue et
 * garde à part.
 *
 * @param deviceType identifiant du bloc, par exemple {@code netcraft:pc_tower}
 * @param role       comportement de l'appareil dans la simulation
 * @param ports      type de chaque port, dans l'ordre des numéros de port
 */
public record NetworkNode(DeviceId id, String deviceType, DeviceRole role, List<PortKind> ports) {

    public NetworkNode {
        Objects.requireNonNull(role, "role");
        ports = List.copyOf(ports);
    }

    /** Poste dont tous les ports sont des prises Ethernet. */
    public NetworkNode(DeviceId id, String deviceType, int ethernetPorts) {
        this(id, deviceType, DeviceRole.HOST, PortKind.ethernet(ethernetPorts));
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
