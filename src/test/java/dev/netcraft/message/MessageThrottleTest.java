package dev.netcraft.message;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessageThrottleTest {
    private static final long MINUTE = 1200;

    private final MessageThrottle throttle = new MessageThrottle(MINUTE);

    @Test
    void unProblemeNeSeRepetePasDansLaMinute() {
        assertTrue(throttle.allow("loop:a", 100));
        assertFalse(throttle.allow("loop:a", 100));
        assertFalse(throttle.allow("loop:a", 100 + MINUTE - 1));
    }

    @Test
    void ilRevientAUneMinuteExactement() {
        assertTrue(throttle.allow("loop:a", 100));
        assertTrue(throttle.allow("loop:a", 100 + MINUTE));
        assertFalse(throttle.allow("loop:a", 100 + MINUTE + 1));
        assertTrue(throttle.allow("loop:a", 100 + 2 * MINUTE));
    }

    @Test
    void deuxProblemesDifferentsSontIndependants() {
        assertTrue(throttle.allow("loop:a", 100));
        assertTrue(throttle.allow("loop:b", 101));
        assertTrue(throttle.allow("saturated", 102));
    }

    @Test
    void oublierUnPrefixeRendLeProblemeSignalableAussitot() {
        assertTrue(throttle.allow("loop:a", 100));
        assertTrue(throttle.allow("saturated", 100));

        throttle.forget("loop:");

        assertTrue(throttle.allow("loop:a", 101));
        assertFalse(throttle.allow("saturated", 101));
    }

    @Test
    void unIntervalleNegatifEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> new MessageThrottle(-1));
    }
}
