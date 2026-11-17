package dev.netcraft.message;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Un message destiné aux joueurs : un niveau, une clé de langue et des valeurs. Aucun texte rédigé ni
 * aucune mise en forme : le client décide seul de l'apparence.
 */
public record Message(MessageLevel level, String key, List<MessageArg> args) {

    public Message {
        args = List.copyOf(args);
    }

    public static Message of(MessageLevel level, String key, MessageArg... args) {
        return new Message(level, key, List.of(args));
    }

    /** Écriture dans le log, en anglais : la clé puis les valeurs, par exemple {@code message.netcraft.cable.unplugged [netcraft:pc_tower [1, 64, 2] minecraft:overworld, 1]}. */
    public String logText() {
        if (this.args.isEmpty()) {
            return this.key;
        }
        return this.key + " " + this.args.stream().map(MessageArg::logText).collect(Collectors.joining(", ", "[", "]"));
    }
}
