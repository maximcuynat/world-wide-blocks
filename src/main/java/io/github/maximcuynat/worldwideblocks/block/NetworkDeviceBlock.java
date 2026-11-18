package io.github.maximcuynat.worldwideblocks.block;

import java.util.List;

import io.github.maximcuynat.worldwideblocks.item.EthernetCableItem;
import io.github.maximcuynat.worldwideblocks.network.DeviceRole;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevice;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevices;
import io.github.maximcuynat.worldwideblocks.network.PortKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Orientable network device with fixed ports: registers itself in the network engine when placed and leaves it,
 * with its cables, when it leaves its position. It must refuse pistons ({@code PushReaction.IMMOVEABLE}),
 * because the engine identifies it by its position.
 *
 * <p>Sneaking + empty-hand right-click: the end of its cable is taken off the device and kept
 * in hand, the other end staying connected. A plain right-click is specific to each device
 * ({@link #useDevice}).
 */
public abstract class NetworkDeviceBlock extends HorizontalDirectionalBlock implements NetworkDevice {
    private final DeviceRole role;
    private final List<PortKind> ports;

    protected NetworkDeviceBlock(BlockBehaviour.Properties properties, DeviceRole role, List<PortKind> ports) {
        super(properties);
        this.role = role;
        this.ports = List.copyOf(ports);
    }

    @Override
    public DeviceRole role() {
        return this.role;
    }

    @Override
    public List<PortKind> ports() {
        return this.ports;
    }

    @Override
    protected final InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isSecondaryUseActive()) {
            if (level instanceof ServerLevel serverLevel) {
                EthernetCableItem.takeCableEnd(serverLevel, player, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return this.useDevice(state, level, pos, player, hitResult);
    }

    /** Plain empty-hand right-click on the device. By default: nothing. */
    protected InteractionResult useDevice(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // A simple state change (laptop screen opened or closed) is not a new placement.
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            NetworkDevices.dropCables(serverLevel, pos, NetworkDevices.onPlaced(serverLevel, pos));
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        NetworkDevices.dropCables(level, pos, NetworkDevices.onRemoved(level, pos));
    }
}
