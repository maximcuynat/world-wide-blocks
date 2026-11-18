package io.github.maximcuynat.worldwideblocks.message;

/** Prévenu de chaque message envoyé ; sert aux tests, qui captent ainsi les messages sans joueur réel. */
@FunctionalInterface
public interface MessageListener {
    void onSent(SentMessage message);
}
