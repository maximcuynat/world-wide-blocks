package io.github.maximcuynat.worldwideblocks.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The character grid of a terminal screen ({@code docs/lot-plan.md}, lot 1c-1, "Appearance"): its own text size,
 * independent of the GUI scale of the game, counted in framebuffer pixels per pixel of the font; columns and rows
 * from the size of the text area; lines wrapped at the column, mid-word, as a terminal emulator does; the rows shown
 * from the first line on the screen, or higher when the player scrolls up. Plain Java: the screen only draws.
 */
public final class TerminalLayout {
    /** Width of a cell in pixels of the font: Monocraft at size 9, as the game font (prototype, issue #30). */
    public static final int CELL = 6;
    /** Height of a row in pixels of the font: 12, room for the accents of capitals. */
    public static final int ROW = 12;
    /** The automatic text size is the largest that gives at least this many columns. */
    public static final int MIN_COLUMNS = 80;
    /** Largest text size, in framebuffer pixels per pixel of the font. */
    public static final int MAX_SCALE = 8;

    // ---- The frame, in GUI pixels ----

    /** Space between the frame and the edges of the window. */
    public static final int MARGIN = 20;
    /** Border of the popup panel of the game: the nine-slice border of {@code popup/background.png.mcmeta}. */
    public static final int BORDER = 6;
    /**
     * How far the accented capitals of the game font (É) reach above the top of the other glyphs: {@code accented.png}
     * has an ascent of 10, {@code ascii.png} of 7.
     */
    public static final int ACCENT_ABOVE = 3;
    /** Height of a line of the game font, shadow included. */
    public static final int FONT_LINE = 9;
    /** Top of the title text below the top of the frame: the border, the accents, then one pixel. */
    public static final int TITLE_TOP = BORDER + ACCENT_ABOVE + 1;
    /** Space between the sides of the frame and the title text: the border, then two pixels. */
    public static final int TITLE_SIDE = BORDER + 2;
    /** Top of the black text area below the top of the frame: below the title and its shadow, then two pixels. */
    public static final int TEXT_TOP = TITLE_TOP + FONT_LINE + 2;
    /** Sides and bottom of the black text area: the border, then one pixel. */
    public static final int TEXT_SIDE = BORDER + 1;
    /** Space between the black text area and the grid of characters. */
    public static final int PAD = 3;

    private TerminalLayout() {
    }

    /** A rectangle in GUI pixels, right and bottom excluded. */
    public record Box(int left, int top, int right, int bottom) {
        public boolean contains(Box other) {
            return other.left >= this.left && other.top >= this.top && other.right <= this.right && other.bottom <= this.bottom;
        }

        @Override
        public String toString() {
            return "x " + this.left + ".." + this.right + " y " + this.top + ".." + this.bottom;
        }
    }

    /**
     * Where the screen draws: the frame (popup panel), the title text with its accents and shadow, the black text
     * area.
     */
    public record Frame(Box frame, Box title, Box text) {
        /** The frame inside its border. */
        public Box inside() {
            return new Box(this.frame.left + BORDER, this.frame.top + BORDER, this.frame.right - BORDER, this.frame.bottom - BORDER);
        }
    }

    /** The frame of a GUI of this size: {@link #MARGIN} from each edge, never smaller than its border and title. */
    public static Frame frame(int guiWidth, int guiHeight) {
        int left = MARGIN;
        int top = MARGIN;
        int right = Math.max(left + 2 * TEXT_SIDE + 2 * PAD + 1, guiWidth - MARGIN);
        int bottom = Math.max(top + TEXT_TOP + TEXT_SIDE + 2 * PAD + 1, guiHeight - MARGIN);
        Box title = new Box(left + TITLE_SIDE, top + TITLE_TOP - ACCENT_ABOVE, right - TITLE_SIDE, top + TITLE_TOP + FONT_LINE);
        Box text = new Box(left + TEXT_SIDE, top + TEXT_TOP, right - TEXT_SIDE, bottom - TEXT_SIDE);
        return new Frame(new Box(left, top, right, bottom), title, text);
    }

    /**
     * What is wrong with a frame drawn in a GUI of this size, or empty: the frame leaves the window, the title (accents
     * included) crosses the border, the text area crosses the border or the title. Checked by the capture runs.
     */
    public static Optional<String> frameProblem(Frame frame, int guiWidth, int guiHeight) {
        Box window = new Box(0, 0, guiWidth, guiHeight);
        if (!window.contains(frame.frame())) {
            return Optional.of("the frame (" + frame.frame() + ") leaves the GUI of " + guiWidth + "x" + guiHeight);
        }
        if (!frame.inside().contains(frame.title())) {
            return Optional.of("the title (" + frame.title() + ", accents included) crosses the border of the frame ("
                    + frame.inside() + " inside it)");
        }
        if (!frame.inside().contains(frame.text())) {
            return Optional.of("the text area (" + frame.text() + ") crosses the border of the frame (" + frame.inside() + " inside it)");
        }
        if (frame.text().top() < frame.title().bottom()) {
            return Optional.of("the text area (" + frame.text() + ") overlaps the title (" + frame.title() + ")");
        }
        return Optional.empty();
    }

    /** The largest text size that gives at least {@link #MIN_COLUMNS} columns in this width, 1 if none does. */
    public static int automaticScale(int widthPixels) {
        for (int scale = MAX_SCALE; scale > 1; scale--) {
            if (columns(widthPixels, scale) >= MIN_COLUMNS) {
                return scale;
            }
        }
        return 1;
    }

    /** The text size used: the one chosen by the player (1 to {@link #MAX_SCALE}), or the automatic one (0). */
    public static int scale(int chosen, int widthPixels) {
        return chosen <= 0 ? automaticScale(widthPixels) : Math.min(MAX_SCALE, chosen);
    }

    public static int columns(int widthPixels, int scale) {
        return Math.max(1, widthPixels / (CELL * scale));
    }

    public static int rows(int heightPixels, int scale) {
        return Math.max(1, heightPixels / (ROW * scale));
    }

    /**
     * Where a line of {@code length} cells is cut into rows of {@code columns}: the start of each row. An empty line
     * is one empty row.
     */
    public static List<Integer> rowStarts(int length, int columns) {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int from = columns; from < length; from += columns) {
            starts.add(from);
        }
        return starts;
    }

    /**
     * The first row shown. Without scrolling, the screen starts at the first row of the first line on the screen
     * ({@code screenStartRow}), or lower when the rows below it no longer fit; scrolling up shows rows above, down to
     * the first one kept.
     *
     * @param scrollUp rows scrolled up by the player, clamped here
     */
    public static int firstRow(int totalRows, int screenStartRow, int rows, int scrollUp) {
        int bottom = Math.max(screenStartRow, totalRows - rows);
        return Math.max(0, bottom - Math.max(0, scrollUp));
    }

    /** The largest useful scroll up: back to the first row kept. */
    public static int maxScroll(int totalRows, int screenStartRow, int rows) {
        return Math.max(screenStartRow, totalRows - rows);
    }
}
