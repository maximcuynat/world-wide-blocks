package io.github.maximcuynat.worldwideblocks.message;

/**
 * Where a message is shown. The status panel is not one of them: it is not sent as a
 * message, the client builds it from the state of the targeted device.
 */
public enum MessageChannel {
    /** Above the hotbar, to the player who has just acted. */
    ACTION_BAR,
    /** In the chat, for rare and important events. */
    CHAT,
    /** In the server log, in English. */
    LOG
}
