package io.github.maximcuynat.worldwideblocks.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LaptopPartTest {

    @Test
    void theKeyboardOfTheOpenLaptopOpensTheTerminalAndTheRestMovesTheLid() {
        assertEquals(LaptopPart.KEYBOARD, LaptopPart.at(true, 1.0 / 16.0), "top of the keyboard");
        assertEquals(LaptopPart.KEYBOARD, LaptopPart.at(true, 0.5 / 16.0), "front edge of the keyboard");
        assertEquals(LaptopPart.SCREEN, LaptopPart.at(true, 1.5 / 16.0), "the screen stands above the keyboard");
        assertEquals(LaptopPart.SCREEN, LaptopPart.at(true, 9.5 / 16.0));
        assertEquals(LaptopPart.SCREEN, LaptopPart.at(false, 1.0 / 16.0), "closed: the lid covers the keyboard");
        assertEquals(LaptopPart.SCREEN, LaptopPart.at(false, 0.0));
    }
}
