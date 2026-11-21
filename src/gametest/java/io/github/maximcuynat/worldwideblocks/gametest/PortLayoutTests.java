package io.github.maximcuynat.worldwideblocks.gametest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.github.maximcuynat.worldwideblocks.block.DevicePorts;
import io.github.maximcuynat.worldwideblocks.block.NetworkDeviceBlock;
import io.github.maximcuynat.worldwideblocks.port.PortLayout;
import io.github.maximcuynat.worldwideblocks.port.PortSide;
import io.github.maximcuynat.worldwideblocks.port.PxBox;
import io.github.maximcuynat.worldwideblocks.port.PxPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Game tests of the port layouts against the real blocks (issue #61), in every state of each device (four orientations,
 * laptop open and closed): the ports of the layout are those of the block; every aim zone lies on the surface of the
 * selection shape of the block (just inside it, nothing of it just outside); a click in the middle of a zone, on its
 * face, chooses its port through {@link DevicePorts#aimed}, the code the gestures and the client use.
 */
final class PortLayoutTests {
    /** How far a sample point is moved in and out of the face, in pixels. */
    private static final double STEP = 0.01;

    private PortLayoutTests() {
    }

    static Stream<WorldWideBlocksGameTests.Case> cases() {
        return DeviceShapeTests.DEVICES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(device -> new WorldWideBlocksGameTests.Case("port_layout_" + device.getKey(), WorldWideBlocksGameTests.Batch.SHARED, 20,
                        helper -> zonesLieOnTheShape(helper, device.getValue())));
    }

    static void zonesLieOnTheShape(GameTestHelper helper, Supplier<? extends Block> device) {
        Block block = device.get();
        String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
        helper.assertTrue(block instanceof NetworkDeviceBlock, id + " is not a network device block");
        NetworkDeviceBlock networkDevice = (NetworkDeviceBlock) block;
        helper.assertValueEqual(networkDevice.ports(), networkDevice.northLayout().kinds(), "ports of " + id + " and of its layout");
        List<BlockState> states = block.getStateDefinition().getPossibleStates();
        for (int i = 0; i < states.size(); i++) {
            BlockState state = states.get(i);
            BlockPos pos = new BlockPos(1 + 2 * (i % 7), 0, 1 + 2 * (i / 7));
            helper.setBlock(pos, state);
            BlockPos absolute = helper.absolutePos(pos);
            VoxelShape shape = state.getShape(helper.getLevel(), absolute);
            PortLayout layout = DevicePorts.layout(state).orElseThrow();
            for (PortLayout.Port port : layout.ports()) {
                for (PortLayout.Area area : port.zone()) {
                    String what = id + " " + state + ", zone of port " + port.displayNumber() + " on " + area.side();
                    for (PxPoint sample : samples(area)) {
                        helper.assertTrue(inside(shape, sample.moved(area.side(), -STEP)), what + ": " + sample + " is not on the shape");
                        helper.assertFalse(inside(shape, sample.moved(area.side(), STEP)), what + ": " + sample + " is inside the shape");
                    }
                    Vec3 click = DevicePorts.toWorld(absolute, area.box().centre());
                    Optional<Integer> aimed = DevicePorts.aimed(state, absolute, click, DevicePorts.direction(area.side()))
                            .map(PortLayout.Port::index);
                    helper.assertValueEqual(aimed, Optional.of(port.index()), what + ": port aimed by a click in its middle");
                }
            }
            helper.destroyBlock(pos);
        }
        helper.succeed();
    }

    /** The middle and the four corners of a rectangle of a zone, the corners moved in by a hundredth of a pixel. */
    private static List<PxPoint> samples(PortLayout.Area area) {
        PxBox box = area.box();
        PortSide side = area.side();
        double inset = STEP;
        double x0 = box.minX() + (side.x() == 0 ? inset : 0);
        double x1 = box.maxX() - (side.x() == 0 ? inset : 0);
        double y0 = box.minY() + (side.y() == 0 ? inset : 0);
        double y1 = box.maxY() - (side.y() == 0 ? inset : 0);
        double z0 = box.minZ() + (side.z() == 0 ? inset : 0);
        double z1 = box.maxZ() - (side.z() == 0 ? inset : 0);
        return switch (side) {
            case NORTH, SOUTH -> List.of(box.centre(), new PxPoint(x0, y0, z0), new PxPoint(x1, y0, z0), new PxPoint(x0, y1, z0), new PxPoint(x1, y1, z0));
            case EAST, WEST -> List.of(box.centre(), new PxPoint(x0, y0, z0), new PxPoint(x0, y1, z0), new PxPoint(x0, y0, z1), new PxPoint(x0, y1, z1));
            case UP, DOWN -> List.of(box.centre(), new PxPoint(x0, y0, z0), new PxPoint(x1, y0, z0), new PxPoint(x0, y0, z1), new PxPoint(x1, y0, z1));
        };
    }

    /** True if a point of the block, in pixels, is inside one of the boxes of the shape. */
    private static boolean inside(VoxelShape shape, PxPoint point) {
        double x = point.x() / PxPoint.BLOCK;
        double y = point.y() / PxPoint.BLOCK;
        double z = point.z() / PxPoint.BLOCK;
        for (AABB box : shape.toAabbs()) {
            if (box.contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }
}
