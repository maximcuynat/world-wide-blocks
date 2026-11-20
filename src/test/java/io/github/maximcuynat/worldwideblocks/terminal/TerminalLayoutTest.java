package io.github.maximcuynat.worldwideblocks.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** The grid of the terminal, with the sizes measured on the prototype (issue #30). */
class TerminalLayoutTest {

    @Test
    void theAutomaticSizeIsTheLargestThatGivesEightyColumns() {
        // 1920 x 1080 framebuffer, automatic GUI scale 4: a text area of about 426 GUI pixels, 1704 framebuffer pixels.
        assertEquals(3, TerminalLayout.automaticScale(1704), "18 px cells: 94 columns; 24 px would give 71");
        assertEquals(94, TerminalLayout.columns(1704, 3));
        // 854 x 480, GUI scale 2: about 367 GUI pixels, 734 framebuffer pixels.
        assertEquals(1, TerminalLayout.automaticScale(734), "12 px cells would give 61 columns");
        assertEquals(122, TerminalLayout.columns(734, 1));
        assertEquals(1, TerminalLayout.automaticScale(100), "too narrow for 80 columns: the smallest text");
        assertEquals(TerminalLayout.MAX_SCALE, TerminalLayout.automaticScale(100_000));
        assertEquals(80, TerminalLayout.columns(480, 1));
        assertEquals(1, TerminalLayout.automaticScale(480 * 2 - 1));
        assertEquals(2, TerminalLayout.automaticScale(480 * 2));
    }

    @Test
    void theSizeChosenByThePlayerWins() {
        assertEquals(5, TerminalLayout.scale(5, 734));
        assertEquals(1, TerminalLayout.scale(0, 734));
        assertEquals(TerminalLayout.MAX_SCALE, TerminalLayout.scale(99, 734));
        assertEquals(30, TerminalLayout.rows(1080, 3), "rows of 12 font pixels");
    }

    @Test
    void linesAreCutAtTheColumnMidWord() {
        assertEquals(List.of(0), TerminalLayout.rowStarts(0, 80), "an empty line is one row");
        assertEquals(List.of(0), TerminalLayout.rowStarts(80, 80));
        assertEquals(List.of(0, 80), TerminalLayout.rowStarts(91, 80), "the lo line of ip addr, 91 characters");
        assertEquals(List.of(0, 63, 126), TerminalLayout.rowStarts(127, 63));
    }

    @Test
    void theScreenStartsAtItsFirstLineAndScrollsUpToTheFirstRowKept() {
        assertEquals(0, TerminalLayout.firstRow(5, 0, 20, 0), "a new console starts at the top");
        assertEquals(10, TerminalLayout.firstRow(10, 10, 20, 0), "after Ctrl+L, the prompt is on the first row");
        assertEquals(30, TerminalLayout.firstRow(50, 10, 20, 0), "the rows below no longer fit: the last 20 are shown");
        assertEquals(25, TerminalLayout.firstRow(50, 10, 20, 5));
        assertEquals(0, TerminalLayout.firstRow(50, 10, 20, 1000), "never above the first row kept");
        assertEquals(30, TerminalLayout.maxScroll(50, 10, 20));
    }

    @Test
    void theFrameAndItsTitleFitInEveryCapturedSize() {
        // GUI sizes of the captures: 854 x 480 (scale 2), 1920 x 1080 (auto 4, then scale 2).
        for (int[] gui : new int[][] {{427, 240}, {480, 270}, {960, 540}}) {
            TerminalLayout.Frame frame = TerminalLayout.frame(gui[0], gui[1]);
            assertEquals(Optional.empty(), TerminalLayout.frameProblem(frame, gui[0], gui[1]), gui[0] + "x" + gui[1]);
            assertEquals(gui[0] - 20, frame.frame().right());
            assertEquals(20 + TerminalLayout.BORDER + 1, frame.title().top(), "the accent of É one pixel below the border");
        }
    }

    @Test
    void aTitleOverTheBorderOrAFrameOutOfTheWindowIsReported() {
        TerminalLayout.Frame good = TerminalLayout.frame(427, 240);
        // The title of the first captures: drawn 6 pixels below the top, its accents 3 pixels higher, over the border.
        TerminalLayout.Box oldTitle = new TerminalLayout.Box(28, 20 + 6 - TerminalLayout.ACCENT_ABOVE, 399, 20 + 6 + TerminalLayout.FONT_LINE);
        TerminalLayout.Frame old = new TerminalLayout.Frame(good.frame(), oldTitle, good.text());
        assertTrue(TerminalLayout.frameProblem(old, 427, 240).orElseThrow().startsWith("the title"));
        assertTrue(TerminalLayout.frameProblem(good, 400, 240).orElseThrow().startsWith("the frame"), "a GUI narrower than the frame");
        TerminalLayout.Frame tiny = TerminalLayout.frame(30, 30);
        assertTrue(TerminalLayout.frameProblem(tiny, 30, 30).orElseThrow().startsWith("the frame"), "never smaller than its border");
        TerminalLayout.Frame overlap = new TerminalLayout.Frame(good.frame(), good.title(),
                new TerminalLayout.Box(good.text().left(), good.title().bottom() - 1, good.text().right(), good.text().bottom()));
        assertTrue(TerminalLayout.frameProblem(overlap, 427, 240).orElseThrow().startsWith("the text area"));
    }
}
