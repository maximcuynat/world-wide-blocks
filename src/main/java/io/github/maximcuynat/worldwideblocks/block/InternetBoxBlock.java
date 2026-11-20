package io.github.maximcuynat.worldwideblocks.block;

import java.util.List;
import java.util.Map;

import io.github.maximcuynat.worldwideblocks.network.DeviceRole;
import io.github.maximcuynat.worldwideblocks.network.PortKind;
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
 * Internet box: a standing case of 7 x 3 x 7 px (183 x 80 x 183 mm in real life, scaled up by 2.4,
 * docs/design-guide.md), centered in the block. The 4 local network ports are on the front, the line
 * input at the back. FACING is the direction the front faces.
 */
public class InternetBoxBlock extends NetworkDeviceBlock {
    /** Ports 1 to 4: local network. Port 5: line input, inactive until lot 3. */
    public static final List<PortKind> PORTS = List.of(
            PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE);

    // Shape of a box whose front faces north, turned for the other orientations (as the blockstate turns the model).
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.box(4.5, 0.0, 6.5, 11.5, 7.0, 9.5));

    public InternetBoxBlock(BlockBehaviour.Properties properties) {
        super(properties, DeviceRole.BOX, PORTS);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The front faces the player who places the block.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
