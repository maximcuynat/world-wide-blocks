package dev.netcraft.registry;

import dev.netcraft.NetCraft;
import dev.netcraft.item.CableStart;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NetCraft.MODID);

    /** Première extrémité d'un cordon en cours de branchement. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CableStart>> CABLE_START = DATA_COMPONENTS.registerComponentType("cable_start",
            builder -> builder.persistent(CableStart.CODEC).networkSynchronized(CableStart.STREAM_CODEC).ignoreSwapAnimation());

    private ModDataComponents() {
    }
}
