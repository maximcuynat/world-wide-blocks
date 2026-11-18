package io.github.maximcuynat.worldwideblocks.message;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A message for players: a level, a language key and values. No written text and
 * no formatting: the client alone decides how it looks.
 */
public record Message(MessageLevel level, String key, List<MessageArg> args) {

    public Message {
        args = List.copyOf(args);
    }

    public static Message of(MessageLevel level, String key, MessageArg... args) {
        return new Message(level, key, List.of(args));
    }

    /** Log line, in English: the key then the values, for example {@code message.worldwideblocks.cable.unplugged [worldwideblocks:pc_tower [1, 64, 2] minecraft:overworld, 1]}. */
    public String logText() {
        if (this.args.isEmpty()) {
            return this.key;
        }
        return this.key + " " + this.args.stream().map(MessageArg::logText).collect(Collectors.joining(", ", "[", "]"));
    }
}
