package io.github.maximcuynat.worldwideblocks.block;

import java.util.List;

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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Internet box, generic placeholder model: a flat square case of 7 x 7 x 3 px, centered in the block
 * (183 x 183 x 80 mm in real life, scaled up by about 2.4 like the PC tower). The 4 local network ports are
 * on the front, the line input at the back. FACING is the direction the front faces.
 */
public class InternetBoxBlock extends NetworkDeviceBlock {
    /** Ports 1 to 4: local network. Port 5: line input, inactive until lot 3. */
    public static final List<PortKind> PORTS = List.of(
            PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE);

    // Square: the same shape in all four orientations.
    private static final VoxelShape SHAPE = Block.box(4.5, 0.0, 4.5, 11.5, 3.0, 11.5);

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
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
