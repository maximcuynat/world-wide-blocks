package io.github.maximcuynat.worldwideblocks.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import io.github.maximcuynat.worldwideblocks.client.PlugModels;
import io.github.maximcuynat.worldwideblocks.port.CordColour;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Checks, once the resources are loaded, that the client draws every block and item of the mod with its own model: no
 * block state falls back to the missing model, no item to the missing item model, and no quad of a block state nor the
 * texture of an item is the missing texture (a model or a texture that does not load, a wrong path). Run at the start of
 * every capture run, once in the world (an item stack needs the components bound with the world) (issue "Capture scenarios of the devices").
 *
 * <p>The quads of an item model are not readable from outside its render state in 26.3: an item is checked through its
 * model and the texture its render state picks for its particles, the texture of {@code layer0} for a flat item and the
 * texture of the block for a block item.
 */
final class ModelCheck {
    private static final Direction[] SIDES_AND_NONE = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST, null};

    /** What was checked and what is wrong. */
    record Result(int states, int items, int plugs, int quads, List<String> problems) {
        String summary() {
            return this.states + " block states, " + this.items + " items, " + this.plugs + " plug models, " + this.quads + " quads";
        }
    }

    private ModelCheck() {
    }

    static Result run(Minecraft minecraft) {
        ModelManager models = minecraft.getModelManager();
        BlockStateModelSet blockModels = models.getBlockStateModelSet();
        List<String> problems = new ArrayList<>();
        int states = 0;
        int quads = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(WorldWideBlocks.MODID)) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                states++;
                BlockStateModel model = blockModels.get(state);
                if (model == blockModels.missingModel()) {
                    problems.add(state + ": missing model");
                    continue;
                }
                List<BlockStateModelPart> parts = new ArrayList<>();
                model.collectParts(RandomSource.create(42L), parts);
                boolean missingTexture = isMissing(model.particleMaterial());
                for (BlockStateModelPart part : parts) {
                    for (Direction side : SIDES_AND_NONE) {
                        for (BakedQuad quad : part.getQuads(side)) {
                            quads++;
                            missingTexture |= isMissing(quad.materialInfo().sprite());
                        }
                    }
                }
                if (missingTexture) {
                    problems.add(state + ": missing texture");
                }
            }
        }
        int items = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(WorldWideBlocks.MODID)) {
                continue;
            }
            items++;
            ItemStack stack = new ItemStack(item);
            Identifier modelId = stack.get(DataComponents.ITEM_MODEL);
            if (modelId == null || models.getItemModel(modelId) == models.getMissingModels().item()) {
                problems.add("item " + id + ": missing item model " + modelId);
                continue;
            }
            ItemStackRenderState render = new ItemStackRenderState();
            minecraft.getItemModelResolver().updateForTopItem(render, stack, ItemDisplayContext.GUI, minecraft.level, null, 0);
            Material.Baked particle = render.pickParticleMaterial(RandomSource.create(42L));
            if (particle == null || isMissing(particle)) {
                problems.add("item " + id + ": missing texture");
            }
        }
        // The plug models (lot 1d-1): standalone models, used by no block state.
        int plugs = 0;
        for (Map.Entry<CordColour, StandaloneModelKey<BlockStateModelPart>> plug : PlugModels.keys().entrySet()) {
            plugs++;
            BlockStateModelPart part = models.getStandaloneModel(plug.getValue());
            if (part == null) {
                problems.add("plug " + plug.getKey().serializedName() + ": model not loaded");
                continue;
            }
            boolean missingTexture = false;
            int plugQuads = 0;
            for (Direction side : SIDES_AND_NONE) {
                for (BakedQuad quad : part.getQuads(side)) {
                    plugQuads++;
                    missingTexture |= isMissing(quad.materialInfo().sprite());
                }
            }
            quads += plugQuads;
            if (plugQuads == 0 || missingTexture) {
                problems.add("plug " + plug.getKey().serializedName() + ": " + (plugQuads == 0 ? "no quads" : "missing texture"));
            }
        }
        return new Result(states, items, plugs, quads, List.copyOf(problems));
    }

    private static boolean isMissing(Material.Baked material) {
        return isMissing(material.sprite());
    }

    private static boolean isMissing(TextureAtlasSprite sprite) {
        return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation());
    }
}
