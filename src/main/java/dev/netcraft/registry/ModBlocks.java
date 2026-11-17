package dev.netcraft.registry;

import dev.netcraft.NetCraft;
import dev.netcraft.block.LaptopBlock;
import dev.netcraft.block.PcTowerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NetCraft.MODID);

    // Les appareils réseau refusent les pistons : le moteur réseau les identifie par leur position.
    public static final DeferredBlock<PcTowerBlock> PC_TOWER = BLOCKS.registerBlock("pc_tower", PcTowerBlock::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(2.0F).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));

    public static final DeferredBlock<LaptopBlock> LAPTOP = BLOCKS.registerBlock("laptop", LaptopBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));

    private ModBlocks() {
    }
}
