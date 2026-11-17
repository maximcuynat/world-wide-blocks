package dev.netcraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.netcraft.command.NetCraftCommands;
import dev.netcraft.network.NetworkManager;
import dev.netcraft.network.sync.NetworkSync;
import dev.netcraft.registry.ModAttachments;
import dev.netcraft.registry.ModBlocks;
import dev.netcraft.registry.ModCreativeTabs;
import dev.netcraft.registry.ModDataComponents;
import dev.netcraft.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

// La valeur doit correspondre au modId déclaré dans META-INF/neoforge.mods.toml
@Mod(NetCraft.MODID)
public class NetCraft {
    public static final String MODID = "netcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NetCraft(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(NetworkSync::registerPayloads);

        NeoForge.EVENT_BUS.addListener(NetCraftCommands::register);
        NeoForge.EVENT_BUS.addListener(NetworkSync::onChunkSent);
        NeoForge.EVENT_BUS.addListener(NetworkSync::onChunkUnWatch);
        NeoForge.EVENT_BUS.addListener(NetworkManager::onServerTick);
        LOGGER.info("NetCraft chargé");
    }
}
