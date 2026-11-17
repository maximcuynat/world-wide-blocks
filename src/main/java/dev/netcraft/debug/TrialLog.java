package dev.netcraft.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.netcraft.NetCraft;
import dev.netcraft.network.DeviceId;
import dev.netcraft.network.NetworkGraph;
import dev.netcraft.network.NetworkNode;
import dev.netcraft.network.PortKind;
import dev.netcraft.network.PortRef;
import dev.netcraft.network.protocol.MacAddress;
import dev.netcraft.network.sim.DeviceLogic;
import dev.netcraft.network.sim.NetworkSimulation;
import dev.netcraft.network.switching.MacTable;

/**
 * TEMPORAIRE : traces de diagnostic des essais en jeu du lot 1b-1, demandées par Max le 2026-11-17
 * pour prouver chaque comportement par le log. Toutes les lignes commencent par {@value #PREFIX}.
 * À retirer, avec ses appels, une fois les essais du lot 1b-1 validés (voir {@code tasks/todo.md}).
 *
 * <p>Niveau DEBUG : en développement, elles vont dans {@code run/logs/debug.log}, pas dans
 * {@code latest.log}. Quand ce niveau est coupé, les traces coûteuses ne calculent rien
 * ({@link #enabled()}).
 *
 * <p>Pas d'état statique : le relevé des MAC apprises vit dans une instance portée par les données
 * du serveur.
 */
public final class TrialLog {
    public static final String PREFIX = "[ESSAI 1b-1] ";

    /** Dernier relevé des MAC apprises par chaque appareil qui commute : « port N : mac ». */
    private final Map<DeviceId, Set<String>> learned = new HashMap<>();

    /** Vrai si les traces sont écrites (niveau DEBUG actif) : à tester avant une trace coûteuse. */
    public static boolean enabled() {
        return NetCraft.LOGGER.isDebugEnabled();
    }

    public static void debug(String message, Object... args) {
        NetCraft.LOGGER.debug(PREFIX + message, args);
    }

    /** État de chaque port : « 1 libre », « 2 connecté », « 5 ligne inactive », avec sa MAC. */
    public static String ports(NetworkGraph graph, NetworkNode node) {
        List<String> parts = new ArrayList<>();
        List<MacAddress> macs = graph.macsOf(node.id());
        for (int i = 0; i < node.portCount(); i++) {
            String state = node.portKind(i) == PortKind.LINE ? "ligne inactive"
                    : graph.linkAt(node.port(i)).isPresent() ? "connecté" : "libre";
            String mac = i < macs.size() ? " (MAC " + macs.get(i) + ")" : "";
            parts.add((i + 1) + " " + state + mac);
        }
        return String.join(", ", parts);
    }

    /** Appareil lisible : type et position. */
    public static String device(NetworkGraph graph, DeviceId id) {
        String type = graph.node(id).map(NetworkNode::deviceType).orElse("?");
        return type + " [" + id.x() + ", " + id.y() + ", " + id.z() + "] " + id.dimension();
    }

    public static String port(NetworkGraph graph, PortRef port) {
        String mac = graph.macOf(port).map(m -> " (MAC " + m + ")").orElse("");
        return device(graph, port.device()) + " port " + port.displayNumber() + mac;
    }

    /** Appelé chaque seconde de jeu : trace les MAC apprises et oubliées depuis le relevé précédent. */
    public void compareLearned(NetworkGraph graph, NetworkSimulation simulation, long gameTime) {
        if (!enabled()) {
            return;
        }
        Set<DeviceId> seen = new LinkedHashSet<>();
        for (NetworkNode node : graph.nodes()) {
            DeviceLogic logic = simulation.logic(node.id()).orElse(null);
            if (logic == null || !logic.bridges()) {
                continue;
            }
            seen.add(node.id());
            Set<String> now = new LinkedHashSet<>();
            for (MacTable.Entry entry : logic.learnedMacs()) {
                now.add("port " + (entry.port() + 1) + " : " + entry.mac());
            }
            Set<String> before = this.learned.getOrDefault(node.id(), Set.of());
            for (String entry : now) {
                if (!before.contains(entry)) {
                    debug("t={} {} a appris {}", gameTime, device(graph, node.id()), entry);
                }
            }
            for (String entry : before) {
                if (!now.contains(entry)) {
                    debug("t={} {} a oublié {} (câble retiré ou expiration)", gameTime, device(graph, node.id()), entry);
                }
            }
            this.learned.put(node.id(), now);
        }
        this.learned.keySet().retainAll(seen);
    }
}
