package dev.netcraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

// La valeur doit correspondre au modId déclaré dans META-INF/neoforge.mods.toml
@Mod(NetCraft.MODID)
public class NetCraft {
    public static final String MODID = "netcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NetCraft(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("NetCraft chargé");
    }
}
