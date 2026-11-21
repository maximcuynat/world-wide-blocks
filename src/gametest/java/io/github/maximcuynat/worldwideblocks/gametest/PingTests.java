package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.List;
import java.util.regex.Pattern;

import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.ip.Ipv4Stack;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.sim.HostLogic;
import io.github.maximcuynat.worldwideblocks.os.ConsoleSession;
import io.github.maximcuynat.worldwideblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

/**
 * Game tests of {@code ping} (lot 1c-1, issue #16): real blocks and cables, addresses from the DHCP server of the box,
 * the command typed in the console of a tower and run by the engine of the test server (ARP, ICMP, the latency of the
 * path). The exact formats are compared with real Debian outputs by the unit tests ({@code PingSimulationTest}); here,
 * the whole chain in the game.
 */
final class PingTests {
    /** Ticks after the cables for the computers to get their address by DHCP. */
    private static final int LEASED = 20;
    /** {@code ping -c 3}: requests at 0, 1 and 2 s, errors of ARP after 3 s; the command has ended after 5 s. */
    private static final int ENDED = 100;
    /** An address of the local network of the box that no device has. */
    private static final Ipv4Address ABSENT = Ipv4Address.of(192, 168, 1, 250);

    private PingTests() {
    }

    // ---- Reading ----

    private static HostLogic host(NetworkScene scene, DeviceId computer) {
        return (HostLogic) scene.network().logic(computer).orElseThrow();
    }

    private static ConsoleSession console(NetworkScene scene, DeviceId computer) {
        return host(scene, computer).system().localConsole();
    }

    private static Ipv4Address address(GameTestHelper helper, NetworkScene scene, DeviceId computer) {
        Ipv4Stack stack = host(scene, computer).system().stack();
        return stack.primaryAddress(stack.interfaceNamed("eth0").orElseThrow())
                .orElseThrow(() -> helper.assertionException(Component.literal("Computer without address: " + computer)));
    }

    /** The lines of the console that match, in order. */
    private static List<String> matching(ConsoleSession console, Pattern pattern) {
        return console.lines().stream().filter(line -> pattern.matcher(line).matches()).toList();
    }

    // ---- Tests ----

    /** Two towers on the box: a {@code ping -c 3} from one to the other gets three replies, without loss. */
    static void betweenTwoComputers(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(8, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        DeviceId other = scene.place(new BlockPos(12, 0, 8), ModBlocks.PC_TOWER.get());
        scene.cable(box, tower);
        scene.cable(box, other);
        Ipv4Address[] target = new Ipv4Address[1];

        helper.startSequence()
                .thenExecuteAfter(LEASED, () -> {
                    target[0] = address(helper, scene, other);
                    helper.assertTrue(!target[0].equals(address(helper, scene, tower)), "two different addresses");
                    scene.network().consoleLine(tower, "ping -c 3 " + target[0]);
                })
                .thenExecuteAfter(ENDED, () -> {
                    ConsoleSession console = console(scene, tower);
                    Pattern reply = Pattern.compile("64 bytes from " + Pattern.quote(target[0].toString())
                            + ": icmp_seq=[123] ttl=64 time=[0-9.]+ ms");
                    List<String> replies = matching(console, reply);
                    helper.assertValueEqual(replies.size(), 3, "replies of the other tower: " + console.lines());
                    for (int sequence = 1; sequence <= 3; sequence++) {
                        helper.assertTrue(replies.get(sequence - 1).contains("icmp_seq=" + sequence + " "),
                                "reply " + sequence + " in order: " + replies);
                    }
                    helper.assertTrue(console.lines().stream()
                            .anyMatch(line -> line.startsWith("3 packets transmitted, 3 received, 0% packet loss")),
                            "statistics without loss: " + console.lines());
                    helper.assertValueEqual(console.lastStatus(), 0, "exit status of ping");
                })
                .thenSucceed();
    }

    /**
     * Lot 1d-1, issue #19: seven computers, more than the box has ports, three on the box and four behind a 5-port switch
     * plugged into the box. Each gets its own address from the box, then each pings the next one in a ring (the last
     * pings the first): every computer answers, through the switch, through the box, or through both.
     */
    static void sevenComputersOnABoxAndASwitch(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(4, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId sw = scene.place(new BlockPos(12, 0, 8), ModBlocks.DESKTOP_SWITCH_5.get());
        scene.cable(box, sw);
        List<DeviceId> computers = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            DeviceId computer = scene.place(new BlockPos(1 + 3 * i, 0, 3), i == 1 ? ModBlocks.LAPTOP.get() : ModBlocks.PC_TOWER.get());
            scene.cable(box, computer);
            computers.add(computer);
        }
        for (int i = 0; i < 4; i++) {
            DeviceId computer = scene.place(new BlockPos(9 + 2 * i, 0, 13), i % 2 == 0 ? ModBlocks.PC_TOWER.get() : ModBlocks.LAPTOP.get());
            scene.cable(sw, computer);
            computers.add(computer);
        }
        List<Ipv4Address> addresses = new java.util.ArrayList<>();

        helper.startSequence()
                .thenExecuteAfter(2 * LEASED, () -> {
                    for (DeviceId computer : computers) {
                        addresses.add(address(helper, scene, computer));
                    }
                    helper.assertValueEqual(new java.util.HashSet<>(addresses).size(), computers.size(), "different addresses: " + addresses);
                    for (int i = 0; i < computers.size(); i++) {
                        scene.network().consoleLine(computers.get(i), "ping -c 2 " + addresses.get((i + 1) % computers.size()));
                    }
                })
                .thenExecuteAfter(ENDED, () -> {
                    for (int i = 0; i < computers.size(); i++) {
                        ConsoleSession console = console(scene, computers.get(i));
                        Ipv4Address target = addresses.get((i + 1) % computers.size());
                        Pattern reply = Pattern.compile("64 bytes from " + Pattern.quote(target.toString())
                                + ": icmp_seq=[12] ttl=64 time=[0-9.]+ ms");
                        helper.assertValueEqual(matching(console, reply).size(), 2,
                                "replies of computer " + ((i + 1) % computers.size() + 1) + " to computer " + (i + 1) + ": " + console.lines());
                        helper.assertTrue(console.lines().stream()
                                .anyMatch(line -> line.startsWith("2 packets transmitted, 2 received, 0% packet loss")),
                                "statistics without loss on computer " + (i + 1) + ": " + console.lines());
                    }
                })
                .thenSucceed();
    }

    /**
     * A {@code ping -c 3} to an address of the local network that nobody has: ARP finds nobody, and the computer
     * reports {@code Destination Host Unreachable} from its own address for each request.
     */
    static void absentAddress(GameTestHelper helper) {
        NetworkScene scene = NetworkScene.of(helper);
        DeviceId box = scene.place(new BlockPos(8, 0, 8), ModBlocks.INTERNET_BOX.get());
        DeviceId tower = scene.place(new BlockPos(4, 0, 8), ModBlocks.PC_TOWER.get());
        scene.cable(box, tower);
        Ipv4Address[] own = new Ipv4Address[1];

        helper.startSequence()
                .thenExecuteAfter(LEASED, () -> {
                    own[0] = address(helper, scene, tower);
                    scene.network().consoleLine(tower, "ping -c 3 " + ABSENT);
                })
                .thenExecuteAfter(ENDED, () -> {
                    ConsoleSession console = console(scene, tower);
                    Pattern unreachable = Pattern.compile("From " + Pattern.quote(own[0].toString())
                            + " icmp_seq=[123] Destination Host Unreachable");
                    helper.assertValueEqual(matching(console, unreachable).size(), 3,
                            "Destination Host Unreachable from the computer itself: " + console.lines());
                    helper.assertTrue(console.lines().stream().noneMatch(line -> line.startsWith("64 bytes from")),
                            "no reply: " + console.lines());
                    helper.assertTrue(console.lines().stream()
                            .anyMatch(line -> line.startsWith("3 packets transmitted, 0 received, +3 errors, 100% packet loss")),
                            "statistics with three errors: " + console.lines());
                    helper.assertValueEqual(console.lastStatus(), 1, "exit status of ping");
                })
                .thenSucceed();
    }
}
