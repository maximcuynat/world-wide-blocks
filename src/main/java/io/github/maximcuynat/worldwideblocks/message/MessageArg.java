package io.github.maximcuynat.worldwideblocks.message;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;

/**
 * A value of a message. The server never sends written text: only values, which the
 * client formats in its language, and which the log writes in English ({@link #logText()}).
 */
public sealed interface MessageArg {

    /** Log line: always in English, with identifiers rather than translated names. */
    String logText();

    /** Value shown as is, in all languages: a MAC address, an identifier. */
    record Text(String value) implements MessageArg {
        @Override
        public String logText() {
            return this.value;
        }
    }

    /** Integer. Distances are rounded to the integer: no decimal separator, in any language. */
    record Number(long value) implements MessageArg {
        @Override
        public String logText() {
            return Long.toString(this.value);
        }
    }

    /** Text to be translated by the client, designated by its language key. */
    record Key(String key) implements MessageArg {
        @Override
        public String logText() {
            return this.key;
        }
    }

    /**
     * A device, written the same way everywhere: its translated name and its coordinates on the player side,
     * its block identifier, its coordinates and its dimension in the log.
     *
     * @param type block identifier, for example {@code worldwideblocks:internet_box}
     */
    record Device(String type, String dimension, int x, int y, int z) implements MessageArg {
        /** Language key of the device name, for example {@code block.worldwideblocks.internet_box}. */
        public String nameKey() {
            return "block." + this.type.replace(':', '.');
        }

        @Override
        public String logText() {
            return this.type + " [" + this.x + ", " + this.y + ", " + this.z + "] " + this.dimension;
        }
    }

    static MessageArg text(String value) {
        return new Text(value);
    }

    static MessageArg number(long value) {
        return new Number(value);
    }

    /** Distance in blocks, rounded to the nearest integer. */
    static MessageArg blocks(double distance) {
        return new Number(Math.round(distance));
    }

    static MessageArg key(String key) {
        return new Key(key);
    }

    static MessageArg device(DeviceId id, String type) {
        return new Device(type, id.dimension(), id.x(), id.y(), id.z());
    }
}
