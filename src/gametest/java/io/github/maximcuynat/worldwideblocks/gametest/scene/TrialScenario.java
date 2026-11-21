package io.github.maximcuynat.worldwideblocks.gametest.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.github.maximcuynat.worldwideblocks.block.LaptopBlock;
import io.github.maximcuynat.worldwideblocks.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Scenes of the checks by eye, built in front of the player by {@code /worldwideblocks test <scene>}.
 *
 * <p>Local coordinates: {@code right} towards the player's right, {@code forward} straight ahead,
 * from a point 3 blocks in front of their feet.
 */
public enum TrialScenario {
    /** Box alone: 4 free local network ports, inactive line input, front facing the player. */
    BOX("box", "box alone",
            List.of(device(0, 0, ModBlocks.INTERNET_BOX::get)),
            List.of()),
    /** Box and 3 computers: MAC of each computer, MACs learned by the box on 3 ports. */
    BOX_HOSTS("box_hosts", "box and 3 connected computers",
            List.of(device(0, 0, ModBlocks.INTERNET_BOX::get), device(-3, 3, ModBlocks.PC_TOWER::get),
                    device(0, 4, ModBlocks.LAPTOP::get), device(3, 3, ModBlocks.PC_TOWER::get)),
            List.of(cable(0, 1), cable(0, 2), cable(0, 3))),
    /**
     * Two linked boxes, one computer on the left box and two on the right one: each box also learns
     * the computers of the other, and port 1 of the left box carries 2 MACs (readability of the panel).
     * The DHCP server of the right box is off, as one would do in reality: all computers get their address
     * from the left box.
     */
    CHAIN("chain", "two linked boxes, one computer on the left, two on the right",
            List.of(device(-3, 0, ModBlocks.INTERNET_BOX::get), device(3, 0, ModBlocks.INTERNET_BOX::get),
                    device(-3, 3, ModBlocks.PC_TOWER::get), device(3, 3, ModBlocks.LAPTOP::get),
                    device(6, 3, ModBlocks.PC_TOWER::get)),
            List.of(cable(0, 1), cable(0, 2), cable(1, 3), cable(1, 4)),
            Dhcp.serverOff(1)),
    /**
     * Two boxes linked by two cables, one computer: loop message in the chat, once per minute. The DHCP
     * server of the right box is off, so that only the loop is reported.
     */
    LOOP("loop", "two boxes linked by two cables, one computer",
            List.of(device(-3, 0, ModBlocks.INTERNET_BOX::get), device(3, 0, ModBlocks.INTERNET_BOX::get),
                    device(-3, 3, ModBlocks.PC_TOWER::get)),
            List.of(cable(0, 1), cable(0, 1), cable(0, 2)),
            Dhcp.serverOff(1)),
    /** Box with its 4 local network ports in use: one more cord shows "no free port". */
    FULL_PORTS("full_ports", "box with its 4 ports in use",
            List.of(device(0, 0, ModBlocks.INTERNET_BOX::get), device(-3, 3, ModBlocks.PC_TOWER::get),
                    device(-1, 4, ModBlocks.PC_TOWER::get), device(1, 4, ModBlocks.LAPTOP::get),
                    device(3, 3, ModBlocks.LAPTOP::get)),
            List.of(cable(0, 1), cable(0, 2), cable(0, 3), cable(0, 4))),
    /** Cable of 99 blocks, just under the limit of 100: drawing of a long cable. */
    DISTANCE("distance", "cable of 99 blocks",
            List.of(device(0, 0, ModBlocks.PC_TOWER::get), device(0, 99, ModBlocks.LAPTOP::get)),
            List.of(cable(0, 1))),
    /** Two computers linked without a box: no address, the DHCP client keeps looking ("selecting"). */
    DHCP_NO_BOX("dhcp_no_box", "two computers linked without a box",
            List.of(device(-2, 2, ModBlocks.PC_TOWER::get), device(2, 2, ModBlocks.LAPTOP::get)),
            List.of(cable(0, 1))),
    /** Two linked boxes with their DHCP server on, one computer: conflict warning in the chat. */
    DHCP_CONFLICT("dhcp_conflict", "two linked boxes with their DHCP server on, one computer",
            List.of(device(-3, 0, ModBlocks.INTERNET_BOX::get), device(3, 0, ModBlocks.INTERNET_BOX::get),
                    device(3, 3, ModBlocks.PC_TOWER::get)),
            List.of(cable(0, 1), cable(1, 2))),
    /** Box whose 191 addresses are all lent to clients that do not exist, one computer: no address left. */
    DHCP_POOL_FULL("dhcp_pool_full", "box with all its addresses lent, one computer",
            List.of(device(0, 0, ModBlocks.INTERNET_BOX::get), device(0, 3, ModBlocks.PC_TOWER::get)),
            List.of(cable(0, 1)),
            Dhcp.poolFull(0)),
    /** Box with its DHCP server off, one computer: the box says so, the computer finds no server. */
    DHCP_SERVER_OFF("dhcp_server_off", "box with its DHCP server off, one computer",
            List.of(device(0, 0, ModBlocks.INTERNET_BOX::get), device(0, 3, ModBlocks.PC_TOWER::get)),
            List.of(cable(0, 1)),
            Dhcp.serverOff(0)),
    /**
     * Port chosen by aiming (issue #62): a box on the left and a 5-port switch on the right, each with port 1 linked to a
     * computer behind it, so that a free port and a connected port can be aimed at on both.
     */
    PORT_AIM("port_aim", "box and 5-port switch, port 1 of each linked to a computer",
            List.of(device(-2, 0, ModBlocks.INTERNET_BOX::get), device(2, 0, ModBlocks.DESKTOP_SWITCH_5::get),
                    device(-3, 3, ModBlocks.PC_TOWER::get), device(3, 3, ModBlocks.LAPTOP::get)),
            List.of(cable(0, 2), cable(1, 3))),
    /**
     * Plugs and cables (issue #63): a box and a 5-port switch facing the player, linked to each other and to computers
     * turned by 90, 180 and 270 degrees, the 5-port switch to an 8-port switch turned by 90 degrees, which goes on to a
     * box turned by 270 degrees: a plug in a port of every kind of device, in the four orientations.
     */
    PLUGS("plugs", "box and switches linked to turned computers and devices",
            List.of(device(-3, 0, ModBlocks.INTERNET_BOX::get), device(3, 0, ModBlocks.DESKTOP_SWITCH_5::get),
                    new Device(-5, 3, ModBlocks.PC_TOWER::get, 90, UnaryOperator.identity()),
                    new Device(-2, 4, ModBlocks.LAPTOP::get, 180, UnaryOperator.identity()),
                    new Device(1, 4, ModBlocks.PC_TOWER::get, 270, UnaryOperator.identity()),
                    new Device(5, 3, ModBlocks.DESKTOP_SWITCH_8::get, 90, UnaryOperator.identity()),
                    new Device(4, 7, ModBlocks.INTERNET_BOX::get, 270, UnaryOperator.identity())),
            List.of(cable(0, 2), cable(0, 3), cable(0, 1), cable(1, 4), cable(1, 5), cable(5, 6))),
    /**
     * Internet box for its capture scenario (issue #68): one box alone in front of the player for the close-up views,
     * and four more in a row behind it, turned by 0, 90, 180 and 270 degrees.
     */
    DEVICE_BOX("device_box", "box alone, then four boxes in the four orientations",
            turnedRow(ModBlocks.INTERNET_BOX::get, UnaryOperator.identity()),
            List.of()),
    /** PC tower for its capture scenario, as {@link #DEVICE_BOX}. */
    DEVICE_TOWER("device_tower", "tower alone, then four towers in the four orientations",
            turnedRow(ModBlocks.PC_TOWER::get, UnaryOperator.identity()),
            List.of()),
    /** Laptop for its capture scenario, as {@link #DEVICE_BOX}, with a closed laptop alone on the right (device 5). */
    DEVICE_LAPTOP("device_laptop", "laptop alone, open then closed, then four laptops in the four orientations",
            withClosedLaptop(turnedRow(ModBlocks.LAPTOP::get, UnaryOperator.identity())),
            List.of()),
    /** 5-port desktop switch for its capture scenario (issue #17), as {@link #DEVICE_BOX}. */
    DEVICE_SWITCH_5("device_switch_5", "5-port switch alone, then four in the four orientations",
            turnedRow(ModBlocks.DESKTOP_SWITCH_5::get, UnaryOperator.identity()),
            List.of()),
    /** 8-port desktop switch for its capture scenario (issue #18), as {@link #DEVICE_BOX}. */
    DEVICE_SWITCH_8("device_switch_8", "8-port switch alone, then four in the four orientations",
            turnedRow(ModBlocks.DESKTOP_SWITCH_8::get, UnaryOperator.identity()),
            List.of());

    /**
     * A device of the scene.
     *
     * @param turn  degrees clockwise, seen from above, from the front facing the player: 0, 90, 180 or 270
     * @param state changes to the default state of the block, after its orientation (a closed laptop)
     */
    public record Device(int right, int forward, Supplier<Block> block, int turn, UnaryOperator<BlockState> state) {
        public Device {
            if (turn % 90 != 0 || turn < 0 || turn >= 360) {
                throw new IllegalArgumentException("turn of a device: 0, 90, 180 or 270, not " + turn);
            }
        }

        /** A device whose front faces the player, in its default state. */
        public Device(int right, int forward, Supplier<Block> block) {
            this(right, forward, block, 0, UnaryOperator.identity());
        }

        /** The direction its front faces, in a scene built in front of a player looking toward {@code forward}. */
        public Direction facing(Direction forward) {
            Direction facing = forward.getOpposite();
            for (int quarter = 0; quarter < this.turn / 90; quarter++) {
                facing = facing.getClockWise();
            }
            return facing;
        }
    }

    /** A cable between two devices, given by their rank in the list. */
    public record Cable(int from, int to) {
    }

    /**
     * DHCP settings of the boxes of the scene, given by their rank in the list, applied before the cables.
     *
     * @param serverOff boxes whose DHCP server is turned off
     * @param poolFull  boxes whose whole pool is lent to clients that do not exist
     */
    public record Dhcp(List<Integer> serverOff, List<Integer> poolFull) {
        static final Dhcp NONE = new Dhcp(List.of(), List.of());

        static Dhcp serverOff(int box) {
            return new Dhcp(List.of(box), List.of());
        }

        static Dhcp poolFull(int box) {
            return new Dhcp(List.of(), List.of(box));
        }
    }

    private final String id;
    private final String description;
    private final List<Device> devices;
    private final List<Cable> cables;
    private final Dhcp dhcp;

    TrialScenario(String id, String description, List<Device> devices, List<Cable> cables) {
        this(id, description, devices, cables, Dhcp.NONE);
    }

    TrialScenario(String id, String description, List<Device> devices, List<Cable> cables, Dhcp dhcp) {
        this.id = id;
        this.description = description;
        this.devices = devices;
        this.cables = cables;
        this.dhcp = dhcp;
    }

    public String id() {
        return this.id;
    }

    public String description() {
        return this.description;
    }

    public List<Device> devices() {
        return this.devices;
    }

    public List<Cable> cables() {
        return this.cables;
    }

    public Dhcp dhcp() {
        return this.dhcp;
    }

    private static Device device(int right, int forward, Supplier<Block> block) {
        return new Device(right, forward, block);
    }

    /**
     * Device 0 alone, front facing the player, for the close-up views; devices 1 to 4 in a row 4 blocks further, 2
     * blocks apart, turned by 0, 90, 180 and 270 degrees from left to right.
     */
    private static List<Device> turnedRow(Supplier<Block> block, UnaryOperator<BlockState> state) {
        List<Device> devices = new ArrayList<>();
        devices.add(new Device(0, 0, block, 0, state));
        for (int i = 0; i < 4; i++) {
            devices.add(new Device(-3 + 2 * i, 4, block, 90 * i, state));
        }
        return List.copyOf(devices);
    }

    /** The same devices, and a closed laptop alone 4 blocks to the right of device 0. */
    private static List<Device> withClosedLaptop(List<Device> devices) {
        List<Device> all = new ArrayList<>(devices);
        all.add(new Device(4, 0, ModBlocks.LAPTOP::get, 0, state -> state.setValue(LaptopBlock.OPEN, false)));
        return List.copyOf(all);
    }

    private static Cable cable(int from, int to) {
        return new Cable(from, to);
    }
}
