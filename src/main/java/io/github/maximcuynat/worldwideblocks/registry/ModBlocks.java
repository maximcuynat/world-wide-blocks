package io.github.maximcuynat.worldwideblocks.registry;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.block.DesktopSwitchBlock;
import io.github.maximcuynat.worldwideblocks.block.InternetBoxBlock;
import io.github.maximcuynat.worldwideblocks.block.LaptopBlock;
import io.github.maximcuynat.worldwideblocks.block.PcTowerBlock;
import io.github.maximcuynat.worldwideblocks.port.PortLayouts;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WorldWideBlocks.MODID);

    // Network devices refuse pistons: the network engine identifies them by their position.
    public static final DeferredBlock<PcTowerBlock> PC_TOWER = BLOCKS.registerBlock("pc_tower", PcTowerBlock::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(2.0F).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));

    public static final DeferredBlock<LaptopBlock> LAPTOP = BLOCKS.registerBlock("laptop", LaptopBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));

    public static final DeferredBlock<InternetBoxBlock> INTERNET_BOX = BLOCKS.registerBlock("internet_box", InternetBoxBlock::new,
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.0F).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));

    public static final DeferredBlock<DesktopSwitchBlock> DESKTOP_SWITCH_5 = BLOCKS.registerBlock("desktop_switch_5",
            p -> new DesktopSwitchBlock(p, PortLayouts.DESKTOP_SWITCH_5),
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.0F).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.IMMOVEABLE));

    private ModBlocks() {
    }
}
