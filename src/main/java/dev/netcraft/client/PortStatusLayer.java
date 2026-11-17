package dev.netcraft.client;

import java.util.ArrayList;
import java.util.List;

import dev.netcraft.network.NetworkDevice;
import dev.netcraft.network.PortKind;
import dev.netcraft.network.protocol.MacAddress;
import dev.netcraft.network.sync.DeviceStatusPayload;
import dev.netcraft.network.sync.LearnedMac;
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
 * Indicateur d'état : en visant un appareil réseau, un petit panneau en haut au centre de l'écran
 * affiche son nom puis, pour chaque port, un voyant et son état. Le branchement vient du
 * {@link ClientLinkCache} ; les adresses viennent du {@link ClientDeviceStatus}, envoyé par le serveur
 * pour l'appareil visé :
 * <ul>
 * <li>poste : la MAC de chaque port ;</li>
 * <li>box (appareil qui commute) : la MAC apprise derrière chaque port réseau local ;</li>
 * <li>arrivée de ligne : inactive jusqu'au lot 3.</li>
 * </ul>
 *
 * <p>Limite connue : le panneau occupe la même place que les barres de boss.
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

    /** Une ligne du panneau ; {@code light} vaut {@code null} pour une ligne de détail, sans voyant. */
    private record Line(Component text, Integer light) {
    }

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
        List<Line> lines = this.lines(device, pos, this.deviceStatus.at(pos));

        Font font = minecraft.font;
        int contentWidth = font.width(name);
        for (Line line : lines) {
            contentWidth = Math.max(contentWidth, LIGHT_SIZE + LIGHT_GAP + font.width(line.text()));
        }
        int lineHeight = font.lineHeight + LINE_GAP;
        int width = contentWidth + 2 * PADDING;
        int height = PADDING + lineHeight + LINE_GAP + lines.size() * lineHeight + PADDING - LINE_GAP;
        int left = (graphics.guiWidth() - width) / 2;
        graphics.fill(left, TOP, left + width, TOP + height, COLOR_BACKGROUND);
        graphics.outline(left, TOP, width, height, COLOR_BORDER);

        int x = left + PADDING;
        int y = TOP + PADDING;
        graphics.text(font, name, x, y, COLOR_NAME);
        y += lineHeight + LINE_GAP;
        for (Line line : lines) {
            if (line.light() != null) {
                int lightTop = y + (font.lineHeight - LIGHT_SIZE) / 2;
                graphics.fill(x, lightTop, x + LIGHT_SIZE, lightTop + LIGHT_SIZE, line.light());
            }
            graphics.text(font, line.text(), x + LIGHT_SIZE + LIGHT_GAP, y, line.light() != null ? COLOR_TEXT : COLOR_DETAIL);
            y += lineHeight;
        }
    }

    private List<Line> lines(NetworkDevice device, BlockPos pos, DeviceStatusPayload status) {
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < device.portCount(); i++) {
            int number = i + 1;
            if (device.ports().get(i) == PortKind.LINE) {
                lines.add(new Line(Component.translatable("hud.netcraft.port.line", number), COLOR_INACTIVE));
                continue;
            }
            boolean connected = this.cache.isConnected(pos, i);
            List<MacAddress> learned = status != null && status.switching() ? learnedOn(status, i) : List.of();
            Component text;
            if (connected && !learned.isEmpty()) {
                text = Component.translatable("hud.netcraft.port.connected_learned", number, describe(learned));
            } else {
                text = Component.translatable(connected ? "hud.netcraft.port.connected" : "hud.netcraft.port.free", number);
            }
            lines.add(new Line(text, connected ? COLOR_CONNECTED : COLOR_FREE));
            if (status != null && !status.switching() && i < status.portMacs().size()) {
                lines.add(new Line(Component.translatable("hud.netcraft.mac", new MacAddress(status.portMacs().get(i)).toString()), null));
            }
        }
        return lines;
    }

    private static List<MacAddress> learnedOn(DeviceStatusPayload status, int port) {
        List<MacAddress> macs = new ArrayList<>();
        for (LearnedMac learned : status.learned()) {
            if (learned.port() == port) {
                macs.add(new MacAddress(learned.mac()));
            }
        }
        return macs;
    }

    /** La première MAC apprise, suivie du nombre des autres (plusieurs appareils derrière un switch). */
    private static Component describe(List<MacAddress> macs) {
        String first = macs.get(0).toString();
        return macs.size() == 1
                ? Component.literal(first)
                : Component.translatable("hud.netcraft.learned_more", first, macs.size() - 1);
    }
}
