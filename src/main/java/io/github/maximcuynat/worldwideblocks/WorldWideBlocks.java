package io.github.maximcuynat.worldwideblocks;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import io.github.maximcuynat.worldwideblocks.command.WorldWideBlocksCommands;
import io.github.maximcuynat.worldwideblocks.network.NetworkManager;
import io.github.maximcuynat.worldwideblocks.network.sync.NetworkSync;
import io.github.maximcuynat.worldwideblocks.registry.ModAttachments;
import io.github.maximcuynat.worldwideblocks.registry.ModBlocks;
import io.github.maximcuynat.worldwideblocks.registry.ModCreativeTabs;
import io.github.maximcuynat.worldwideblocks.registry.ModDataComponents;
import io.github.maximcuynat.worldwideblocks.registry.ModItems;
import io.github.maximcuynat.worldwideblocks.terminal.TerminalServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

// The value must match the modId declared in META-INF/neoforge.mods.toml
@Mod(WorldWideBlocks.MODID)
public class WorldWideBlocks {
    public static final String MODID = "worldwideblocks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WorldWideBlocks(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(NetworkSync::registerPayloads);

        NeoForge.EVENT_BUS.addListener(WorldWideBlocksCommands::register);
        NeoForge.EVENT_BUS.addListener(NetworkSync::onChunkSent);
        NeoForge.EVENT_BUS.addListener(NetworkSync::onChunkUnWatch);
        NeoForge.EVENT_BUS.addListener(NetworkManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(NetworkSync::onServerTick);
        // After the simulation: the changes of the consoles of this tick are sent.
        NeoForge.EVENT_BUS.addListener(TerminalServer::onServerTick);
        NeoForge.EVENT_BUS.addListener(TerminalServer::onLoggedOut);
        LOGGER.info("World Wide Blocks loaded");
    }
}
