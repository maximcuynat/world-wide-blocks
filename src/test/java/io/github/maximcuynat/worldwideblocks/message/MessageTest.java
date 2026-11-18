package io.github.maximcuynat.worldwideblocks.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;

class MessageTest {

    @Test
    void theLogWritesTheKeyAndTheValuesWithoutTranslatingAnything() {
        Message message = Message.of(MessageLevel.SUCCESS, "message.worldwideblocks.cable.connected",
                MessageArg.device(new DeviceId("minecraft:overworld", 1, 64, -2), "worldwideblocks:pc_tower"),
                MessageArg.number(1),
                MessageArg.key("block.worldwideblocks.laptop"),
                MessageArg.text("02:00:00:00:00:0a"));

        assertEquals("message.worldwideblocks.cable.connected [worldwideblocks:pc_tower [1, 64, -2] minecraft:overworld, 1,"
                + " block.worldwideblocks.laptop, 02:00:00:00:00:0a]", message.logText());
    }

    @Test
    void aMessageWithoutValuesIsWrittenAsItsKeyAlone() {
        assertEquals("message.worldwideblocks.cable.cancelled", Message.of(MessageLevel.INFO, "message.worldwideblocks.cable.cancelled").logText());
    }

    @Test
    void distancesAreRoundedToTheNearestInteger() {
        assertEquals(new MessageArg.Number(4), MessageArg.blocks(4.4));
        assertEquals(new MessageArg.Number(5), MessageArg.blocks(4.5));
        assertEquals(new MessageArg.Number(99), MessageArg.blocks(99.0));
    }

    @Test
    void aDeviceGivesTheKeyOfItsName() {
        MessageArg.Device device = new MessageArg.Device("worldwideblocks:internet_box", "minecraft:overworld", 0, 0, 0);
        assertEquals("block.worldwideblocks.internet_box", device.nameKey());
    }
}
