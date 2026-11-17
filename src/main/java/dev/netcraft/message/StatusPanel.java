package dev.netcraft.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

import dev.netcraft.network.PortKind;
import dev.netcraft.network.protocol.MacAddress;

/**
 * Contenu du panneau d'état de l'appareil visé, sans aucun dessin : des lignes faites d'un voyant,
 * d'une clé de langue et de valeurs. Le client les dessine comme il veut ; une future interface HUD
 * remplace le dessin sans toucher à ce contenu.
 *
 * <ul>
 * <li>Compact (par défaut) : un résumé, puis « Accroupi : détails ». Pour un appareil à plusieurs
 * ports réseau local, le nombre de ports connectés (« 3/4 ports connectés ») ; pour un poste,
 * « Connecté » ou « Non connecté ».</li>
 * <li>Détaillé (joueur accroupi) : une ligne par port ; pour un appareil qui commute, la MAC apprise
 * derrière chaque port et le nombre des autres (« (+1) ») ; pour un poste, la MAC de chaque port.</li>
 * </ul>
 *
 * <p>Java pur, sans classe Minecraft.
 */
public final class StatusPanel {

    /** Voyant d'une ligne ; {@link #NONE} pour une ligne de détail, sans voyant. */
    public enum Light {
        CONNECTED,
        FREE,
        INACTIVE,
        NONE
    }

    public record Line(Light light, String key, List<MessageArg> args) {
        public Line {
            args = List.copyOf(args);
        }

        static Line of(Light light, String key, MessageArg... args) {
            return new Line(light, key, List.of(args));
        }
    }

    /**
     * Ce que le serveur a dit de l'appareil visé.
     *
     * @param switching vrai pour un appareil qui commute (box, switch)
     * @param portMacs  MAC de chaque port, dans l'ordre des ports
     * @param learned   MAC apprises derrière chaque port (numéro à partir de 0), pour un appareil qui commute
     */
    public record DeviceStatus(boolean switching, List<MacAddress> portMacs, Map<Integer, List<MacAddress>> learned) {
        public DeviceStatus {
            portMacs = List.copyOf(portMacs);
            learned = Map.copyOf(learned);
        }
    }

    private StatusPanel() {
    }

    /**
     * @param ports     type de chaque port de l'appareil
     * @param connected vrai si un câble est branché sur le port (numéro à partir de 0)
     * @param status    état envoyé par le serveur, {@code null} s'il n'est pas encore arrivé
     * @param detailed  vrai quand le joueur est accroupi
     */
    public static List<Line> lines(List<PortKind> ports, IntPredicate connected, DeviceStatus status, boolean detailed) {
        return detailed ? detailedLines(ports, connected, status) : compactLines(ports, connected);
    }

    private static List<Line> compactLines(List<PortKind> ports, IntPredicate connected) {
        int lan = 0;
        int lanConnected = 0;
        for (int i = 0; i < ports.size(); i++) {
            if (ports.get(i) == PortKind.ETHERNET) {
                lan++;
                if (connected.test(i)) {
                    lanConnected++;
                }
            }
        }
        List<Line> lines = new ArrayList<>();
        Light light = lanConnected > 0 ? Light.CONNECTED : Light.FREE;
        if (lan > 1) {
            lines.add(Line.of(light, "hud.netcraft.ports_connected", MessageArg.number(lanConnected), MessageArg.number(lan)));
        } else {
            lines.add(Line.of(light, lanConnected > 0 ? "hud.netcraft.connected" : "hud.netcraft.not_connected"));
        }
        lines.add(Line.of(Light.NONE, "hud.netcraft.sneak_for_details"));
        return lines;
    }

    private static List<Line> detailedLines(List<PortKind> ports, IntPredicate connected, DeviceStatus status) {
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < ports.size(); i++) {
            MessageArg number = MessageArg.number(i + 1);
            if (ports.get(i) == PortKind.LINE) {
                lines.add(Line.of(Light.INACTIVE, "hud.netcraft.port.line", number));
                continue;
            }
            boolean isConnected = connected.test(i);
            Light light = isConnected ? Light.CONNECTED : Light.FREE;
            List<MacAddress> learned = status != null && status.switching() ? status.learned().getOrDefault(i, List.of()) : List.of();
            if (isConnected && learned.size() == 1) {
                lines.add(Line.of(light, "hud.netcraft.port.connected_learned", number, mac(learned.get(0))));
            } else if (isConnected && learned.size() > 1) {
                lines.add(Line.of(light, "hud.netcraft.port.connected_learned_more", number, mac(learned.get(0)),
                        MessageArg.number(learned.size() - 1)));
            } else {
                lines.add(Line.of(light, isConnected ? "hud.netcraft.port.connected" : "hud.netcraft.port.free", number));
            }
            if (status != null && !status.switching() && i < status.portMacs().size()) {
                lines.add(Line.of(Light.NONE, "hud.netcraft.mac", mac(status.portMacs().get(i))));
            }
        }
        return lines;
    }

    private static MessageArg mac(MacAddress mac) {
        return MessageArg.text(mac.toString());
    }
}
