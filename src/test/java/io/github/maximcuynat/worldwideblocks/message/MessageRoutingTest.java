package io.github.maximcuynat.worldwideblocks.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.message.MessageRouting.Route;

class MessageRoutingTest {

    @Test
    void aGestureGoesToTheActionBarWhateverItsLevel() {
        for (MessageLevel level : MessageLevel.values()) {
            assertEquals(new Route(Optional.of(MessageChannel.ACTION_BAR), LogLevel.DEBUG),
                    MessageRouting.route(MessageUsage.GESTURE, level), level.name());
        }
    }

    @Test
    void aWarningEventGoesToTheChatAndToWarn() {
        assertEquals(new Route(Optional.of(MessageChannel.CHAT), LogLevel.WARN),
                MessageRouting.route(MessageUsage.EVENT, MessageLevel.WARNING));
    }

    @Test
    void anErrorEventGoesToTheChatAndToError() {
        assertEquals(new Route(Optional.of(MessageChannel.CHAT), LogLevel.ERROR),
                MessageRouting.route(MessageUsage.EVENT, MessageLevel.ERROR));
    }

    @Test
    void anInfoOrSuccessEventGoesOnlyToTheLog() {
        assertEquals(new Route(Optional.empty(), LogLevel.INFO), MessageRouting.route(MessageUsage.EVENT, MessageLevel.INFO));
        assertEquals(new Route(Optional.empty(), LogLevel.INFO), MessageRouting.route(MessageUsage.EVENT, MessageLevel.SUCCESS));
    }
}
