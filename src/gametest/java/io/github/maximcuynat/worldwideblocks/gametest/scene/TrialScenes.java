package io.github.maximcuynat.worldwideblocks.gametest.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.network.ConnectOutcome;
import io.github.maximcuynat.worldwideblocks.network.DeviceId;
import io.github.maximcuynat.worldwideblocks.network.LinkMedium;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevice;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevices;
import io.github.maximcuynat.worldwideblocks.network.NetworkManager;
import io.github.maximcuynat.worldwideblocks.network.PortRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Construction et retrait des décors d'essai. Les appareils sont de vrais blocs, posés comme par un
 * joueur ; les câbles passent par le moteur, sans cordon consommé (comme en créatif).
 */
public final class TrialScenes {
    /** Distance entre les pieds du joueur et le point de départ du décor. */
    public static final int DISTANCE_FROM_PLAYER = 3;

    private TrialScenes() {
    }

    /** Résultat d'une construction : les positions des appareils posés, ou la raison d'un refus. */
    public record Result(List<BlockPos> devices, int cables, Optional<String> refusal) {
        static Result refused(String reason) {
            return new Result(List.of(), 0, Optional.of(reason));
        }
    }

    /**
     * Position d'un appareil du décor.
     *
     * @param origin  point de départ du décor, au niveau des pieds
     * @param forward direction dans laquelle le joueur regarde (horizontale)
     */
    public static BlockPos positionOf(TrialScenario.Device device, BlockPos origin, Direction forward) {
        return origin.relative(forward.getClockWise(), device.right()).relative(forward, device.forward());
    }

    /**
     * Construit le décor si toute sa zone est vide (deux blocs de haut au-dessus du sol). Les
     * appareils regardent le joueur.
     */
    public static Result build(ServerLevel level, TrialScenario scenario, BlockPos origin, Direction forward) {
        Optional<BlockPos> obstacle = firstObstacle(level, scenario, origin, forward);
        if (obstacle.isPresent()) {
            BlockPos at = obstacle.get();
            return Result.refused("La zone du décor n'est pas vide : bloc « "
                    + level.getBlockState(at).getBlock().getName().getString() + " » en "
                    + at.getX() + " " + at.getY() + " " + at.getZ() + ". Placez-vous face à un terrain dégagé.");
        }
        List<BlockPos> placed = new ArrayList<>();
        for (TrialScenario.Device device : scenario.devices()) {
            BlockPos pos = positionOf(device, origin, forward);
            BlockState state = device.block().get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, forward.getOpposite());
            level.setBlockAndUpdate(pos, state);
            placed.add(pos);
        }
        NetworkManager network = NetworkManager.of(level.getServer());
        int cables = 0;
        for (TrialScenario.Cable cable : scenario.cables()) {
            DeviceId from = NetworkDevices.idOf(level, placed.get(cable.from()));
            DeviceId to = NetworkDevices.idOf(level, placed.get(cable.to()));
            Optional<PortRef> fromPort = network.firstFreePort(from, LinkMedium.COPPER_ETHERNET);
            Optional<PortRef> toPort = network.firstFreePort(to, LinkMedium.COPPER_ETHERNET);
            if (fromPort.isEmpty() || toPort.isEmpty()) {
                return new Result(placed, cables, Optional.of("Câble " + (cables + 1) + " impossible : plus de port libre."));
            }
            ConnectOutcome outcome = network.connect(fromPort.get(), toPort.get(), LinkMedium.COPPER_ETHERNET, false);
            if (!outcome.isSuccess()) {
                return new Result(placed, cables, Optional.of("Câble " + (cables + 1) + " refusé par le moteur : " + outcome.result()));
            }
            cables++;
        }
        return new Result(placed, cables, Optional.empty());
    }

    /** Retire les appareils encore présents à ces positions (leurs câbles partent avec eux). */
    public static int remove(ServerLevel level, List<BlockPos> devices) {
        int removed = 0;
        for (BlockPos pos : devices) {
            if (level.getBlockState(pos).getBlock() instanceof NetworkDevice) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                removed++;
            }
        }
        return removed;
    }

    private static Optional<BlockPos> firstObstacle(ServerLevel level, TrialScenario scenario, BlockPos origin, Direction forward) {
        int minRight = Integer.MAX_VALUE;
        int maxRight = Integer.MIN_VALUE;
        int maxForward = 0;
        for (TrialScenario.Device device : scenario.devices()) {
            minRight = Math.min(minRight, device.right());
            maxRight = Math.max(maxRight, device.right());
            maxForward = Math.max(maxForward, device.forward());
        }
        for (int right = minRight; right <= maxRight; right++) {
            for (int ahead = 0; ahead <= maxForward; ahead++) {
                BlockPos ground = origin.relative(forward.getClockWise(), right).relative(forward, ahead);
                for (int up = 0; up < 2; up++) {
                    if (!level.isEmptyBlock(ground.above(up))) {
                        return Optional.of(ground.above(up));
                    }
                }
            }
        }
        return Optional.empty();
    }
}
