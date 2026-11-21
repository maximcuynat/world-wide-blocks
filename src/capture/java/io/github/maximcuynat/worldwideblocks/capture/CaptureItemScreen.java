package io.github.maximcuynat.worldwideblocks.capture;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * An item as the inventory draws it, with its real {@code gui} display settings: once large, filling two thirds of the
 * height of the window, to read its pixels, and once at its real size in a slot beside it. On the grey of the inventory
 * slots (issue #68).
 */
final class CaptureItemScreen extends Screen {
    /** Grey of an inventory slot. */
    private static final int SLOT_GREY = 0xFF8B8B8B;
    /** Darker edge around the real-size slot. */
    private static final int SLOT_EDGE = 0xFF373737;

    private final ItemStack stack;

    CaptureItemScreen(ItemStack stack) {
        super(Component.empty());
        this.stack = stack;
    }

    ItemStack stack() {
        return this.stack;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(0, 0, this.width, this.height, SLOT_GREY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        float scale = this.height * 2f / 3f / 16f;
        float size = 16 * scale;
        float left = (this.width - size) / 2f - this.width / 10f;
        float top = (this.height - size) / 2f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(left, top);
        graphics.pose().scale(scale, scale);
        graphics.item(this.stack, 0, 0);
        graphics.pose().popMatrix();
        // The real size, in a slot to the right of the large item.
        int slotX = (int) (left + size + this.width / 20f);
        int slotY = (int) (top + size / 2f) - 9;
        graphics.fill(slotX - 1, slotY - 1, slotX + 19, slotY + 19, SLOT_EDGE);
        graphics.fill(slotX, slotY, slotX + 18, slotY + 18, SLOT_GREY);
        graphics.item(this.stack, slotX + 1, slotY + 1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
