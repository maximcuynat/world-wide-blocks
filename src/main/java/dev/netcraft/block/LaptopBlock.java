package dev.netcraft.block;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * PC portable : 14 px de large, 10 px de profondeur. Un clic droit ouvre ou ferme l'écran.
 * FACING désigne le côté où se tient l'utilisateur (le bord avant du clavier).
 */
public class LaptopBlock extends NetworkDeviceBlock {
    /** Une prise Ethernet ; le Wi-Fi arrive au lot 2a. */
    public static final int PORT_COUNT = 1;

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    // Formes pour un portable dont l'utilisateur est au nord : l'écran se dresse côté sud.
    private static final VoxelShape CLOSED_NORTH = Block.box(1.0, 0.0, 3.0, 15.0, 2.0, 13.0);
    private static final VoxelShape OPEN_NORTH = Shapes.or(
            Block.box(1.0, 0.0, 3.0, 15.0, 1.0, 13.0),
            Block.box(1.0, 1.0, 12.0, 15.0, 9.5, 16.0));
    private static final Map<Direction, VoxelShape> CLOSED_SHAPES = Shapes.rotateHorizontal(CLOSED_NORTH);
    private static final Map<Direction, VoxelShape> OPEN_SHAPES = Shapes.rotateHorizontal(OPEN_NORTH);

    public LaptopBlock(BlockBehaviour.Properties properties) {
        super(properties, PORT_COUNT);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, true));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Le clavier fait face au joueur qui pose le bloc.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        level.setBlock(pos, state.cycle(OPEN), Block.UPDATE_CLIENTS);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(OPEN) ? OPEN_SHAPES : CLOSED_SHAPES).get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }
}
