package io.github.maximcuynat.worldwideblocks.registry;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WorldWideBlocks.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WORLD_WIDE_BLOCKS = CREATIVE_MODE_TABS.register("worldwideblocks", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.worldwideblocks"))
            .icon(() -> ModItems.PC_TOWER.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.PC_TOWER.get());
                output.accept(ModItems.LAPTOP.get());
                output.accept(ModItems.INTERNET_BOX.get());
                output.accept(ModItems.DESKTOP_SWITCH_5.get());
                output.accept(ModItems.ETHERNET_CABLE.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
