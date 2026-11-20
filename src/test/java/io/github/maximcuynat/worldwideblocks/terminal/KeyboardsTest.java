package io.github.maximcuynat.worldwideblocks.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;

class KeyboardsTest {
    private static final DeviceId TOWER = new DeviceId("minecraft:overworld", 0, 64, 0);
    private static final DeviceId LAPTOP = new DeviceId("minecraft:overworld", 3, 64, 0);
    private static final UUID ALICE = new UUID(0, 1);
    private static final UUID BOB = new UUID(0, 2);

    @Test
    void onePlayerAtTheKeyboardAtATime() {
        Keyboards keyboards = new Keyboards();

        assertEquals(Keyboards.Take.TAKEN, keyboards.take(TOWER, ALICE, 1));
        assertEquals(Keyboards.Take.BUSY, keyboards.take(TOWER, BOB, 1));
        assertEquals(Keyboards.Take.AGAIN, keyboards.take(TOWER, ALICE, 1));
        assertEquals(Optional.of(TOWER), keyboards.deviceOf(ALICE));
        assertEquals(Optional.empty(), keyboards.deviceOf(BOB));

        keyboards.release(TOWER);
        assertEquals(Keyboards.Take.TAKEN, keyboards.take(TOWER, BOB, 2));
        assertEquals(BOB, keyboards.watch(TOWER).orElseThrow().player());
        assertEquals(2, keyboards.watch(TOWER).orElseThrow().session());
    }

    @Test
    void theUpdatesWaitingAreSentInPacketsAndOnce() {
        Keyboards keyboards = new Keyboards();
        keyboards.take(LAPTOP, ALICE, 1);
        Keyboards.Watch watch = keyboards.watch(LAPTOP).orElseThrow();
        for (int i = 0; i < 5; i++) {
            watch.offer(new TerminalUpdate.Current("line " + i));
        }
        watch.offer(new TerminalUpdate.Ended(0));

        List<List<TerminalUpdate>> packets = watch.drain(4);
        assertEquals(2, packets.size());
        assertEquals(4, packets.get(0).size());
        assertEquals(new TerminalUpdate.Ended(0), packets.get(1).getLast());
        assertTrue(watch.drain(4).isEmpty());
    }
}
