package dev.netcraft.network;

import java.util.List;
import java.util.Optional;

import dev.netcraft.network.protocol.MacAddress;

/**
 * Un appareil tel qu'il est écrit dans la sauvegarde : sa description et les adresses MAC de ses ports.
 * Java pur : la relecture des anciens formats se teste sans lancer le jeu.
 *
 * @param macs vide pour un monde sauvegardé avant le lot 1b ; le graphe en génère alors de nouvelles
 */
record SavedNode(NetworkNode node, List<MacAddress> macs) {

    SavedNode {
        macs = List.copyOf(macs);
    }

    /**
     * Relit un appareil. Avant le lot 1b, seul le nombre de ports était écrit : tous les appareils
     * étaient des postes et tous leurs ports des prises Ethernet. Depuis, le rôle et le type de
     * chaque port sont écrits et font foi.
     */
    static SavedNode decode(DeviceId id, String type, Optional<DeviceRole> role, int portCount,
            Optional<List<PortKind>> portKinds, List<MacAddress> macs) {
        return new SavedNode(new NetworkNode(id, type, role.orElse(DeviceRole.HOST),
                portKinds.orElseGet(() -> PortKind.ethernet(portCount))), macs);
    }
}
