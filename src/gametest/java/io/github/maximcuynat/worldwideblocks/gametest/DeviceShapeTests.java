package io.github.maximcuynat.worldwideblocks.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.maximcuynat.worldwideblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Game tests of the shapes and of the placement of the devices, in the four orientations and in every state (laptop
 * open and closed):
 *
 * <ul>
 *   <li>the selection and collision shapes cover the model exactly as the blockstate turns it, read from the model and
 *       blockstate files of the mod (one texel of tolerance for a model with a rotated element, whose corners leave the
 *       grid);</li>
 *   <li>a device placed by a player faces that player ({@code FACING} toward them); with the rotations checked by
 *       {@code BlockResourcesTest}, the front of the model faces the player who places it.</li>
 * </ul>
 */
final class DeviceShapeTests {
    /** One texel of the design guide, in model pixels. */
    private static final double TEXEL = 0.25;
    private static final double EXACT = 1.0e-4;
    private static final List<Direction> LOOKS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    /** The devices checked, by registry name. */
    static final Map<String, Supplier<? extends Block>> DEVICES = Map.of(
            "pc_tower", ModBlocks.PC_TOWER,
            "internet_box", ModBlocks.INTERNET_BOX,
            "laptop", ModBlocks.LAPTOP,
            "desktop_switch_5", ModBlocks.DESKTOP_SWITCH_5,
            "desktop_switch_8", ModBlocks.DESKTOP_SWITCH_8);

    private record Bounds(double[] min, double[] max, boolean rotated) {
    }

    private DeviceShapeTests() {
    }

    /** Two tests per device, run with the shared tests: its shapes, and its placement by a player. */
    static Stream<WorldWideBlocksGameTests.Case> cases() {
        return DEVICES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(device -> Stream.of(
                        new WorldWideBlocksGameTests.Case("device_shapes_" + device.getKey(), WorldWideBlocksGameTests.Batch.SHARED, 20,
                                helper -> shapesFollowTheModel(helper, device.getValue())),
                        new WorldWideBlocksGameTests.Case("device_placement_" + device.getKey(), WorldWideBlocksGameTests.Batch.SHARED, 20,
                                helper -> placementFacesThePlayer(helper, device.getValue()))));
    }

    /** Every state of the device, each at its own place: selection and collision shapes against the turned model. */
    static void shapesFollowTheModel(GameTestHelper helper, Supplier<? extends Block> device) {
        Block block = device.get();
        String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
        JsonObject variants = resource(helper, "blockstates/" + id + ".json").getAsJsonObject("variants");
        List<BlockState> states = block.getStateDefinition().getPossibleStates();
        for (int i = 0; i < states.size(); i++) {
            BlockState state = states.get(i);
            BlockPos pos = new BlockPos(1 + 2 * (i % 7), 0, 1 + 2 * (i / 7));
            helper.setBlock(pos, state);
            BlockPos absolute = helper.absolutePos(pos);
            JsonObject variant = variant(helper, variants, state);
            int y = variant.has("y") ? variant.get("y").getAsInt() : 0;
            Bounds expected = turn(modelBounds(helper, variant.get("model").getAsString()), y);
            String name = id + " " + state;
            check(helper, "selection shape of " + name, state.getShape(helper.getLevel(), absolute), expected);
            check(helper, "collision shape of " + name, state.getCollisionShape(helper.getLevel(), absolute), expected);
            helper.destroyBlock(pos);
        }
        helper.succeed();
    }

    /** A player looking toward each direction places the device on a block: the device faces the player. */
    static void placementFacesThePlayer(GameTestHelper helper, Supplier<? extends Block> device) {
        Block block = device.get();
        String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
        NetworkScene scene = NetworkScene.of(helper);
        TestPlayer player = scene.player(GameType.CREATIVE);
        for (int i = 0; i < LOOKS.size(); i++) {
            Direction look = LOOKS.get(i);
            BlockPos support = new BlockPos(2 + 4 * i, 0, 8);
            helper.setBlock(support, Blocks.STONE);
            BlockPos absolute = helper.absolutePos(support);
            player.setYRot(look.toYRot());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block));
            BlockHitResult top = new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.5, 0.0), Direction.UP, absolute, false);
            player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, top);
            BlockPos placed = support.above();
            BlockState state = helper.getBlockState(placed);
            helper.assertTrue(state.is(block), id + " not placed by a player looking " + look);
            helper.assertValueEqual(state.getValue(HorizontalDirectionalBlock.FACING), look.getOpposite(),
                    "FACING of " + id + " placed by a player looking " + look);
            helper.destroyBlock(placed);
            helper.destroyBlock(support);
        }
        helper.succeed();
    }

    private static void check(GameTestHelper helper, String what, VoxelShape shape, Bounds expected) {
        helper.assertTrue(!shape.isEmpty(), what + ": empty");
        AABB box = shape.bounds();
        double[] actual = {box.minX * 16, box.minY * 16, box.minZ * 16, box.maxX * 16, box.maxY * 16, box.maxZ * 16};
        double[] wanted = {expected.min()[0], expected.min()[1], expected.min()[2], expected.max()[0], expected.max()[1], expected.max()[2]};
        double tolerance = expected.rotated() ? TEXEL : EXACT;
        for (int k = 0; k < 6; k++) {
            if (Math.abs(actual[k] - wanted[k]) > tolerance) {
                helper.fail(what + ": bounds " + format(actual) + " px, model " + format(wanted) + " px (tolerance " + tolerance + ")");
            }
        }
    }

    /** Bounds of the elements of a model of the mod, rotated elements included (single-axis rotation, as the game does). */
    private static Bounds modelBounds(GameTestHelper helper, String reference) {
        String path = reference.substring(reference.indexOf(':') + 1);
        JsonArray elements = resource(helper, "models/" + path + ".json").getAsJsonArray("elements");
        double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        boolean rotated = false;
        for (JsonElement element : elements) {
            JsonObject e = element.getAsJsonObject();
            double[] from = doubles(e.getAsJsonArray("from"));
            double[] to = doubles(e.getAsJsonArray("to"));
            JsonObject rotation = e.has("rotation") ? e.getAsJsonObject("rotation") : null;
            double angle = rotation == null ? 0.0 : Math.toRadians(rotation.get("angle").getAsDouble());
            rotated |= angle != 0.0;
            for (int corner = 0; corner < 8; corner++) {
                double[] p = {(corner & 1) == 0 ? from[0] : to[0], (corner & 2) == 0 ? from[1] : to[1], (corner & 4) == 0 ? from[2] : to[2]};
                if (angle != 0.0) {
                    // Rotation by the angle around the positive axis through the origin (CuboidRotation.SingleAxisRotation).
                    int axis = "xyz".indexOf(rotation.get("axis").getAsString());
                    double[] origin = doubles(rotation.getAsJsonArray("origin"));
                    int a = (axis + 1) % 3;
                    int b = (axis + 2) % 3;
                    double u = p[a] - origin[a];
                    double v = p[b] - origin[b];
                    p[a] = origin[a] + u * Math.cos(angle) - v * Math.sin(angle);
                    p[b] = origin[b] + u * Math.sin(angle) + v * Math.cos(angle);
                }
                for (int k = 0; k < 3; k++) {
                    min[k] = Math.min(min[k], p[k]);
                    max[k] = Math.max(max[k], p[k]);
                }
            }
        }
        return new Bounds(min, max, rotated);
    }

    /** Bounds turned by the {@code y} of the blockstate, by steps of 90 degrees: north goes east, as the game turns models. */
    private static Bounds turn(Bounds bounds, int y) {
        double[] min = bounds.min().clone();
        double[] max = bounds.max().clone();
        for (int step = 0; step < Math.floorMod(y, 360) / 90; step++) {
            double minX = 16 - max[2];
            double maxX = 16 - min[2];
            max[2] = max[0];
            min[2] = min[0];
            min[0] = minX;
            max[0] = maxX;
        }
        return new Bounds(min, max, bounds.rotated());
    }

    /** The variant of the blockstate file that matches the state: all the properties of its key have the state's values. */
    private static JsonObject variant(GameTestHelper helper, JsonObject variants, BlockState state) {
        for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
            boolean matches = true;
            for (String pair : entry.getKey().split(",")) {
                if (pair.isEmpty()) {
                    continue;
                }
                String[] parts = pair.split("=", 2);
                Property<?> property = state.getBlock().getStateDefinition().getProperty(parts[0]);
                if (property == null || parts.length < 2 || !valueName(state, property).equals(parts[1])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                JsonElement value = entry.getValue();
                return value.isJsonArray() ? value.getAsJsonArray().get(0).getAsJsonObject() : value.getAsJsonObject();
            }
        }
        throw helper.assertionException(Component.literal("No blockstate variant for " + state));
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /** A JSON resource of the mod ({@code assets/worldwideblocks/...}), from the classpath of the development run. */
    private static JsonObject resource(GameTestHelper helper, String path) {
        String name = "/assets/worldwideblocks/" + path;
        try (InputStream in = DeviceShapeTests.class.getResourceAsStream(name)) {
            if (in == null) {
                throw helper.assertionException(Component.literal("Resource not found on the classpath: " + name));
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw helper.assertionException(Component.literal("Cannot read " + name + ": " + e.getMessage()));
        }
    }

    private static double[] doubles(JsonArray array) {
        double[] values = new double[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsDouble();
        }
        return values;
    }

    private static String format(double[] values) {
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            text.append(i == 0 ? "" : ", ").append(Math.round(values[i] * 1000) / 1000.0);
        }
        return text.append(']').toString();
    }
}
