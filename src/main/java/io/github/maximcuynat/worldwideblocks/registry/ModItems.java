package io.github.maximcuynat.worldwideblocks.registry;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.item.EthernetCableItem;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WorldWideBlocks.MODID);

    public static final DeferredItem<BlockItem> PC_TOWER = ITEMS.registerSimpleBlockItem("pc_tower", ModBlocks.PC_TOWER);

    public static final DeferredItem<BlockItem> LAPTOP = ITEMS.registerSimpleBlockItem("laptop", ModBlocks.LAPTOP);

    public static final DeferredItem<BlockItem> INTERNET_BOX = ITEMS.registerSimpleBlockItem("internet_box", ModBlocks.INTERNET_BOX);

    public static final DeferredItem<EthernetCableItem> ETHERNET_CABLE = ITEMS.registerItem("ethernet_cable", EthernetCableItem::new);

    private ModItems() {
    }
}
