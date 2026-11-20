package io.github.maximcuynat.worldwideblocks.block;

import java.util.List;
import java.util.Map;

import io.github.maximcuynat.worldwideblocks.network.DeviceRole;
import io.github.maximcuynat.worldwideblocks.network.PortKind;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
 * Laptop: 14 px wide, 9 px deep (360 x 240 mm in real life, scaled up by 2.4, docs/design-guide.md). FACING is the
 * side where the user sits (the front edge of the keyboard).
 * A right-click on the screen opens or closes the lid; on the keyboard of the open laptop, it opens the terminal
 * ({@link LaptopPart}, {@link TerminalServer}). The item keeps the state of the lid: the loot table copies a closed lid into the
 * block state component of the item ({@code copy_state}; an open laptop drops a plain item, which stacks with new ones
 * and is placed open by default), and the game places the block in that state.
 */
public class LaptopBlock extends NetworkDeviceBlock {
    /** One Ethernet port; Wi-Fi comes in lot 2a. */
    public static final List<PortKind> PORTS = List.of(PortKind.ETHERNET);

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    // Shapes for a laptop whose user is to the north: the screen stands on the south side.
    private static final VoxelShape CLOSED_NORTH = Block.box(1.0, 0.0, 3.5, 15.0, 2.0, 12.5);
    private static final VoxelShape OPEN_NORTH = Shapes.or(
            Block.box(1.0, 0.0, 3.5, 15.0, 1.0, 12.5),
            // The screen leans back by 22.5 degrees around its front bottom edge: a box around it, within one texel.
            Block.box(1.0, 1.0, 11.5, 15.0, 9.25, 16.0));
    private static final Map<Direction, VoxelShape> CLOSED_SHAPES = Shapes.rotateHorizontal(CLOSED_NORTH);
    private static final Map<Direction, VoxelShape> OPEN_SHAPES = Shapes.rotateHorizontal(OPEN_NORTH);

    public LaptopBlock(BlockBehaviour.Properties properties) {
        super(properties, DeviceRole.HOST, PORTS);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, true));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The keyboard faces the player who places the block.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useDevice(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        LaptopPart part = LaptopPart.at(state.getValue(OPEN), hitResult.getLocation().y - pos.getY());
        if (part == LaptopPart.KEYBOARD) {
            if (player instanceof ServerPlayer serverPlayer) {
                TerminalServer.open(serverPlayer, pos);
            }
        } else {
            level.setBlock(pos, state.cycle(OPEN), Block.UPDATE_CLIENTS);
        }
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
