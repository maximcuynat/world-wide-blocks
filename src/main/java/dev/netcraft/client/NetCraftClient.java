package dev.netcraft.client;

import dev.netcraft.NetCraft;
import dev.netcraft.network.sync.LinkAddedPayload;
import dev.netcraft.network.sync.LinkRemovedPayload;
import dev.netcraft.network.sync.LinksSyncPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
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

    public NetCraftClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerPayloadHandlers);
        modEventBus.addListener(this::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);
    }

    private void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(LinksSyncPayload.TYPE, (payload, context) -> this.links.replaceAll(payload.links()));
        event.register(LinkAddedPayload.TYPE, (payload, context) -> this.links.add(payload.link()));
        event.register(LinkRemovedPayload.TYPE, (payload, context) -> this.links.remove(payload.id()));
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, Identifier.fromNamespaceAndPath(NetCraft.MODID, "port_status"), new PortStatusLayer(this.links));
    }

    /** En solo, deux mondes ne doivent pas se mélanger : on oublie tout en quittant. */
    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        this.links.clear();
    }
}
