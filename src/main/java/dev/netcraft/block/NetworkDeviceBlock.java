package dev.netcraft.block;

import dev.netcraft.network.NetworkDevice;
import dev.netcraft.network.NetworkDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Appareil réseau orientable : s'enregistre dans le moteur réseau à la pose et en sort, avec ses
 * câbles, quand il quitte sa position. Il doit refuser les pistons ({@code PushReaction.BLOCK}),
 * car le moteur l'identifie par sa position.
 */
public abstract class NetworkDeviceBlock extends HorizontalDirectionalBlock implements NetworkDevice {
    private final int portCount;

    protected NetworkDeviceBlock(BlockBehaviour.Properties properties, int portCount) {
        super(properties);
        this.portCount = portCount;
    }

    @Override
    public int portCount() {
        return this.portCount;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Un simple changement d'état (écran du portable ouvert ou fermé) n'est pas une nouvelle pose.
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            NetworkDevices.onPlaced(serverLevel, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        NetworkDevices.onRemoved(level, pos);
    }
}
