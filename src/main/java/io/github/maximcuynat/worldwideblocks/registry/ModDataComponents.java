package io.github.maximcuynat.worldwideblocks.registry;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.item.CableStart;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, WorldWideBlocks.MODID);

    /** First end of a cord whose connection is in progress. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CableStart>> CABLE_START = DATA_COMPONENTS.registerComponentType("cable_start",
            builder -> builder.persistent(CableStart.CODEC).networkSynchronized(CableStart.STREAM_CODEC).ignoreSwapAnimation());

    private ModDataComponents() {
    }
}
