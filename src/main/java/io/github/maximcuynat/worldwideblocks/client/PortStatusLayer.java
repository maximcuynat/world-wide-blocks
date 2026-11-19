package io.github.maximcuynat.worldwideblocks.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.maximcuynat.worldwideblocks.message.StatusPanel;
import io.github.maximcuynat.worldwideblocks.network.NetworkDevice;
import io.github.maximcuynat.worldwideblocks.network.dhcp.DhcpClientState;
import io.github.maximcuynat.worldwideblocks.network.protocol.Ipv4Address;
import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;
import io.github.maximcuynat.worldwideblocks.network.sync.DeviceStatusPayload;
import io.github.maximcuynat.worldwideblocks.network.sync.DhcpPortStatus;
import io.github.maximcuynat.worldwideblocks.network.sync.LearnedMac;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.common.NeoForge;

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
 *
 * <p>Each change of what it draws is posted as a {@link StatusPanelShownEvent} (capture runs).
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
    private static final int COLOR_WAITING = 0xFFFFC040;
    private static final int COLOR_FREE = 0xFF707070;
    private static final int COLOR_INACTIVE = 0xFF383838;

    private final ClientLinkCache cache;
    private final ClientDeviceStatus deviceStatus;
    /**
     * What was drawn last (device, block, lines, language), to post a {@link StatusPanelShownEvent} only when it changes:
     * nothing is built for the event on the other frames. Null before the first frame.
     */
    private List<Object> lastShown;

    PortStatusLayer(ClientLinkCache cache, ClientDeviceStatus deviceStatus) {
        this.cache = cache;
        this.deviceStatus = deviceStatus;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        HitResult hit = minecraft.hitResult;
        if (minecraft.level == null || minecraft.gui.screen() != null || !(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            this.hidden();
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetworkDevice device)) {
            this.hidden();
            return;
        }

        Component name = state.getBlock().getName();
        boolean detailed = minecraft.player != null && minecraft.player.isShiftKeyDown();
        // Same game time as the server's, which the client receives and advances: the time left on a lease.
        List<StatusPanel.Line> panel = StatusPanel.lines(device.ports(), port -> this.cache.isConnected(pos, port),
                toStatus(this.deviceStatus.at(pos)), detailed, minecraft.level.getGameTime());
        List<Component> texts = panel.stream().map(line -> (Component) ClientText.of(line.key(), line.args())).toList();
        // The loaded language, not the code chosen: the code changes before the texts are reloaded.
        List<Object> drawn = List.of(pos, state.getBlock(), panel, Language.getInstance());
        if (!drawn.equals(this.lastShown)) {
            this.lastShown = drawn;
            List<String> shownLines = new ArrayList<>(texts.size());
            for (int i = 0; i < texts.size(); i++) {
                StatusPanel.Light light = panel.get(i).light();
                shownLines.add((light == StatusPanel.Light.NONE ? "" : "[" + light.name().toLowerCase(java.util.Locale.ROOT) + "] ")
                        + texts.get(i).getString());
            }
            NeoForge.EVENT_BUS.post(new StatusPanelShownEvent(Optional.of(pos), name.getString(), shownLines));
        }

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

    /** No panel this frame: posted once when a panel was drawn before. */
    private void hidden() {
        if (this.lastShown == null || !this.lastShown.isEmpty()) {
            this.lastShown = List.of();
            NeoForge.EVENT_BUS.post(new StatusPanelShownEvent(Optional.empty(), "", List.of()));
        }
    }

    /** Color of the indicator light; {@code null} for a detail line, without a light. */
    private static Integer color(StatusPanel.Light light) {
        return switch (light) {
            case CONNECTED -> COLOR_CONNECTED;
            case WAITING -> COLOR_WAITING;
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
        Map<Integer, StatusPanel.PortAddress> dhcp = new HashMap<>();
        DhcpClientState[] states = DhcpClientState.values();
        for (DhcpPortStatus port : status.dhcp()) {
            if (port.state() < 0 || port.state() >= states.length) {
                continue;
            }
            Optional<StatusPanel.Address> address = port.address() == 0 ? Optional.empty()
                    : Optional.of(new StatusPanel.Address(new Ipv4Address(port.address()), new Ipv4Address(port.subnetMask()),
                            new Ipv4Address(port.router()), new Ipv4Address(port.dnsServer()), port.expiresAt()));
            dhcp.put(port.port(), new StatusPanel.PortAddress(states[port.state()], address));
        }
        Optional<StatusPanel.ServerStatus> server = status.dhcpServer().map(enabled -> new StatusPanel.ServerStatus(enabled,
                status.leases().stream()
                        .map(lease -> new StatusPanel.Lent(new Ipv4Address(lease.address()), new MacAddress(lease.client())))
                        .toList()));
        return new StatusPanel.DeviceStatus(status.switching(), portMacs, learned, dhcp, server);
    }
}
