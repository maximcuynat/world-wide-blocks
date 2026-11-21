package io.github.maximcuynat.worldwideblocks.client;

import java.util.EnumMap;
import java.util.Map;

import io.github.maximcuynat.worldwideblocks.WorldWideBlocks;
import io.github.maximcuynat.worldwideblocks.port.CordColour;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * The RJ45 plug models, one per cord colour ({@code worldwideblocks:block/rj45_plug_<colour>}, {@code docs/cable-design.md}
 * section 4). No block state uses them, so the game does not bake them by itself: they are registered as standalone
 * models and read back from the model manager when the plugs are drawn.
 */
public final class PlugModels {
    private static final Map<CordColour, StandaloneModelKey<BlockStateModelPart>> KEYS = new EnumMap<>(CordColour.class);

    static {
        for (CordColour colour : CordColour.values()) {
            String id = WorldWideBlocks.MODID + ":" + colour.plugModel();
            KEYS.put(colour, new StandaloneModelKey<>(() -> id));
        }
    }

    private PlugModels() {
    }

    static void register(ModelEvent.RegisterStandalone event) {
        KEYS.forEach((colour, key) -> event.register(key,
                SimpleUnbakedStandaloneModel.simpleModelWrapper(Identifier.fromNamespaceAndPath(WorldWideBlocks.MODID, colour.plugModel()))));
    }

    /** The baked plug of this colour; {@code null} before the models are loaded. */
    public static BlockStateModelPart get(ModelManager models, CordColour colour) {
        return models.getStandaloneModel(KEYS.get(colour));
    }

    /** The keys, for the model check of the capture runs. */
    public static Map<CordColour, StandaloneModelKey<BlockStateModelPart>> keys() {
        return Map.copyOf(KEYS);
    }
}
