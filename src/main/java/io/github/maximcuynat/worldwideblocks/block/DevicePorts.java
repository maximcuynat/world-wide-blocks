package io.github.maximcuynat.worldwideblocks.block;

import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.port.PortLayout;
import io.github.maximcuynat.worldwideblocks.port.PortSide;
import io.github.maximcuynat.worldwideblocks.port.PxPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The port layout of a device placed in the world: turned by its {@code FACING}, and the port aimed at by a click. The
 * same code for the server (which port a gesture takes) and the client (outline, panel line, plug).
 */
public final class DevicePorts {
    private DevicePorts() {
    }

    /** The layout of the device in this state, turned by its {@code FACING}; empty for a block that is not a network device. */
    public static Optional<PortLayout> layout(BlockState state) {
        if (!(state.getBlock() instanceof NetworkDeviceBlock device)) {
            return Optional.empty();
        }
        return Optional.of(device.northLayout().turned(quarters(state.getValue(HorizontalDirectionalBlock.FACING))));
    }

    /**
     * The port whose aim zone holds the point clicked on the device, on the face clicked; empty outside every zone (the
     * gesture then takes the first free or occupied port, as before lot 1d-1).
     *
     * @param click the point clicked, in world coordinates ({@code UseOnContext.getClickLocation()}, {@code BlockHitResult.getLocation()})
     */
    public static Optional<PortLayout.Port> aimed(BlockState state, BlockPos pos, Vec3 click, Direction face) {
        return layout(state).flatMap(layout -> layout.portAt(side(face), toPixels(pos, click)));
    }

    /** Quarter turns clockwise from a front facing north: 0 north, 1 east, 2 south, 3 west. */
    public static int quarters(Direction facing) {
        return side(facing).quartersFromNorth();
    }

    public static PortSide side(Direction direction) {
        return switch (direction) {
            case NORTH -> PortSide.NORTH;
            case EAST -> PortSide.EAST;
            case SOUTH -> PortSide.SOUTH;
            case WEST -> PortSide.WEST;
            case UP -> PortSide.UP;
            case DOWN -> PortSide.DOWN;
        };
    }

    public static Direction direction(PortSide side) {
        return switch (side) {
            case NORTH -> Direction.NORTH;
            case EAST -> Direction.EAST;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }

    /** A point of the world in the pixels of the block at {@code pos}. */
    public static PxPoint toPixels(BlockPos pos, Vec3 point) {
        return new PxPoint((point.x - pos.getX()) * PxPoint.BLOCK, (point.y - pos.getY()) * PxPoint.BLOCK, (point.z - pos.getZ()) * PxPoint.BLOCK);
    }

    /** A point in the pixels of the block at {@code pos}, in world coordinates. */
    public static Vec3 toWorld(BlockPos pos, PxPoint point) {
        return new Vec3(pos.getX() + point.x() / PxPoint.BLOCK, pos.getY() + point.y() / PxPoint.BLOCK, pos.getZ() + point.z() / PxPoint.BLOCK);
    }
}
