package io.github.maximcuynat.worldwideblocks.client;

import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.block.DevicePorts;
import io.github.maximcuynat.worldwideblocks.item.EthernetCableItem;
import io.github.maximcuynat.worldwideblocks.port.PortLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The port the player aims at, as the server will resolve it at the click ({@link DevicePorts#aimed}, the same code):
 * only with a cord in hand, or sneaking with an empty hand (taking an end off), as {@code docs/cable-design.md}
 * section 3 says. Read each frame by the outline and the status panel.
 */
final class PortAim {
    /** The aimed port of a device, with the layout of the device turned by its {@code FACING}. */
    record Aimed(BlockPos pos, PortLayout layout, PortLayout.Port port) {
    }

    private PortAim() {
    }

    static Optional<Aimed> current(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        boolean cord = minecraft.player.getMainHandItem().getItem() instanceof EthernetCableItem;
        boolean takingEnd = minecraft.player.isShiftKeyDown() && minecraft.player.getMainHandItem().isEmpty();
        if (!cord && !takingEnd) {
            return Optional.empty();
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        return DevicePorts.layout(state).flatMap(layout -> layout.portAt(DevicePorts.side(hit.getDirection()), DevicePorts.toPixels(pos, hit.getLocation()))
                .map(port -> new Aimed(pos, layout, port)));
    }
}
