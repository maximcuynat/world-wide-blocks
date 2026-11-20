package io.github.maximcuynat.worldwideblocks.capture;

import io.github.maximcuynat.worldwideblocks.block.LaptopBlock;
import io.github.maximcuynat.worldwideblocks.gametest.scene.TrialScenes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Where a capture run clicks to open the terminal of a computer of a trial scene. */
final class TerminalScenes {
    /** Height of the top of the keyboard of the open laptop ({@code LaptopPart}). */
    private static final double KEYBOARD_TOP = 1.0 / 16.0;
    /** From the centre of the block towards the user, on the keyboard and in front of the screen. */
    private static final double TOWARDS_USER = 3.5 / 16.0;

    private TerminalScenes() {
    }

    static void requireDevice(TrialScenes.Result scene, int device) {
        if (scene == null || device >= scene.devices().size()) {
            throw new IllegalStateException("no device " + device + " in the last scene");
        }
    }

    /**
     * The point to aim at: on the top of the keyboard of an open laptop (its side faces the user: {@code FACING}), the
     * middle of the block otherwise, which is the front of a tower.
     */
    static Vec3 clickPoint(BlockState state, BlockPos device) {
        if (state.getBlock() instanceof LaptopBlock && state.getValue(LaptopBlock.OPEN)) {
            Direction user = state.getValue(LaptopBlock.FACING);
            return new Vec3(device.getX() + 0.5 + user.getStepX() * TOWARDS_USER, device.getY() + KEYBOARD_TOP,
                    device.getZ() + 0.5 + user.getStepZ() * TOWARDS_USER);
        }
        return Vec3.atCenterOf(device);
    }
}
