package dev.netcraft.client;

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
 * Indicateur d'état minimal : en visant un appareil réseau, affiche sous le réticule son nom puis,
 * pour chaque port, « libre » ou « connecté ». L'état vient du {@link ClientLinkCache}, tenu à jour
 * par le serveur.
 */
final class PortStatusLayer implements GuiLayer {
    private static final int COLOR_NAME = 0xFFFFFFFF;
    private static final int COLOR_CONNECTED = 0xFF55FF55;
    private static final int COLOR_FREE = 0xFFAAAAAA;

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
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 12;
        graphics.centeredText(font, state.getBlock().getName(), x, y, COLOR_NAME);
        for (int i = 0; i < device.portCount(); i++) {
            y += font.lineHeight + 1;
            boolean connected = this.cache.isConnected(pos, i);
            Component line = Component.translatable(connected ? "hud.netcraft.port.connected" : "hud.netcraft.port.free", i + 1);
            graphics.centeredText(font, line, x, y, connected ? COLOR_CONNECTED : COLOR_FREE);
        }
    }
}
