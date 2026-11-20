package io.github.maximcuynat.worldwideblocks.capture;

import java.util.Optional;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/**
 * The inventory screen of the game, unchanged, with read access to what a capture run needs: where a slot is drawn and
 * which slot the mouse is over ({@code leftPos}, {@code topPos} and {@code hoveredSlot} are protected in 26.3).
 */
final class CaptureInventoryScreen extends InventoryScreen {

    CaptureInventoryScreen(Player player) {
        super(player);
    }

    /** Centre of a slot of the menu, in GUI coordinates. */
    double slotCentreX(Slot slot) {
        return this.leftPos + slot.x + 8;
    }

    double slotCentreY(Slot slot) {
        return this.topPos + slot.y + 8;
    }

    /** The slot under the mouse, as the screen last computed it. */
    Optional<Slot> hovered() {
        return Optional.ofNullable(this.hoveredSlot);
    }
}
