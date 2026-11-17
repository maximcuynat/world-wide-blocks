package dev.netcraft.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.netcraft.message.MessageRouting.Route;

class MessageRoutingTest {

    @Test
    void unGesteVaDansLaBarreDActionQuelQueSoitSonNiveau() {
        for (MessageLevel level : MessageLevel.values()) {
            assertEquals(new Route(Optional.of(MessageChannel.ACTION_BAR), LogLevel.DEBUG),
                    MessageRouting.route(MessageUsage.GESTURE, level), level.name());
        }
    }

    @Test
    void unEvenementAvertissementVaDansLeChatEtEnWarn() {
        assertEquals(new Route(Optional.of(MessageChannel.CHAT), LogLevel.WARN),
                MessageRouting.route(MessageUsage.EVENT, MessageLevel.WARNING));
    }

    @Test
    void unEvenementErreurVaDansLeChatEtEnError() {
        assertEquals(new Route(Optional.of(MessageChannel.CHAT), LogLevel.ERROR),
                MessageRouting.route(MessageUsage.EVENT, MessageLevel.ERROR));
    }

    @Test
    void unEvenementInformationOuSuccesNeVaQuAuLog() {
        assertEquals(new Route(Optional.empty(), LogLevel.INFO), MessageRouting.route(MessageUsage.EVENT, MessageLevel.INFO));
        assertEquals(new Route(Optional.empty(), LogLevel.INFO), MessageRouting.route(MessageUsage.EVENT, MessageLevel.SUCCESS));
    }
}
