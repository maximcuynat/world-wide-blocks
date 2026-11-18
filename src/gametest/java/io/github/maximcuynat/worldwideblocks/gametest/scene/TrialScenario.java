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
     */
    CHAIN("chain", "two linked boxes, one computer on the left, two on the right",
            List.of(device(-3, 0, ModBlocks.INTERNET_BOX::get), device(3, 0, ModBlocks.INTERNET_BOX::get),
                    device(-3, 3, ModBlocks.PC_TOWER::get), device(3, 3, ModBlocks.LAPTOP::get),
                    device(6, 3, ModBlocks.PC_TOWER::get)),
            List.of(cable(0, 1), cable(0, 2), cable(1, 3), cable(1, 4))),
    /** Two boxes linked by two cables, one computer: loop message in the chat, once per minute. */
    LOOP("loop", "two boxes linked by two cables, one computer",
            List.of(device(-3, 0, ModBlocks.INTERNET_BOX::get), device(3, 0, ModBlocks.INTERNET_BOX::get),
                    device(-3, 3, ModBlocks.PC_TOWER::get)),
            List.of(cable(0, 1), cable(0, 1), cable(0, 2))),
    /** Box with its 4 local network ports in use: one more cord shows "no free port". */
    FULL_PORTS("full_ports", "box with its 4 ports in use",
            List.of(device(0, 0, ModBlocks.INTERNET_BOX::get), device(-3, 3, ModBlocks.PC_TOWER::get),
                    device(-1, 4, ModBlocks.PC_TOWER::get), device(1, 4, ModBlocks.LAPTOP::get),
                    device(3, 3, ModBlocks.LAPTOP::get)),
            List.of(cable(0, 1), cable(0, 2), cable(0, 3), cable(0, 4))),
    /** Cable of 99 blocks, just under the limit of 100: drawing of a long cable. */
    DISTANCE("distance", "cable of 99 blocks",
            List.of(device(0, 0, ModBlocks.PC_TOWER::get), device(0, 99, ModBlocks.LAPTOP::get)),
            List.of(cable(0, 1)));

    /** A device of the scene. */
    public record Device(int right, int forward, Supplier<Block> block) {
    }

    /** A cable between two devices, given by their rank in the list. */
    public record Cable(int from, int to) {
    }

    private final String id;
    private final String description;
    private final List<Device> devices;
    private final List<Cable> cables;

    TrialScenario(String id, String description, List<Device> devices, List<Cable> cables) {
        this.id = id;
        this.description = description;
        this.devices = devices;
        this.cables = cables;
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

    private static Device device(int right, int forward, Supplier<Block> block) {
        return new Device(right, forward, block);
    }

    private static Cable cable(int from, int to) {
        return new Cable(from, to);
    }
}
