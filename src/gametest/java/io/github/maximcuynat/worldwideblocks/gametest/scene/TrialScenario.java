package io.github.maximcuynat.worldwideblocks.gametest.scene;

import java.util.List;
import java.util.function.Supplier;

import io.github.maximcuynat.worldwideblocks.registry.ModBlocks;
import net.minecraft.world.level.block.Block;

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
            Dhcp.serverOff(0));

    /** A device of the scene. */
    public record Device(int right, int forward, Supplier<Block> block) {
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

    private static Cable cable(int from, int to) {
        return new Cable(from, to);
    }
}
