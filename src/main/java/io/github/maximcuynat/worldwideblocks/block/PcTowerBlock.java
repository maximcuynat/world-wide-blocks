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
 * Tour PC : 8 px de large, 14 px de profondeur, 15 px de haut, centrée dans le bloc.
 * FACING désigne la direction vers laquelle regarde la face avant.
 */
public class PcTowerBlock extends NetworkDeviceBlock {
    /** Une carte réseau intégrée. */
    public static final List<PortKind> PORTS = List.of(PortKind.ETHERNET);

    // Forme pour une tour alignée sur l'axe Z (face avant au nord ou au sud), tournée pour l'axe X.
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.boxZ(8.0, 0.0, 15.0, 1.0, 15.0));

    public PcTowerBlock(BlockBehaviour.Properties properties) {
        super(properties, DeviceRole.HOST, PORTS);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // La face avant regarde le joueur qui pose le bloc.
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
