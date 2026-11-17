package dev.netcraft.client;

import java.util.ArrayList;
import java.util.List;

import dev.netcraft.network.NetworkDevice;
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
 * affiche son nom puis, pour chaque port, un voyant et « libre » ou « connecté ». L'état vient du
 * {@link ClientLinkCache}, tenu à jour par le serveur.
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
    private static final int COLOR_CONNECTED = 0xFF55FF55;
    private static final int COLOR_FREE = 0xFF707070;

    private final ClientLinkCache cache;

    PortStatusLayer(ClientLinkCache cache) {
        this.cache = cache;
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

        Font font = minecraft.font;
        Component name = state.getBlock().getName();
        List<Component> ports = new ArrayList<>();
        List<Boolean> connected = new ArrayList<>();
        int contentWidth = font.width(name);
        for (int i = 0; i < device.portCount(); i++) {
            boolean isConnected = this.cache.isConnected(pos, i);
            Component line = Component.translatable(isConnected ? "hud.netcraft.port.connected" : "hud.netcraft.port.free", i + 1);
            ports.add(line);
            connected.add(isConnected);
            contentWidth = Math.max(contentWidth, LIGHT_SIZE + LIGHT_GAP + font.width(line));
        }

        int lineHeight = font.lineHeight + LINE_GAP;
        int width = contentWidth + 2 * PADDING;
        int height = PADDING + lineHeight + LINE_GAP + ports.size() * lineHeight + PADDING - LINE_GAP;
        int left = (graphics.guiWidth() - width) / 2;
        graphics.fill(left, TOP, left + width, TOP + height, COLOR_BACKGROUND);
        graphics.outline(left, TOP, width, height, COLOR_BORDER);

        int x = left + PADDING;
        int y = TOP + PADDING;
        graphics.text(font, name, x, y, COLOR_NAME);
        y += lineHeight + LINE_GAP;
        for (int i = 0; i < ports.size(); i++) {
            int lightTop = y + (font.lineHeight - LIGHT_SIZE) / 2;
            graphics.fill(x, lightTop, x + LIGHT_SIZE, lightTop + LIGHT_SIZE, connected.get(i) ? COLOR_CONNECTED : COLOR_FREE);
            graphics.text(font, ports.get(i), x + LIGHT_SIZE + LIGHT_GAP, y, COLOR_TEXT);
            y += lineHeight;
        }
    }
}
