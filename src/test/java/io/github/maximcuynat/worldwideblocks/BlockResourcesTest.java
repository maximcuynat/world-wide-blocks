package io.github.maximcuynat.worldwideblocks;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/**
 * Static checks of the block resources, without launching the game: what the client would otherwise only report while
 * loading the models (a missing model or texture, a face outside its texture), the rotations of the blockstates that
 * turn the front of a model toward {@code FACING}, and the colours of the textures against the palette of the design
 * guide ({@code design/palette.json}, the single source of the palette).
 */
class BlockResourcesTest {
    private static final String NAMESPACE = "worldwideblocks";
    private static final Path ASSETS = Path.of("src/main/resources/assets", NAMESPACE);
    private static final Path PALETTE = Path.of("design/palette.json");
    /** Rotation {@code y} of the blockstate that turns a model whose front faces north toward each {@code FACING}. */
    private static final Map<String, Integer> FACING_Y = Map.of("north", 0, "east", 90, "south", 180, "west", 270);

    @Test
    void blockstatesPointToModelsAndTurnTheFrontTowardFacing() throws IOException {
        List<String> errors = new ArrayList<>();
        for (Path file : jsonFiles(ASSETS.resolve("blockstates"))) {
            JsonObject variants = read(file).getAsJsonObject("variants");
            if (variants == null) {
                errors.add(file.getFileName() + ": no \"variants\" (multipart is not checked)");
                continue;
            }
            for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
                for (JsonObject model : objects(variant.getValue())) {
                    String where = file.getFileName() + " [" + variant.getKey() + "]";
                    checkModelExists(model.get("model").getAsString(), where, errors);
                    String facing = property(variant.getKey(), "facing");
                    if (facing != null) {
                        int y = model.has("y") ? model.get("y").getAsInt() : 0;
                        if (!FACING_Y.containsKey(facing) || FACING_Y.get(facing) != y) {
                            errors.add(where + ": y = " + y + ", expected " + FACING_Y.get(facing) + " to turn the front toward " + facing);
                        }
                    }
                }
            }
        }
        assertNoError(errors);
    }

    @Test
    void itemModelsPointToModels() throws IOException {
        List<String> errors = new ArrayList<>();
        for (Path file : jsonFiles(ASSETS.resolve("items"))) {
            List<String> references = new ArrayList<>();
            collectModelReferences(read(file), references);
            if (references.isEmpty()) {
                errors.add(file.getFileName() + ": no model");
            }
            references.forEach(reference -> checkModelExists(reference, file.getFileName().toString(), errors));
        }
        assertNoError(errors);
    }

    @Test
    void modelsUseExistingTexturesWithFacesInsideTheirBounds() throws IOException {
        List<String> errors = new ArrayList<>();
        for (Path file : jsonFiles(ASSETS.resolve("models/block"))) {
            String name = "models/block/" + file.getFileName();
            JsonObject model = read(file);
            JsonObject textures = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
            int[] textureSize = model.has("texture_size") ? ints(model.getAsJsonArray("texture_size")) : null;
            for (Map.Entry<String, JsonElement> texture : textures.entrySet()) {
                String value = texture.getValue().getAsString();
                if (value.startsWith("#")) {
                    if (!textures.has(value.substring(1))) {
                        errors.add(name + ": texture #" + texture.getKey() + " points to an undefined " + value);
                    }
                    continue;
                }
                Path png = texturePath(value);
                if (png == null) {
                    continue; // texture of another namespace (the game's)
                }
                if (!Files.isRegularFile(png)) {
                    errors.add(name + ": missing texture " + value + " (" + png + ")");
                    continue;
                }
                BufferedImage image = ImageIO.read(png.toFile());
                if (textureSize != null && (image.getWidth() != textureSize[0] || image.getHeight() != textureSize[1])) {
                    errors.add(name + ": texture_size " + textureSize[0] + " x " + textureSize[1] + " but " + value + " is "
                            + image.getWidth() + " x " + image.getHeight());
                }
            }
            JsonArray elements = model.has("elements") ? model.getAsJsonArray("elements") : new JsonArray();
            for (JsonElement element : elements) {
                checkElement(name, element.getAsJsonObject(), textures, errors);
            }
        }
        assertNoError(errors);
    }

    @Test
    void texturesUseOnlyThePaletteOfTheDesignGuide() throws IOException {
        JsonObject palette = read(PALETTE);
        Set<String> colours = new TreeSet<>();
        for (Map.Entry<String, JsonElement> role : palette.getAsJsonObject("roles").entrySet()) {
            role.getValue().getAsJsonObject().entrySet().forEach(tone -> colours.add(tone.getValue().getAsString().toLowerCase()));
        }
        Set<String> exceptions = palette.getAsJsonObject("exceptions").keySet();
        List<String> errors = new ArrayList<>();
        for (String exception : exceptions) {
            if (!Files.isRegularFile(ASSETS.resolve(exception))) {
                errors.add("design/palette.json: exception for a texture that does not exist: " + exception);
            }
        }
        List<Path> pngs;
        try (Stream<Path> walk = Files.walk(ASSETS.resolve("textures"))) {
            pngs = walk.filter(path -> path.toString().endsWith(".png")).sorted().toList();
        }
        for (Path png : pngs) {
            String name = ASSETS.relativize(png).toString().replace('\\', '/');
            if (exceptions.contains(name)) {
                continue;
            }
            BufferedImage image = ImageIO.read(png.toFile());
            Set<String> outside = new TreeSet<>();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    if (argb >>> 24 == 0) {
                        continue; // transparent: not drawn
                    }
                    String colour = String.format("#%06x", argb & 0xFFFFFF);
                    if (!colours.contains(colour)) {
                        outside.add(colour);
                    }
                }
            }
            if (!outside.isEmpty()) {
                errors.add(name + ": colours outside the palette " + outside);
            }
        }
        assertNoError(errors);
    }

    @Test
    void everyBlockHasItsNameInBothLanguages() throws IOException {
        List<String> errors = new ArrayList<>();
        Map<String, JsonObject> languages = new HashMap<>();
        for (String language : List.of("en_us", "fr_fr")) {
            languages.put(language, read(ASSETS.resolve("lang/" + language + ".json")));
        }
        for (Path file : jsonFiles(ASSETS.resolve("blockstates"))) {
            String key = "block." + NAMESPACE + "." + file.getFileName().toString().replace(".json", "");
            languages.forEach((language, keys) -> {
                if (!keys.has(key)) {
                    errors.add(language + ".json: no " + key);
                }
            });
        }
        assertNoError(errors);
    }

    /** An element: coordinates within the range the game accepts, faces with a defined texture and UVs inside it. */
    private static void checkElement(String model, JsonObject element, JsonObject textures, List<String> errors) {
        String where = model + " [" + (element.has("name") ? element.get("name").getAsString() : "element") + "]";
        double[] from = doubles(element.getAsJsonArray("from"));
        double[] to = doubles(element.getAsJsonArray("to"));
        for (int axis = 0; axis < 3; axis++) {
            if (from[axis] < -16 || to[axis] > 32 || from[axis] > to[axis]) {
                errors.add(where + ": from/to outside -16..32 or reversed on axis " + "xyz".charAt(axis));
            }
        }
        JsonObject faces = element.getAsJsonObject("faces");
        if (faces == null || faces.isEmpty()) {
            errors.add(where + ": no face");
            return;
        }
        for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
            JsonObject data = face.getValue().getAsJsonObject();
            String texture = data.has("texture") ? data.get("texture").getAsString() : "";
            if (!texture.startsWith("#") || !textures.has(texture.substring(1))) {
                errors.add(where + " " + face.getKey() + ": texture " + texture + " not defined in the model");
            }
            if (data.has("uv")) {
                for (double uv : doubles(data.getAsJsonArray("uv"))) {
                    if (uv < 0 || uv > 16) {
                        errors.add(where + " " + face.getKey() + ": uv " + data.get("uv") + " outside 0..16");
                        break;
                    }
                }
            }
        }
    }

    private static void checkModelExists(String reference, String where, List<String> errors) {
        Path path = modelPath(reference);
        if (path != null && !Files.isRegularFile(path)) {
            errors.add(where + ": missing model " + reference + " (" + path + ")");
        }
    }

    /** {@code worldwideblocks:block/x} to its file, or null for a model of another namespace. */
    private static Path modelPath(String reference) {
        String prefix = NAMESPACE + ":";
        return reference.startsWith(prefix) ? ASSETS.resolve("models/" + reference.substring(prefix.length()) + ".json") : null;
    }

    private static Path texturePath(String reference) {
        String prefix = NAMESPACE + ":";
        return reference.startsWith(prefix) ? ASSETS.resolve("textures/" + reference.substring(prefix.length()) + ".png") : null;
    }

    /** Every {@code "model": "<namespace>:..."} string of an item model definition, at any depth. */
    private static void collectModelReferences(JsonElement element, List<String> references) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if (entry.getKey().equals("model") && value.isJsonPrimitive()) {
                    references.add(value.getAsString());
                } else {
                    collectModelReferences(value, references);
                }
            }
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectModelReferences(child, references));
        }
    }

    /** Value of a property in a variant key such as {@code facing=north,open=true}, or null. */
    private static String property(String variantKey, String name) {
        for (String pair : variantKey.split(",")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(name)) {
                return parts[1];
            }
        }
        return null;
    }

    private static List<JsonObject> objects(JsonElement element) {
        List<JsonObject> list = new ArrayList<>();
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> list.add(child.getAsJsonObject()));
        } else {
            list.add(element.getAsJsonObject());
        }
        return list;
    }

    private static List<Path> jsonFiles(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static int[] ints(JsonArray array) {
        int[] values = new int[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsInt();
        }
        return values;
    }

    private static double[] doubles(JsonArray array) {
        double[] values = new double[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsDouble();
        }
        return values;
    }

    private static void assertNoError(List<String> errors) {
        assertTrue(errors.isEmpty(), errors.size() + " problem(s):\n" + String.join("\n", errors));
    }
}
