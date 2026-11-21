package io.github.maximcuynat.worldwideblocks.client;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.network.sync.DeviceStatusPayload;
import io.github.maximcuynat.worldwideblocks.network.sync.LinkAddedPayload;
import io.github.maximcuynat.worldwideblocks.network.sync.LinkRemovedPayload;
import io.github.maximcuynat.worldwideblocks.network.sync.LinksResetPayload;
import io.github.maximcuynat.worldwideblocks.network.sync.MessagePayload;
import io.github.maximcuynat.worldwideblocks.network.sync.TerminalClosePayload;
import io.github.maximcuynat.worldwideblocks.network.sync.TerminalUpdatesPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * Client-side entry point: never loaded on a dedicated server. The client only displays
 * what the server sends it.
 */
@Mod(value = WorldWideBlocks.MODID, dist = Dist.CLIENT)
public final class WorldWideBlocksClient {
    private final ClientLinkCache links = new ClientLinkCache();
    private final ClientDeviceStatus deviceStatus = new ClientDeviceStatus();
    private final CableRenderer cableRenderer = new CableRenderer(this.links);
    private final PortOutlineRenderer portOutline = new PortOutlineRenderer();
    private final WorldWideBlocksClientConfig config = new WorldWideBlocksClientConfig();
    private final ClientMessages messages = new ClientMessages(this.config);
    private final ClientTerminal terminal = new ClientTerminal(this.config);

    public WorldWideBlocksClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, this.config.spec());
        modEventBus.addListener(this::registerPayloadHandlers);
        modEventBus.addListener(this::registerGuiLayers);
        modEventBus.addListener(PlugModels::register);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(this::extractCables);
        NeoForge.EVENT_BUS.addListener(this::submitCables);
    }

    private void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(LinksResetPayload.TYPE, (payload, context) -> {
            this.links.clear();
            this.deviceStatus.clear();
        });
        event.register(LinkAddedPayload.TYPE, (payload, context) -> this.links.add(payload.link()));
        event.register(LinkRemovedPayload.TYPE, (payload, context) -> this.links.remove(payload.id()));
        event.register(DeviceStatusPayload.TYPE, (payload, context) -> this.deviceStatus.set(payload));
        event.register(MessagePayload.TYPE, (payload, context) -> this.messages.show(payload));
        event.register(TerminalUpdatesPayload.TYPE, (payload, context) -> this.terminal.receive(payload));
        event.register(TerminalClosePayload.TYPE, (payload, context) -> this.terminal.close(payload));
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, "port_status"), new PortStatusLayer(this.links, this.deviceStatus));
    }

    private void extractCables(ExtractLevelRenderStateEvent event) {
        this.cableRenderer.extract(event);
        this.portOutline.extract(event);
    }

    private void submitCables(SubmitCustomGeometryEvent event) {
        this.cableRenderer.submit(event);
        this.portOutline.submit(event);
    }

    /** In singleplayer, two worlds must not get mixed up: everything is forgotten on leaving. */
    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        this.links.clear();
        this.terminal.clear();
        this.deviceStatus.clear();
    }
}
