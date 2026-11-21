package io.github.maximcuynat.worldwideblocks.block;

import java.util.Map;

import io.github.maximcuynat.worldwideblocks.network.DeviceRole;
import io.github.maximcuynat.worldwideblocks.port.PortLayout;
import io.github.maximcuynat.worldwideblocks.port.PxBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Desktop switch (lot 1d-1): a flat slab of light plastic, 1 px high, its whole front taken by RJ45 ports
 * ({@code design/blockbench/desktop_switch_5.md}, {@code desktop_switch_8.md}). It multiplies the network sockets,
 * without configuration: the switching of the box, without its local point ({@link DeviceRole#SWITCH}). FACING is the
 * direction its front faces. Its shape is the body of its port layout, turned with the orientation.
 */
public class DesktopSwitchBlock extends NetworkDeviceBlock {
    private final Map<Direction, VoxelShape> shapes;

    /** @param layout the ports of the switch and its body, for a front facing north ({@code PortLayouts}) */
    public DesktopSwitchBlock(BlockBehaviour.Properties properties, PortLayout layout) {
        super(properties, DeviceRole.SWITCH, layout);
        PxBox body = layout.body().getFirst();
        this.shapes = Shapes.rotateHorizontal(Block.box(body.minX(), body.minY(), body.minZ(), body.maxX(), body.maxY(), body.maxZ()));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The ports face the player who places the block.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
