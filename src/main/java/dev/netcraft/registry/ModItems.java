package dev.netcraft.registry;

import dev.netcraft.NetCraft;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NetCraft.MODID);

    public static final DeferredItem<BlockItem> PC_TOWER = ITEMS.registerSimpleBlockItem("pc_tower", ModBlocks.PC_TOWER);

    private ModItems() {
    }
}
