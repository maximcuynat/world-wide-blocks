package io.github.maximcuynat.worldwideblocks.message;

/** Notified of every sent message; used by tests, which capture messages this way without a real player. */
@FunctionalInterface
public interface MessageListener {
    void onSent(SentMessage message);
}
