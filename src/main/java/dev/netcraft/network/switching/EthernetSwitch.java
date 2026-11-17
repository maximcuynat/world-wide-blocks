package dev.netcraft.network.switching;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import dev.netcraft.network.protocol.EthernetFrame;

/**
 * Commutation Ethernet transparente (pont IEEE 802.1D sans STP) : elle apprend la MAC source de
 * chaque trame reçue, relaie une trame vers le port où se trouve sa destination, et la diffuse sur
 * tous les autres ports quand la destination est inconnue ou que la trame est adressée à tous.
 *
 * <p>Réutilisée telle quelle par la box (lot 1b) et par les switchs de bureau (lot 1d). Les ports
 * sont les numéros de port de l'appareil qui la porte. Un appareil qui a son propre service sur le
 * réseau (la box et son serveur DHCP au lot 1b-2, un switch administrable au lot 8b) l'y branche par
 * le point local {@link #LOCAL_PORT}, comme un hôte interne ; un switch de bureau n'en a pas.
 *
 * <p>Elle ne fait que décider : l'envoi des trames et la protection contre les boucles sont l'affaire
 * de la simulation. Java pur, sans classe Minecraft.
 */
public final class EthernetSwitch {
    /** Numéro du point local : jamais un numéro de port réel, qui commence à 0. */
    public static final int LOCAL_PORT = -1;

    private final List<Integer> ports;
    private final boolean localPort;
    private final MacTable table;

    /**
     * @param ports     numéros des ports commutés, dans l'ordre de diffusion
     * @param localPort vrai si l'appareil a un service branché sur le point local
     */
    public EthernetSwitch(List<Integer> ports, boolean localPort, long agingTicks) {
        for (int port : ports) {
            if (port < 0) {
                throw new IllegalArgumentException("Numéro de port négatif : " + port);
            }
        }
        this.ports = List.copyOf(ports);
        this.localPort = localPort;
        this.table = new MacTable(agingTicks);
    }

    /**
     * Une trame arrive par {@code ingress} (un port commuté ou {@link #LOCAL_PORT}) : le switch apprend
     * sa source, puis renvoie les ports par lesquels la relayer. Une liste vide signifie que la trame
     * est filtrée (sa destination est derrière le port même d'où elle vient).
     */
    public List<Integer> receive(int ingress, EthernetFrame frame, long now) {
        if (ingress != LOCAL_PORT && !this.ports.contains(ingress)) {
            throw new IllegalArgumentException("Port " + ingress + " non commuté");
        }
        this.table.learn(frame.source(), ingress, now);
        if (!frame.destination().isMulticast()) {
            OptionalInt known = this.table.lookup(frame.destination(), now);
            if (known.isPresent()) {
                return known.getAsInt() == ingress ? List.of() : List.of(known.getAsInt());
            }
        }
        return this.floodFrom(ingress);
    }

    /** Le câble de ce port vient d'être retiré : tout ce qui était appris derrière est oublié. */
    public void linkDown(int port) {
        this.table.flushPort(port);
    }

    /** À appeler régulièrement : retire les entrées expirées. */
    public void purgeExpired(long now) {
        this.table.purgeExpired(now);
    }

    public boolean switches(int port) {
        return this.ports.contains(port);
    }

    /** Table d'apprentissage, en lecture (indicateur, futur analyseur). */
    public List<MacTable.Entry> entries() {
        return this.table.entries();
    }

    private List<Integer> floodFrom(int ingress) {
        List<Integer> egress = new ArrayList<>(this.ports.size() + 1);
        for (int port : this.ports) {
            if (port != ingress) {
                egress.add(port);
            }
        }
        if (this.localPort && ingress != LOCAL_PORT) {
            egress.add(LOCAL_PORT);
        }
        return egress;
    }
}
