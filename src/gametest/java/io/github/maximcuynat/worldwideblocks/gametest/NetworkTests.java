package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.message.MessageArg;
import io.github.maximcuynat.worldwideblocks.message.MessageChannel;
import io.github.maximcuynat.worldwideblocks.message.SentMessage;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.NetworkManager;
import io.github.maximcuynat.worldwideblocks.network.NetworkSavedData;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.sim.BoxLogic;
import io.github.maximcuynat.worldwideblocks.network.sim.DeviceLogic;
import io.github.maximcuynat.worldwideblocks.network.sim.SimulationLimits;
import io.github.maximcuynat.worldwideblocks.network.switching.MacTable;
import io.github.maximcuynat.worldwideblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

/** Game tests of the network engine: switching, save, loop. */
final class NetworkTests {
    private static final String LOOP = "message.worldwideblocks.network.loop";

    private NetworkTests() {
    }

    /**
     * Box A linked to box B, a computer H on B. A learns H behind its port towards B. H is unplugged
     * from B: B forgets it at the next tick (cable removed), A knows nothing about it and keeps it
     * exactly 300 s of game time after the last frame of H, then removes it at the next cleanup (at
     * most one second later).
     *
     * <p>Checked at every tick on the game time: a GameTest callback runs during tick {@code T},
     * before the simulation processes that tick; it has therefore processed the ticks up to {@code T - 1}.
     */
    static void macExpiresAfter300Seconds(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId boxA = scene.place(new BlockPos(4, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId boxB = scene.place(new BlockPos(12, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId host = scene.place(new BlockPos(12, 0, 12), ModBlocks.PC_TOWER.get());
        Link ab = scene.cable(boxA, boxB);
        Link bh = scene.cable(boxB, host);
        MacAddress hostMac = scene.network().macsOf(host).getFirst();

        long[] unplugged = {-1};
        long[] expiry = {-1};
        helper.onEachTick(() -> {
            long processed = scene.gameTime() - 1;
            if (unplugged[0] < 0) {
                Optional<MacTable.Entry> learned = learned(scene, boxA, hostMac);
                if (learned.isEmpty()) {
                    return; // the presence of H has not been delivered yet
                }
                helper.assertValueEqual(learned.get().port(), endOn(ab, boxA).index(), "port of A behind which H is");
                scene.network().disconnect(endOn(bh, host));
                unplugged[0] = processed + 1; // applied by the simulation at this tick
                expiry[0] = learned.get().lastSeen() + MacTable.DEFAULT_AGING_TICKS;
                return;
            }
            if (processed >= unplugged[0]) {
                helper.assertTrue(learned(scene, boxB, hostMac).isEmpty(), "B keeps the MAC of H one tick after its cable was removed");
            }
            if (processed < expiry[0]) {
                helper.assertTrue(learned(scene, boxA, hostMac).isPresent(),
                        "A forgot the MAC of H at tick " + processed + ", before its expiry at tick " + expiry[0]);
            } else if (processed >= expiry[0] + BoxLogic.PURGE_INTERVAL_TICKS - 1) {
                helper.assertTrue(learned(scene, boxA, hostMac).isEmpty(),
                        "A keeps the MAC of H at tick " + processed + ", more than one second after its expiry at tick " + expiry[0]);
                helper.succeed();
            }
        });
    }

    /**
     * The network is written to {@code data/worldwideblocks/network.dat} as at a world save, then
     * read back by a new storage, as at world load: same devices, MACs and cables. (The server
     * itself does not restart.)
     */
    static void networkSurvivesSaveAndReload(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(8, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        DeviceId laptop = scene.place(new BlockPos(12, 0, 8), ModBlocks.LAPTOP.get());
        scene.cable(box, tower);
        scene.cable(box, laptop);

        MinecraftServer server = helper.getLevel().getServer();
        server.getDataStorage().saveAndJoin();
        try (SavedDataStorage disk = new SavedDataStorage(server.overworld(), server.getWorldPath(LevelResource.DATA),
                server.getFixerUpper(), server.registryAccess())) {
            NetworkSavedData reloaded = disk.get(NetworkSavedData.TYPE);
            helper.assertTrue(reloaded != null, "network.dat missing or unreadable");
            NetworkManager live = scene.network();
            for (DeviceId id : List.of(box, tower, laptop)) {
                helper.assertValueEqual(reloaded.node(id), live.node(id), "device " + id + " read back");
                helper.assertValueEqual(reloaded.macsOf(id), live.macsOf(id), "MACs of " + id + " read back");
                helper.assertValueEqual(Set.copyOf(reloaded.linksOf(id)), Set.copyOf(live.linksOf(id)), "cables of " + id + " read back");
            }
        }
        helper.succeed();
    }

    /**
     * Two boxes linked by two cables, a computer on A: each broadcast frame loops, and both boxes see
     * it. The nearby player receives **one** message per game minute (not one per box), naming one
     * of the two boxes, and the log has only one line per message. The presence of the computer comes
     * back every 30 s: the one-minute limit is therefore really put to the test. The messages stop
     * when one of the two cables is removed.
     *
     * <p>Separate batch: the frame limit is global to the engine.
     */
    static void loopReportedOncePerMinute(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId boxA = scene.place(new BlockPos(4, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId boxB = scene.place(new BlockPos(12, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId host = scene.place(new BlockPos(4, 0, 12), ModBlocks.PC_TOWER.get());
        Link firstCable = scene.cable(boxA, boxB);
        scene.cable(boxA, boxB);
        TestPlayer player = scene.player(GameType.CREATIVE);
        // Plugged in last: its presence, broadcast at connection then every 30 s, finds the loop.
        scene.cable(host, boxA);
        Set<DeviceId> boxes = Set.of(boxA, boxB);

        long interval = SimulationLimits.DEFAULT.reportIntervalTicks();
        long observation = 2 * interval + 200;
        helper.runAfterDelay(observation, () -> {
            List<SentMessage> chat = loopChat(scene, player, boxes);
            List<Long> times = chat.stream().map(SentMessage::gameTime).toList();
            WorldWideBlocks.LOGGER.info("[GameTest] loop messages received at game times {}", times);
            helper.assertTrue(!times.isEmpty(), "No loop message in " + observation + " ticks");
            // Exactly one message per minute, at 0, 1,200 and 2,400 ticks after the first. A larger gap
            // (1,800 before the clock fix of 2026-11-18) is a defect, not a margin.
            long firstReport = times.getFirst();
            helper.assertValueEqual(times, List.of(firstReport, firstReport + interval, firstReport + 2 * interval),
                    "times of the loop messages (only one per minute for both boxes)");
            long logLines = scene.messages().logs(LOOP).stream().filter(message -> names(message, boxes)).count();
            helper.assertValueEqual(logLines, 3L, "log lines for this loop");

            int before = chat.size();
            scene.network().disconnect(firstCable.a());
            helper.runAfterDelay(interval + 100, () -> {
                helper.assertValueEqual(loopChat(scene, player, boxes).size(), before, "loop messages after a cable was removed");
                helper.succeed();
            });
        });
    }

    /**
     * A change in the looping network makes it forget its limit (the loop is reported again at once);
     * a change in another network does not touch it.
     *
     * <ul>
     * <li>S+1: presence of the first computer, first message;</li>
     * <li>S+100: a second computer plugged into box B (this network changes): second message at the next tick;</li>
     * <li>S+200: two PC towers linked elsewhere (another network changes);</li>
     * <li>S+601 and S+701: presences of the two computers, within the minute of the second message: nothing.</li>
     * </ul>
     * A global reset would give a third message at S+601; no reset at all, a single message in total.
     *
     * <p>Separate batch, like the previous test.
     */
    static void loopForgottenOnlyForChangedNetwork(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId boxA = scene.place(new BlockPos(4, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId boxB = scene.place(new BlockPos(12, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId host = scene.place(new BlockPos(4, 0, 12), ModBlocks.PC_TOWER.get());
        DeviceId secondHost = scene.place(new BlockPos(12, 0, 12), ModBlocks.PC_TOWER.get());
        scene.cable(boxA, boxB);
        scene.cable(boxA, boxB);
        TestPlayer player = scene.player(GameType.CREATIVE);
        scene.cable(host, boxA);
        Set<DeviceId> boxes = Set.of(boxA, boxB);

        helper.runAfterDelay(100, () -> {
            helper.assertValueEqual(loopChat(scene, player, boxes).size(), 1, "loop messages before the second computer");
            scene.cable(secondHost, boxB);
            helper.runAfterDelay(100, () -> {
                helper.assertValueEqual(loopChat(scene, player, boxes).size(), 2, "loop messages after the change in this network");
                DeviceId towerA = scene.place(new BlockPos(1, 0, 1), ModBlocks.PC_TOWER.get());
                DeviceId towerB = scene.place(new BlockPos(3, 0, 1), ModBlocks.PC_TOWER.get());
                scene.cable(towerA, towerB);
                helper.runAfterDelay(600, () -> {
                    helper.assertValueEqual(loopChat(scene, player, boxes).size(), 2,
                            "loop messages after a change in another network");
                    helper.succeed();
                });
            });
        });
    }

    /** Loop messages received by the player that name one of these boxes (the batch is shared with other loops). */
    private static List<SentMessage> loopChat(NetworkScene scene, TestPlayer player, Set<DeviceId> boxes) {
        return scene.messages().to(player.getUUID(), MessageChannel.CHAT, LOOP).stream()
                .filter(message -> names(message, boxes))
                .toList();
    }

    private static boolean names(SentMessage message, Set<DeviceId> devices) {
        return deviceOf(message).filter(devices::contains).isPresent();
    }

    /** Device named by the first value of a message. */
    private static Optional<DeviceId> deviceOf(SentMessage message) {
        if (message.args().isEmpty() || !(message.args().getFirst() instanceof MessageArg.Device device)) {
            return Optional.empty();
        }
        return Optional.of(new DeviceId(device.dimension(), device.x(), device.y(), device.z()));
    }

    private static Optional<MacTable.Entry> learned(NetworkScene scene, DeviceId device, MacAddress mac) {
        return scene.network().logic(device).map(DeviceLogic::learnedMacs).orElse(List.of()).stream()
                .filter(entry -> entry.mac().equals(mac))
                .findFirst();
    }

    private static PortRef endOn(Link link, DeviceId device) {
        return link.a().device().equals(device) ? link.a() : link.b();
    }
}
