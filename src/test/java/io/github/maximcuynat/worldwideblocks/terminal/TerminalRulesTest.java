package io.github.maximcuynat.worldwideblocks.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class TerminalRulesTest {

    @Test
    void anyPrintableTextIsAccepted() {
        assertTrue(TerminalRules.refusal("ping -c 3 192.168.1.1").isEmpty());
        assertTrue(TerminalRules.refusal("é è ç à ù ê ë œ « » ~ @  ").isEmpty(), "accents, typed as they come");
        assertTrue(TerminalRules.refusal("😀").isEmpty(), "a character outside the basic plane");
        assertTrue(TerminalRules.refusal("").isEmpty());
        assertTrue(TerminalRules.refusal("x".repeat(TerminalRules.MAX_LINE)).isEmpty());
    }

    @Test
    void aLongLineOrAControlCharacterIsRefused() {
        assertEquals(Optional.of("257 characters, 256 at most"), TerminalRules.refusal("x".repeat(TerminalRules.MAX_LINE + 1)));
        assertEquals(Optional.of("control character U+000A"), TerminalRules.refusal("ip a\nreboot"));
        assertEquals(Optional.of("control character U+001B"), TerminalRules.refusal("\u001b[2J"));
        assertFalse(TerminalRules.refusal("a\u007fb").isEmpty(), "delete");
        assertFalse(TerminalRules.refusal("a b").isEmpty(), "line separator");
    }

    @Test
    void theBucketLetsABurstThenOnePacketPerTick() {
        TokenBucket bucket = new TokenBucket(3, 100);
        assertTrue(bucket.take(100));
        assertTrue(bucket.take(100));
        assertTrue(bucket.take(100));
        assertFalse(bucket.take(100));
        assertTrue(bucket.take(101));
        assertFalse(bucket.take(101));
        assertTrue(bucket.take(200));
        assertTrue(bucket.take(200));
        assertTrue(bucket.take(200));
        assertFalse(bucket.take(200), "never more than the burst");
    }
}
