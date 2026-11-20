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
 * PC tower: 7 px wide, 15 px deep, 15 px high (210 x 450 x 450 mm in real life, scaled up by 2.4 then reduced to
 * fit one block, docs/design-guide.md), centered in the block. Its network port is on an expansion card at the
 * back, low. FACING is the direction the front face looks toward.
 */
public class PcTowerBlock extends NetworkDeviceBlock {
    /** One network card. */
    public static final List<PortKind> PORTS = List.of(PortKind.ETHERNET);

    // Shape for a tower aligned on the Z axis (front face north or south), rotated for the X axis.
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.boxZ(7.0, 0.0, 15.0, 0.5, 15.5));

    public PcTowerBlock(BlockBehaviour.Properties properties) {
        super(properties, DeviceRole.HOST, PORTS);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The front face looks at the player who places the block.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING).getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
