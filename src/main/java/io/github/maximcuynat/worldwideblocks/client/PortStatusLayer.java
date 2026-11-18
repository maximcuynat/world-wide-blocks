package io.github.maximcuynat.worldwideblocks.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.maximcuynat.worldwideblocks.message.StatusPanel;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevice;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.sync.DeviceStatusPayload;
import io.github.maximcuynat.worldwideblocks.network.sync.LearnedMac;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * Status indicator: when aiming at a network device, a small panel at the top center of the screen
 * shows its name then the lines of {@link StatusPanel}: compact by default, detailed when the
 * player is sneaking. The content (keys, values, indicator lights) comes from {@link StatusPanel}, in plain Java;
 * this class only draws it, and a future HUD can replace it alone.
 *
 * <p>The connection comes from the {@link ClientLinkCache}; the addresses come from the
 * {@link ClientDeviceStatus}, sent by the server for the targeted device.
 *
 * <p>Known limitation: the panel takes the same place as the boss bars.
 */
final class PortStatusLayer implements GuiLayer {
    private static final int TOP = 4;
    private static final int PADDING = 4;
    private static final int LINE_GAP = 2;
    private static final int LIGHT_SIZE = 6;
    private static final int LIGHT_GAP = 4;

    private static final int COLOR_BACKGROUND = 0xC0101418;
    private static final int COLOR_BORDER = 0xFF2F6FD6;
    private static final int COLOR_NAME = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFFDDDDDD;
    private static final int COLOR_DETAIL = 0xFFA0A8B0;
    private static final int COLOR_CONNECTED = 0xFF55FF55;
    private static final int COLOR_FREE = 0xFF707070;
    private static final int COLOR_INACTIVE = 0xFF383838;

    private final ClientLinkCache cache;
    private final ClientDeviceStatus deviceStatus;

    PortStatusLayer(ClientLinkCache cache, ClientDeviceStatus deviceStatus) {
        this.cache = cache;
        this.deviceStatus = deviceStatus;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        HitResult hit = minecraft.hitResult;
        if (minecraft.level == null || minecraft.gui.screen() != null || !(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetworkDevice device)) {
            return;
        }

        Component name = state.getBlock().getName();
        boolean detailed = minecraft.player != null && minecraft.player.isShiftKeyDown();
        List<StatusPanel.Line> panel = StatusPanel.lines(device.ports(), port -> this.cache.isConnected(pos, port),
                toStatus(this.deviceStatus.at(pos)), detailed);
        List<Component> texts = panel.stream().map(line -> (Component) ClientText.of(line.key(), line.args())).toList();

        Font font = minecraft.font;
        int contentWidth = font.width(name);
        for (Component text : texts) {
            contentWidth = Math.max(contentWidth, LIGHT_SIZE + LIGHT_GAP + font.width(text));
        }
        int lineHeight = font.lineHeight + LINE_GAP;
        int width = contentWidth + 2 * PADDING;
        int height = PADDING + lineHeight + LINE_GAP + panel.size() * lineHeight + PADDING - LINE_GAP;
        int left = (graphics.guiWidth() - width) / 2;
        graphics.fill(left, TOP, left + width, TOP + height, COLOR_BACKGROUND);
        graphics.outline(left, TOP, width, height, COLOR_BORDER);

        int x = left + PADDING;
        int y = TOP + PADDING;
        graphics.text(font, name, x, y, COLOR_NAME);
        y += lineHeight + LINE_GAP;
        for (int i = 0; i < panel.size(); i++) {
            Integer light = color(panel.get(i).light());
            if (light != null) {
                int lightTop = y + (font.lineHeight - LIGHT_SIZE) / 2;
                graphics.fill(x, lightTop, x + LIGHT_SIZE, lightTop + LIGHT_SIZE, light);
            }
            graphics.text(font, texts.get(i), x + LIGHT_SIZE + LIGHT_GAP, y, light != null ? COLOR_TEXT : COLOR_DETAIL);
            y += lineHeight;
        }
    }

    /** Color of the indicator light; {@code null} for a detail line, without a light. */
    private static Integer color(StatusPanel.Light light) {
        return switch (light) {
            case CONNECTED -> COLOR_CONNECTED;
            case FREE -> COLOR_FREE;
            case INACTIVE -> COLOR_INACTIVE;
            case NONE -> null;
        };
    }

    /** The state received from the server, in the form read by {@link StatusPanel}; {@code null} if it has not arrived. */
    private static StatusPanel.DeviceStatus toStatus(DeviceStatusPayload status) {
        if (status == null) {
            return null;
        }
        List<MacAddress> portMacs = status.portMacs().stream().map(MacAddress::new).toList();
        Map<Integer, List<MacAddress>> learned = new HashMap<>();
        for (LearnedMac entry : status.learned()) {
            learned.computeIfAbsent(entry.port(), port -> new ArrayList<>()).add(new MacAddress(entry.mac()));
        }
        return new StatusPanel.DeviceStatus(status.switching(), portMacs, learned);
    }
}
