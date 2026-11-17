package dev.netcraft.network;

/**
 * Appareil connu du moteur réseau. Il est enregistré à la pose du bloc et existe même quand
 * son chunk n'est pas chargé : le moteur sait ainsi combien de ports il a sans charger le monde.
 *
 * <p>Les lots suivants y rattacheront l'état propre à l'appareil (adresse matérielle des ports au lot 1b).
 *
 * @param deviceType identifiant du bloc, par exemple {@code netcraft:pc_tower}
 * @param portCount  nombre fixe de ports réseau
 */
public record NetworkNode(DeviceId id, String deviceType, int portCount) {

    public NetworkNode {
        if (portCount < 0) {
            throw new IllegalArgumentException("Nombre de ports négatif : " + portCount);
        }
    }

    public boolean hasPort(int index) {
        return index >= 0 && index < this.portCount;
    }

    public PortRef port(int index) {
        return new PortRef(this.id, index);
    }
}
