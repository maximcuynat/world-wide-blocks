package io.github.maximcuynat.worldwideblocks.block;

/**
 * The part of the laptop a player clicks ({@code docs/lot-plan.md}, lot 1c-1, "Gestures"): on the open laptop, the
 * screen stands above the keyboard, which is 1 pixel high ({@link LaptopBlock}); closed, the lid covers everything.
 * Plain Java.
 */
public enum LaptopPart {
    /** Opens or closes the lid. */
    SCREEN,
    /** Opens the terminal. */
    KEYBOARD;

    /** Height of the keyboard of the open laptop, in blocks. */
    static final double KEYBOARD_TOP = 1.0 / 16.0;

    /**
     * @param open           the lid is open
     * @param heightInBlock  height of the point clicked above the bottom of the block, in blocks
     */
    public static LaptopPart at(boolean open, double heightInBlock) {
        return open && heightInBlock <= KEYBOARD_TOP + 1e-6 ? KEYBOARD : SCREEN;
    }
}
