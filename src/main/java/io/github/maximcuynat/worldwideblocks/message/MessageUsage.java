package io.github.maximcuynat.worldwideblocks.message;

/** Why a message exists; with its level, it decides the channel (see {@link MessageRouting}). */
public enum MessageUsage {
    /** Immediate answer to a player's gesture (connection, refusal, unplugging). */
    GESTURE,
    /** Network event, without a player gesture (loop, saturated network). */
    EVENT
}
