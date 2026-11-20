package io.github.maximcuynat.worldwideblocks.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joml.Matrix3x2fStack;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.message.MessageLevel;
import io.github.maximcuynat.worldwideblocks.network.sync.TerminalInputPayload;
import io.github.maximcuynat.worldwideblocks.os.ConsoleLine;
import io.github.maximcuynat.worldwideblocks.os.shell.Prompt;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalLayout;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalRules;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.TextCursorUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The terminal of a computer ({@code docs/lot-plan.md}, lot 1c-1, "Appearance"): the console the server keeps
 * ({@link TerminalView}), drawn in Monocraft inside the dark popup panel of the game, with a white title bar; grey
 * text on black with the shadow of the game, the prompt in green and blue, the lines of the game in white; a
 * blinking block cursor; rows of 12 font pixels. Its text size is its own ({@link TerminalLayout}), independent of
 * the GUI scale: the text is drawn through a scale of the pose, never cut by a scissor (a scissor recorded before a
 * resize crashed the prototype).
 *
 * <p>Keys follow the layout of the player: the text comes only from the typed characters ({@code charTyped}),
 * dead keys through the preedit; the shortcuts are read from the symbol of the layout ({@code shortcutKey}), with the
 * real Ctrl key, also on macOS, and never with Alt (Option+c is {@code ç} on macOS). The line is edited here and sent
 * with Enter; the server checks it again. The game does not pause behind it in single player.
 */
public final class TerminalScreen extends Screen {
    private static final int BLACK = 0xFF000000;
    private static final int GREY = 0xFFAAAAAA;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREEN = 0xFF55FF55;
    private static final int BLUE = 0xFF5555FF;
    private static final int AQUA = 0xFF55FFFF;
    private static final int YELLOW = 0xFFFFFF55;
    private static final Identifier POPUP_SPRITE = Identifier.withDefaultNamespace("popup/background");
    private static final FontDescription MONOCRAFT =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "monocraft"));
    private static final int WHEEL_ROWS = 3;

    /** One row of the grid: a code point and a colour per cell. */
    private record Row(int[] codepoints, int[] colors) {
        String text() {
            return new String(this.codepoints, 0, this.codepoints.length);
        }
    }

    /**
     * The grid of the last frame, read by the capture runs ({@code src/capture}).
     *
     * @param scale         framebuffer pixels per pixel of the font
     * @param automatic     the size is the automatic one
     * @param visibleRows   the text of the rows shown, top first
     * @param guiWidth      width of the GUI the frame was drawn in, in GUI pixels
     * @param frame         where the frame, its title and its text area were drawn, in GUI pixels
     */
    public record Layout(int columns, int rows, int scale, boolean automatic, int scrollUp, List<String> visibleRows,
            int guiWidth, int guiHeight, TerminalLayout.Frame frame) {
    }

    private final ClientTerminal terminal;
    private final WorldWideBlocksClientConfig config;
    private final BlockPos device;
    private final StringBuilder input = new StringBuilder();
    /** Position of the cursor in {@link #input}, in chars. */
    private int cursor;
    private String preedit = "";
    private int historyIndex = -1;
    private int scrollUp;
    private long cursorBlinkStart;
    private boolean closedByServer;
    private int lastScale = 1;
    private int lastRows = 1;
    private Layout layout = new Layout(1, 1, 1, true, 0, List.of(), 0, 0, TerminalLayout.frame(0, 0));

    TerminalScreen(ClientTerminal terminal, WorldWideBlocksClientConfig config, BlockPos device) {
        super(Component.literal("Terminal"));
        this.terminal = terminal;
        this.config = config;
        this.device = device;
    }

    public BlockPos device() {
        return this.device;
    }

    /** The grid of the last frame drawn. */
    public Layout layout() {
        return this.layout;
    }

    // ---- Screen ----

    /** The game goes on behind the terminal in single player ({@code Screen.isPauseScreen} is true by default). */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Typed characters reach the screen only once SDL text input is started; again on resize, harmless. */
    @Override
    protected void init() {
        this.minecraft.textInputManager().startTextInput(this);
        this.cursorBlinkStart = Util.getMillis();
    }

    @Override
    public void removed() {
        this.minecraft.textInputManager().stopTextInput(this);
    }

    /** Blocks the global keys of the game (friends list) while the terminal has the keyboard. */
    @Override
    public boolean isInputCaptured() {
        return true;
    }

    /** Esc: the screen is hidden, the console and its command go on; the keyboard is free again. */
    @Override
    public void onClose() {
        if (!this.closedByServer) {
            this.send(TerminalInputPayload.Kind.CLOSE, "");
        }
        super.onClose();
    }

    /** The server freed the keyboard (too far, lid closed, shell ended...): closed without telling it again. */
    void closeFromServer() {
        this.closedByServer = true;
        this.onClose();
    }

    /** No blur nor darkening: the world stays visible around the terminal. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    }

    // ---- Keys ----

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.handleKey(event)) {
            this.cursorBlinkStart = Util.getMillis();
            return true;
        }
        int key = event.key();
        // Esc closes (Screen.keyPressed); F1 to F12 go on to the game (screenshot, debug, full screen).
        if (event.isEscape() || key >= InputConstants.KEY_F1 && key <= InputConstants.KEY_F12) {
            return super.keyPressed(event);
        }
        return true;
    }

    private boolean handleKey(KeyEvent event) {
        TerminalView view = this.view();
        if (view == null) {
            return false;
        }
        boolean running = view.running();
        // The real Ctrl, also on macOS; without Alt, which is AltGr on Windows.
        if (event.hasControlDown() && !event.hasAltDown()) {
            switch (event.shortcutKey()) {
                case 'c' -> {
                    this.send(TerminalInputPayload.Kind.INTERRUPT, running ? "" : this.input.toString());
                    this.resetInput();
                    return true;
                }
                case 'd' -> {
                    if (!running && this.input.isEmpty()) {
                        this.send(TerminalInputPayload.Kind.END_OF_INPUT, "");
                    }
                    return true; // with text, readline only rings the bell
                }
                case 'l' -> {
                    if (!running) {
                        this.send(TerminalInputPayload.Kind.CLEAR, "");
                        this.scrollUp = 0;
                    }
                    return true;
                }
                case '=', '+' -> {
                    this.config.setTerminalTextSize(Math.min(TerminalLayout.MAX_SCALE, this.lastScale + 1));
                    return true;
                }
                case '-' -> {
                    this.config.setTerminalTextSize(Math.max(1, this.lastScale - 1));
                    return true;
                }
                case '0' -> {
                    this.config.setTerminalTextSize(0);
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }
        switch (event.key()) {
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                if (!running) {
                    this.send(TerminalInputPayload.Kind.LINE, this.input.toString());
                    this.resetInput();
                }
                return true;
            }
            case InputConstants.KEY_BACKSPACE -> {
                if (this.cursor > 0) {
                    int from = this.input.offsetByCodePoints(this.cursor, -1);
                    this.input.delete(from, this.cursor);
                    this.cursor = from;
                }
                return true;
            }
            case InputConstants.KEY_DELETE -> {
                if (this.cursor < this.input.length()) {
                    this.input.delete(this.cursor, this.input.offsetByCodePoints(this.cursor, 1));
                }
                return true;
            }
            case InputConstants.KEY_LEFT -> {
                this.cursor = this.cursor > 0 ? this.input.offsetByCodePoints(this.cursor, -1) : 0;
                return true;
            }
            case InputConstants.KEY_RIGHT -> {
                this.cursor = this.cursor < this.input.length() ? this.input.offsetByCodePoints(this.cursor, 1) : this.cursor;
                return true;
            }
            case InputConstants.KEY_HOME -> {
                this.cursor = 0;
                return true;
            }
            case InputConstants.KEY_END -> {
                this.cursor = this.input.length();
                return true;
            }
            case InputConstants.KEY_UP -> {
                List<String> history = view.history();
                if (!running && !history.isEmpty()) {
                    this.historyIndex = this.historyIndex < 0 ? history.size() - 1 : Math.max(0, this.historyIndex - 1);
                    this.setInput(history.get(this.historyIndex));
                }
                return true;
            }
            case InputConstants.KEY_DOWN -> {
                List<String> history = view.history();
                if (!running && this.historyIndex >= 0) {
                    this.historyIndex++;
                    if (this.historyIndex < history.size()) {
                        this.setInput(history.get(this.historyIndex));
                    } else {
                        this.historyIndex = -1;
                        this.setInput("");
                    }
                }
                return true;
            }
            case InputConstants.KEY_PAGEUP -> {
                if (event.hasShiftDown()) {
                    this.scrollUp += Math.max(1, this.lastRows - 1);
                    return true;
                }
                return false;
            }
            case InputConstants.KEY_PAGEDOWN -> {
                if (event.hasShiftDown()) {
                    this.scrollUp = Math.max(0, this.scrollUp - Math.max(1, this.lastRows - 1));
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /** Any typed character is taken, as it comes from the layout of the player; the server checks it again. */
    @Override
    public boolean charTyped(CharacterEvent event) {
        TerminalView view = this.view();
        if (view == null || view.running() || !event.isAllowedChatCharacter()) {
            return true; // no type-ahead while a command runs (lot 1c-1)
        }
        String text = event.codepointAsString();
        if (this.input.length() + text.length() <= TerminalRules.MAX_LINE) {
            this.input.insert(this.cursor, text);
            this.cursor += text.length();
            this.historyIndex = -1;
            this.scrollUp = 0;
            this.cursorBlinkStart = Util.getMillis();
        }
        return true;
    }

    /** Dead keys and input methods: the text being composed, shown in aqua before the final character arrives. */
    @Override
    public boolean preeditUpdated(PreeditEvent event) {
        this.preedit = event == null ? "" : event.fullText();
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY > 0) {
            this.scrollUp += WHEEL_ROWS;
        } else if (scrollY < 0) {
            this.scrollUp = Math.max(0, this.scrollUp - WHEEL_ROWS);
        }
        return true;
    }

    // ---- Drawing ----

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        TerminalView view = this.view();
        if (view == null) {
            return;
        }
        TerminalLayout.Frame frame = TerminalLayout.frame(this.width, this.height);
        TerminalLayout.Box panel = frame.frame();
        TerminalLayout.Box text = frame.text();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, POPUP_SPRITE, panel.left(), panel.top(),
                panel.right() - panel.left(), panel.bottom() - panel.top());
        graphics.fill(text.left(), text.top(), text.right(), text.bottom(), BLACK);
        this.drawTitle(graphics, view, frame.title());

        // The grid, in framebuffer pixels: its own text size, independent of the GUI scale.
        Window window = this.minecraft.getWindow();
        double guiScale = window.getGuiScale();
        int areaWidth = text.right() - text.left() - 2 * TerminalLayout.PAD;
        int areaHeight = text.bottom() - text.top() - 2 * TerminalLayout.PAD;
        int chosen = this.config.terminalTextSize();
        int scale = TerminalLayout.scale(chosen, (int) Math.round(areaWidth * guiScale));
        int columns = TerminalLayout.columns((int) Math.round(areaWidth * guiScale), scale);
        int rows = TerminalLayout.rows((int) Math.round(areaHeight * guiScale), scale);
        this.lastScale = scale;
        this.lastRows = rows;

        List<Row> display = new ArrayList<>();
        List<ConsoleLine> lines = view.lines();
        int screenStartRow = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (i == view.screenStart()) {
                screenStartRow = display.size();
            }
            Row line = this.line(lines.get(i));
            if (line != null) {
                wrap(line, columns, display);
            }
        }
        if (view.screenStart() >= lines.size()) {
            screenStartRow = display.size();
        }
        int cursorRow;
        int cursorColumn;
        if (view.running()) {
            Row current = row(view.current(), GREY);
            int start = display.size();
            wrap(current, columns, display);
            cursorRow = start + current.codepoints().length / columns;
            cursorColumn = current.codepoints().length % columns;
        } else {
            // The text being composed (dead key) is shown at the cursor, in aqua, until its character arrives.
            Prompt prompt = view.prompt();
            Row before = this.promptRow(prompt, this.input.substring(0, this.cursor));
            Row whole = concat(before, row(this.preedit, AQUA), row(this.input.substring(this.cursor), GREY));
            int start = display.size();
            wrap(whole, columns, display);
            int at = before.codepoints().length + this.preedit.codePointCount(0, this.preedit.length());
            cursorRow = start + at / columns;
            cursorColumn = at % columns;
        }
        while (display.size() <= cursorRow) {
            display.add(row("", GREY));
        }

        this.scrollUp = Math.min(this.scrollUp, TerminalLayout.maxScroll(display.size(), screenStartRow, rows));
        int first = TerminalLayout.firstRow(display.size(), screenStartRow, rows, this.scrollUp);
        int last = Math.min(display.size(), first + rows);
        List<String> visible = new ArrayList<>(last - first);

        float factor = (float) (scale / guiScale);
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(text.left() + TerminalLayout.PAD, text.top() + TerminalLayout.PAD);
        pose.scale(factor, factor);
        int lineHeight = this.font.lineHeight;
        boolean blink = TextCursorUtils.isCursorVisible(Util.getMillis() - this.cursorBlinkStart);
        for (int r = first; r < last; r++) {
            Row row = display.get(r);
            visible.add(row.text());
            int y = (r - first) * TerminalLayout.ROW;
            int glyphY = y + TerminalLayout.ROW - (lineHeight + 1); // the extra height goes above: room for accents
            for (int c = 0; c < row.codepoints().length && c < columns; c++) {
                int codepoint = row.codepoints()[c];
                if (codepoint != ' ') {
                    graphics.text(this.font, Component.literal(Character.toString(codepoint)).withStyle(style -> style.withFont(MONOCRAFT)),
                            c * TerminalLayout.CELL, glyphY, row.colors()[c], true);
                }
            }
            if (r == cursorRow && this.scrollUp == 0 && blink) {
                int x = cursorColumn * TerminalLayout.CELL;
                graphics.fill(x, glyphY - 1, x + TerminalLayout.CELL, glyphY + lineHeight - 1, GREY);
            }
        }
        pose.popMatrix();
        this.layout = new Layout(columns, rows, scale, chosen <= 0, this.scrollUp, List.copyOf(visible), this.width, this.height, frame);
    }

    /**
     * Name of the machine on the left; on the right, how to close, and how to stop while a command runs. Both stay
     * inside {@code title}, which leaves room above them for the accents of the capitals.
     */
    private void drawTitle(GuiGraphicsExtractor graphics, TerminalView view, TerminalLayout.Box title) {
        String hints = (view.running() ? Component.translatable("terminal.worldwideblocks.title.stop").getString() + "   " : "")
                + Component.translatable("terminal.worldwideblocks.title.close").getString();
        int y = title.top() + TerminalLayout.ACCENT_ABOVE;
        int hintsLeft = Math.max(title.left(), title.right() - this.font.width(hints));
        String name = view.prompt().host();
        graphics.text(this.font, this.font.plainSubstrByWidth(name, Math.max(0, hintsLeft - 8 - title.left())), title.left(), y, WHITE, true);
        graphics.text(this.font, this.font.plainSubstrByWidth(hints, Math.max(0, title.right() - hintsLeft)), hintsLeft, y, WHITE, true);
    }

    /** A line of the console as cells; null for an advice the player turned off. */
    private Row line(ConsoleLine line) {
        return switch (line) {
            case ConsoleLine.Output output -> row(output.text(), GREY);
            case ConsoleLine.Input input -> this.promptRow(input.prompt(), input.text());
            case ConsoleLine.Game game -> row(Component.translatable(game.key(), game.values().toArray()).getString(), WHITE);
            case ConsoleLine.Advice advice -> this.config.terminalAdvice()
                    ? row(ClientMessages.withLevel(this.config.messageSymbols(), MessageLevel.INFO,
                            Component.translatable(advice.key(), advice.values().toArray())).getString(), YELLOW)
                    : null;
        };
    }

    /** {@code user@host} in green, {@code :} in grey, the directory in blue, {@code $ } and the text in grey. */
    private Row promptRow(Prompt prompt, String text) {
        return concat(row(prompt.user() + "@" + prompt.host(), GREEN), row(":", GREY), row(prompt.directory(), BLUE),
                row("$ " + text, GREY));
    }

    private static Row row(String text, int color) {
        int[] codepoints = text.codePoints().toArray();
        int[] colors = new int[codepoints.length];
        Arrays.fill(colors, color);
        return new Row(codepoints, colors);
    }

    private static Row concat(Row... parts) {
        int length = 0;
        for (Row part : parts) {
            length += part.codepoints().length;
        }
        int[] codepoints = new int[length];
        int[] colors = new int[length];
        int at = 0;
        for (Row part : parts) {
            System.arraycopy(part.codepoints(), 0, codepoints, at, part.codepoints().length);
            System.arraycopy(part.colors(), 0, colors, at, part.colors().length);
            at += part.codepoints().length;
        }
        return new Row(codepoints, colors);
    }

    private static void wrap(Row line, int columns, List<Row> out) {
        int length = line.codepoints().length;
        for (int from : TerminalLayout.rowStarts(length, columns)) {
            int to = Math.min(length, from + columns);
            out.add(new Row(Arrays.copyOfRange(line.codepoints(), from, to), Arrays.copyOfRange(line.colors(), from, to)));
        }
    }

    // ---- Helpers ----

    private TerminalView view() {
        return this.terminal.view().filter(view -> this.terminal.device().filter(this.device::equals).isPresent()).orElse(null);
    }

    private void send(TerminalInputPayload.Kind kind, String text) {
        TerminalView view = this.view();
        if (view != null) {
            ClientPacketDistributor.sendToServer(new TerminalInputPayload(this.device, view.session(), kind, text));
        }
    }

    private void setInput(String text) {
        this.input.setLength(0);
        this.input.append(text);
        this.cursor = this.input.length();
    }

    private void resetInput() {
        this.setInput("");
        this.historyIndex = -1;
        this.scrollUp = 0;
    }
}
