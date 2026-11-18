package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.maximcuynat.worldwideblocks.message.MessageChannel;
import io.github.maximcuynat.worldwideblocks.message.MessageListener;
import io.github.maximcuynat.worldwideblocks.message.SentMessage;

/**
 * Records each message sent by the message module during a test: channel, level, key, values,
 * recipient, game time. The tests of one batch share the server: each test filters what it reads
 * by its player (or its devices).
 */
public final class MessageRecorder implements MessageListener {
    private final List<SentMessage> messages = new ArrayList<>();

    @Override
    public void onSent(SentMessage message) {
        this.messages.add(message);
    }

    /** Messages received by this player on this channel, with this key. */
    public List<SentMessage> to(UUID player, MessageChannel channel, String key) {
        return this.messages.stream()
                .filter(message -> message.sentTo(player) && message.channel() == channel && message.key().equals(key))
                .toList();
    }

    /** Log lines of this key (network events), all devices together: filter them by device. */
    public List<SentMessage> logs(String key) {
        return this.messages.stream()
                .filter(message -> message.channel() == MessageChannel.LOG && message.key().equals(key))
                .toList();
    }

    /** All messages received by this player, to explain a failure. */
    public List<SentMessage> to(UUID player) {
        return this.messages.stream().filter(message -> message.sentTo(player)).toList();
    }
}
