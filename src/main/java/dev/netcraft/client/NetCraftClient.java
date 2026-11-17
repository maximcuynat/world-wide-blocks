package dev.netcraft.client;

import dev.netcraft.NetCraft;
import dev.netcraft.network.sync.DeviceStatusPayload;
import dev.netcraft.network.sync.LinkAddedPayload;
import dev.netcraft.network.sync.LinkRemovedPayload;
import dev.netcraft.network.sync.LinksResetPayload;
import dev.netcraft.network.sync.MessagePayload;
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
 * Point d'entrée côté client : jamais chargé sur un serveur dédié. Le client ne fait qu'afficher
 * ce que le serveur lui envoie.
 */
@Mod(value = NetCraft.MODID, dist = Dist.CLIENT)
public final class NetCraftClient {
    private final ClientLinkCache links = new ClientLinkCache();
    private final ClientDeviceStatus deviceStatus = new ClientDeviceStatus();
    private final CableRenderer cableRenderer = new CableRenderer(this.links);
    private final NetCraftClientConfig config = new NetCraftClientConfig();
    private final ClientMessages messages = new ClientMessages(this.config);

    public NetCraftClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, this.config.spec());
        modEventBus.addListener(this::registerPayloadHandlers);
        modEventBus.addListener(this::registerGuiLayers);
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
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, Identifier.fromNamespaceAndPath(NetCraft.MODID, "port_status"), new PortStatusLayer(this.links, this.deviceStatus));
    }

    private void extractCables(ExtractLevelRenderStateEvent event) {
        this.cableRenderer.extract(event);
    }

    private void submitCables(SubmitCustomGeometryEvent event) {
        this.cableRenderer.submit(event);
    }

    /** En solo, deux mondes ne doivent pas se mélanger : on oublie tout en quittant. */
    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        this.links.clear();
        this.deviceStatus.clear();
    }
}
