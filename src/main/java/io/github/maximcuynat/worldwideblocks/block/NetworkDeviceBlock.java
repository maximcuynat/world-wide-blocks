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
 * Appareil réseau orientable, aux ports fixes : s'enregistre dans le moteur réseau à la pose et en sort, avec ses
 * câbles, quand il quitte sa position. Il doit refuser les pistons ({@code PushReaction.IMMOVEABLE}),
 * car le moteur l'identifie par sa position.
 *
 * <p>Accroupi + clic droit main vide : on retire de l'appareil le bout de son câble et on le garde
 * en main, l'autre bout restant branché. Le clic droit simple est propre à chaque appareil
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

    /** Clic droit simple, main vide, sur l'appareil. Par défaut : rien. */
    protected InteractionResult useDevice(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Un simple changement d'état (écran du portable ouvert ou fermé) n'est pas une nouvelle pose.
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
