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
 * Box internet, modèle provisoire générique : boîtier plat et carré de 7 x 7 x 3 px, centré dans le bloc
 * (183 x 183 x 80 mm réels, agrandis d'environ 2,4 comme la tour PC). Les 4 prises réseau local sont
 * en façade, l'arrivée de ligne à l'arrière. FACING désigne la direction vers laquelle regarde la façade.
 */
public class InternetBoxBlock extends NetworkDeviceBlock {
    /** Ports 1 à 4 : réseau local. Port 5 : arrivée de ligne, inactive jusqu'au lot 3. */
    public static final List<PortKind> PORTS = List.of(
            PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.ETHERNET, PortKind.LINE);

    // Carré : la même forme dans les quatre orientations.
    private static final VoxelShape SHAPE = Block.box(4.5, 0.0, 4.5, 11.5, 3.0, 11.5);

    public InternetBoxBlock(BlockBehaviour.Properties properties) {
        super(properties, DeviceRole.BOX, PORTS);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // La façade regarde le joueur qui pose le bloc.
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
