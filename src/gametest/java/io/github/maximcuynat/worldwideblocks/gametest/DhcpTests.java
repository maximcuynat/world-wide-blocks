package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.github.maximcuynat.worldwideblocks.message.MessageArg;
import io.github.maximcuynat.worldwideblocks.message.MessageChannel;
import io.github.maximcuynat.worldwideblocks.message.SentMessage;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.Link;
import io.github.maximcuynat.worldwideblocks.network.NetworkSavedData;
import io.github.maximcuynat.worldwideblocks.network.NetworkTestAccess;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpBinding;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClient;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClientState;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpFrames;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpLease;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpServerConfig;
import io.github.maximcuynat.worldwideblocks.network.protocol.DhcpMessageType;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.sim.BoxLogic;
import io.github.maximcuynat.worldwideblocks.network.sim.FrameDirection;
import io.github.maximcuynat.worldwideblocks.network.sim.HostLogic;
import io.github.maximcuynat.worldwideblocks.network.sim.NetworkSimulation;
import io.github.maximcuynat.worldwideblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

/**
 * Game tests of DHCP (lot 1b-2): real blocks, real exchanges through the engine of the test server.
 *
 * <p>A GameTest callback runs during tick {@code T}, before the simulation processes that tick: it has
 * processed the ticks up to {@code T - 1} ({@code processed} below).
 */
final class DhcpTests {
    private static final String CONFLICT = "message.worldwideblocks.network.dhcp_conflict";
    private static final long LEASE = DhcpServerConfig.INTERNET_BOX.leaseTicks();

    private DhcpTests() {
    }

    // ---- Reading ----

    private static Optional<DhcpClient> client(NetworkScene scene, DeviceId host) {
        return scene.network().logic(host).filter(HostLogic.class::isInstance).map(logic -> ((HostLogic) logic).dhcpClient(0));
    }

    private static Optional<DhcpLease> lease(NetworkScene scene, DeviceId host) {
        return client(scene, host).flatMap(DhcpClient::lease);
    }

    private static DhcpClientState state(NetworkScene scene, DeviceId host) {
        return client(scene, host).map(DhcpClient::state).orElse(DhcpClientState.UNPLUGGED);
    }

    private static List<DhcpBinding> leasesOf(NetworkScene scene, DeviceId box) {
        return scene.network().logic(box).filter(BoxLogic.class::isInstance)
                .map(logic -> ((BoxLogic) logic).dhcpServer().state().leasesAt(scene.gameTime()))
                .orElse(List.of());
    }

    private static void assertInPool(GameTestHelper helper, Ipv4Address address) {
        helper.assertTrue(DhcpServerConfig.INTERNET_BOX.inPool(address), "Address out of the pool: " + address);
    }

    // ---- Tests ----

    /** A tower and a laptop plugged into the box each get their own address from the pool, and the box lists both. */
    static void addressObtained(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(8, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        DeviceId laptop = scene.place(new BlockPos(12, 0, 8), ModBlocks.LAPTOP.get());
        scene.cable(box, tower);
        scene.cable(box, laptop);

        helper.succeedWhen(() -> {
            DhcpLease ofTower = lease(scene, tower).orElseThrow(() -> helper.assertionException(Component.literal("Tower without address")));
            DhcpLease ofLaptop = lease(scene, laptop).orElseThrow(() -> helper.assertionException(Component.literal("Laptop without address")));
            assertInPool(helper, ofTower.address());
            assertInPool(helper, ofLaptop.address());
            helper.assertTrue(!ofTower.address().equals(ofLaptop.address()), "Same address for both: " + ofTower.address());
            helper.assertValueEqual(ofTower.router(), Ipv4Address.of(192, 168, 1, 1), "gateway");
            helper.assertValueEqual(ofTower.subnetMask(), Ipv4Address.of(255, 255, 255, 0), "mask");
            helper.assertValueEqual(ofTower.expiresAt() - ofTower.obtainedAt(), LEASE, "lease of one game day");
            helper.assertValueEqual(leasesOf(scene, box).size(), 2, "leases listed by the box");
        });
    }

    /** Unplugged, the computer loses its address at the tick where the cable is removed. */
    static void addressLostWhenUnplugged(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(8, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        Link cable = scene.cable(box, tower);

        long[] unplugged = {-1};
        helper.onEachTick(() -> {
            long processed = scene.gameTime() - 1;
            if (unplugged[0] < 0) {
                if (lease(scene, tower).isPresent()) {
                    scene.network().disconnect(cable.a());
                    unplugged[0] = processed + 1; // applied by the simulation at this tick
                }
                return;
            }
            if (processed >= unplugged[0]) {
                helper.assertTrue(lease(scene, tower).isEmpty(), "Address kept after the cable was removed");
                helper.assertValueEqual(state(scene, tower), DhcpClientState.UNPLUGGED, "state of the client");
                helper.succeed();
            }
        });
    }

    /** Plugged again, the computer gets the same address back, even after another computer took one. */
    static void sameAddressWhenPluggedAgain(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(8, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        DeviceId laptop = scene.place(new BlockPos(12, 0, 8), ModBlocks.LAPTOP.get());
        Link cable = scene.cable(box, tower);

        Ipv4Address[] first = {null};
        int[] phase = {0};
        helper.onEachTick(() -> {
            switch (phase[0]) {
                case 0 -> lease(scene, tower).ifPresent(lease -> {
                    first[0] = lease.address();
                    scene.network().disconnect(cable.a());
                    phase[0] = 1;
                });
                case 1 -> {
                    if (lease(scene, tower).isEmpty()) {
                        scene.cable(box, laptop); // the laptop must not take the tower's address
                        phase[0] = 2;
                    }
                }
                case 2 -> {
                    if (lease(scene, laptop).isPresent()) {
                        helper.assertTrue(!lease(scene, laptop).get().address().equals(first[0]), "The laptop took the tower's address");
                        scene.cable(box, tower);
                        phase[0] = 3;
                    }
                }
                default -> lease(scene, tower).ifPresent(lease -> {
                    helper.assertValueEqual(lease.address(), first[0], "address of the tower plugged again");
                    helper.succeed();
                });
            }
        });
    }

    /** At half of the lease exactly (T1), the computer renews it with the box: the lease runs one more day from there. */
    static void renewalAtHalfLease(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(8, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        scene.cable(box, tower);

        long[] obtained = {-1};
        helper.onEachTick(() -> {
            long processed = scene.gameTime() - 1;
            if (obtained[0] < 0) {
                lease(scene, tower).ifPresent(lease -> obtained[0] = lease.obtainedAt());
                return;
            }
            long renewal = obtained[0] + LEASE / 2;
            DhcpLease lease = lease(scene, tower).orElseThrow(() -> helper.assertionException(Component.literal("Address lost at tick " + processed)));
            helper.assertValueEqual(state(scene, tower), DhcpClientState.BOUND, "state at tick " + processed);
            if (processed < renewal) {
                helper.assertValueEqual(lease.expiresAt(), obtained[0] + LEASE, "end of the first lease at tick " + processed);
            } else {
                helper.assertValueEqual(lease.obtainedAt(), renewal, "renewal tick");
                helper.assertValueEqual(lease.expiresAt(), renewal + LEASE, "end of the renewed lease");
                helper.succeed();
            }
        });
    }

    /**
     * The box stops answering (its DHCP server is turned off): the computer renews in vain from T1, rebinds by
     * broadcast from T2, and loses its address at the end of the lease, to the tick, then looks for a server again.
     */
    static void leaseExpires(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(8, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        scene.cable(box, tower);
        MinecraftServer server = helper.getLevel().getServer();

        long[] obtained = {-1};
        helper.onEachTick(() -> {
            long processed = scene.gameTime() - 1;
            if (obtained[0] < 0) {
                lease(scene, tower).ifPresent(lease -> {
                    obtained[0] = lease.obtainedAt();
                    NetworkTestAccess.setDhcpServerEnabled(server, box, false);
                });
                return;
            }
            long t1 = obtained[0] + LEASE / 2;
            long t2 = obtained[0] + LEASE * 7 / 8;
            long end = obtained[0] + LEASE;
            DhcpClientState state = state(scene, tower);
            if (processed < end) {
                helper.assertTrue(lease(scene, tower).isPresent(), "Address lost at tick " + processed + ", before the end at " + end);
                DhcpClientState expected = processed < t1 ? DhcpClientState.BOUND
                        : processed < t2 ? DhcpClientState.RENEWING : DhcpClientState.REBINDING;
                helper.assertValueEqual(state, expected, "state at tick " + processed);
            } else {
                helper.assertTrue(lease(scene, tower).isEmpty(), "Address kept after the end of the lease");
                helper.assertValueEqual(state, DhcpClientState.SELECTING, "state after the end of the lease");
                helper.succeed();
            }
        });
    }

    /** Two computers linked without a box: no address, both keep looking for a server. */
    static void noBox(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        DeviceId laptop = scene.place(new BlockPos(12, 0, 8), ModBlocks.LAPTOP.get());
        scene.cable(tower, laptop);

        helper.runAfterDelay(400, () -> {
            for (DeviceId host : List.of(tower, laptop)) {
                helper.assertTrue(lease(scene, host).isEmpty(), "Address without a box on " + host);
                helper.assertValueEqual(state(scene, host), DhcpClientState.SELECTING, "state of " + host);
            }
            helper.succeed();
        });
    }

    /**
     * Two linked boxes with their server on, a computer: the nearby player receives one conflict warning naming
     * one of the two boxes, and the computer still gets an address. A second computer plugged in changes the
     * network: the conflict is reported again at once.
     */
    static void conflictOfTwoBoxes(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId boxA = scene.place(new BlockPos(4, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId boxB = scene.place(new BlockPos(12, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(12, 0, 12), ModBlocks.PC_TOWER.get());
        DeviceId laptop = scene.place(new BlockPos(4, 0, 12), ModBlocks.LAPTOP.get());
        TestPlayer player = scene.player(GameType.CREATIVE);
        scene.cable(boxA, boxB);
        scene.cable(boxB, tower);
        Set<DeviceId> boxes = Set.of(boxA, boxB);

        helper.runAfterDelay(100, () -> {
            helper.assertValueEqual(conflictChat(scene, player, boxes).size(), 1, "conflict warnings");
            helper.assertTrue(lease(scene, tower).isPresent(), "The computer got no address");
            long logLines = scene.messages().logs(CONFLICT).stream().filter(message -> names(message, boxes)).count();
            helper.assertValueEqual(logLines, 1L, "log lines of the conflict");
            scene.cable(boxA, laptop);
            helper.runAfterDelay(100, () -> {
                helper.assertValueEqual(conflictChat(scene, player, boxes).size(), 2, "conflict warnings after the network changed");
                helper.succeed();
            });
        });
    }

    /**
     * Server restart: the network is written to {@code network.dat} then read back by a new storage, whose
     * simulation starts from zero with the saved memory. The computer asks for its address again (INIT-REBOOT:
     * a REQUEST, no DISCOVER) and gets the same one.
     */
    static void restartAsksForTheSameAddress(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(8, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        scene.cable(box, tower);

        helper.runAfterDelay(20, () -> {
            DhcpLease before = lease(scene, tower).orElseThrow(() -> helper.assertionException(Component.literal("Tower without address")));
            MinecraftServer server = helper.getLevel().getServer();
            server.getDataStorage().saveAndJoin();
            try (SavedDataStorage disk = new SavedDataStorage(server.overworld(), server.getWorldPath(LevelResource.DATA),
                    server.getFixerUpper(), server.registryAccess())) {
                NetworkSavedData reloaded = disk.get(NetworkSavedData.TYPE);
                helper.assertTrue(reloaded != null, "network.dat missing or unreadable");
                PortRef towerPort = new PortRef(tower, 0);
                helper.assertValueEqual(NetworkTestAccess.dhcp(reloaded).clients().get(towerPort), before, "saved lease of the tower");
                helper.assertTrue(NetworkTestAccess.dhcp(reloaded).server(box).bindingOf(scene.network().macsOf(tower).getFirst())
                        .filter(binding -> binding.address().equals(before.address())).isPresent(), "saved binding of the box");

                NetworkSimulation restarted = NetworkTestAccess.simulation(reloaded);
                List<DhcpMessageType> sent = new ArrayList<>();
                restarted.setTap((now, port, direction, frame) -> {
                    if (direction == FrameDirection.SENT && port.equals(towerPort)) {
                        DhcpFrames.toServer(frame).ifPresent(message -> sent.add(message.type()));
                    }
                });
                long now = scene.gameTime();
                restarted.tick(now);
                restarted.tick(now + 1);

                DhcpClient client = restarted.logic(tower).map(logic -> ((HostLogic) logic).dhcpClient(0)).orElseThrow();
                helper.assertValueEqual(sent, List.of(DhcpMessageType.REQUEST), "messages of the tower after the restart");
                helper.assertValueEqual(client.state(), DhcpClientState.BOUND, "state after the restart");
                helper.assertValueEqual(client.lease().orElseThrow().address(), before.address(), "address after the restart");
            }
            helper.succeed();
        });
    }

    /** The whole pool of the box is lent: a computer plugged in gets no address and keeps looking. */
    static void poolFull(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(8, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        int lent = NetworkTestAccess.fillDhcpPool(helper.getLevel().getServer(), box, scene.gameTime());
        helper.assertValueEqual(lent, DhcpServerConfig.INTERNET_BOX.poolSize(), "addresses lent before the computer");
        scene.cable(box, tower);

        helper.runAfterDelay(400, () -> {
            helper.assertTrue(lease(scene, tower).isEmpty(), "Address obtained from a full pool");
            helper.assertValueEqual(state(scene, tower), DhcpClientState.SELECTING, "state of the computer");
            helper.assertValueEqual(leasesOf(scene, box).size(), DhcpServerConfig.INTERNET_BOX.poolSize(), "leases of the box");
            helper.succeed();
        });
    }

    // ---- Messages ----

    private static List<SentMessage> conflictChat(NetworkScene scene, TestPlayer player, Set<DeviceId> boxes) {
        return scene.messages().to(player.getUUID(), MessageChannel.CHAT, CONFLICT).stream()
                .filter(message -> names(message, boxes))
                .toList();
    }

    private static boolean names(SentMessage message, Set<DeviceId> devices) {
        if (message.args().isEmpty() || !(message.args().getFirst() instanceof MessageArg.Device device)) {
            return false;
        }
        return devices.contains(new DeviceId(device.dimension(), device.x(), device.y(), device.z()));
    }
}
