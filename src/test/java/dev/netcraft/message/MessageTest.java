package dev.netcraft.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.netcraft.network.DeviceId;

class MessageTest {

    @Test
    void leLogEcritLaCleEtLesValeursSansRienTraduire() {
        Message message = Message.of(MessageLevel.SUCCESS, "message.netcraft.cable.connected",
                MessageArg.device(new DeviceId("minecraft:overworld", 1, 64, -2), "netcraft:pc_tower"),
                MessageArg.number(1),
                MessageArg.key("block.netcraft.laptop"),
                MessageArg.text("02:00:00:00:00:0a"));

        assertEquals("message.netcraft.cable.connected [netcraft:pc_tower [1, 64, -2] minecraft:overworld, 1,"
                + " block.netcraft.laptop, 02:00:00:00:00:0a]", message.logText());
    }

    @Test
    void unMessageSansValeurSEcritParSaCleSeule() {
        assertEquals("message.netcraft.cable.cancelled", Message.of(MessageLevel.INFO, "message.netcraft.cable.cancelled").logText());
    }

    @Test
    void lesDistancesSontArrondiesALEntierLePlusProche() {
        assertEquals(new MessageArg.Number(4), MessageArg.blocks(4.4));
        assertEquals(new MessageArg.Number(5), MessageArg.blocks(4.5));
        assertEquals(new MessageArg.Number(99), MessageArg.blocks(99.0));
    }

    @Test
    void unAppareilDonneLaCleDeSonNom() {
        MessageArg.Device device = new MessageArg.Device("netcraft:internet_box", "minecraft:overworld", 0, 0, 0);
        assertEquals("block.netcraft.internet_box", device.nameKey());
    }
}
