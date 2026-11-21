package io.github.maximcuynat.worldwideblocks.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** The cord colours of the code against {@code design/palette.json} and the plug assets. */
class CordColourTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/worldwideblocks");

    @Test
    void theColoursAreThoseOfThePaletteInItsOrder() throws IOException {
        JsonObject cords = JsonParser.parseString(Files.readString(Path.of("design/palette.json"))).getAsJsonObject().getAsJsonObject("cords");
        List<String> palette = new ArrayList<>(cords.keySet());
        palette.remove("description");
        List<String> code = new ArrayList<>();
        for (CordColour colour : CordColour.values()) {
            code.add(colour.serializedName());
        }
        assertEquals(palette, code);
    }

    @Test
    void eachColourHasItsPlugModelAndItsBaseTexel() throws IOException {
        JsonObject cords = JsonParser.parseString(Files.readString(Path.of("design/palette.json"))).getAsJsonObject().getAsJsonObject("cords");
        BufferedImage texture = ImageIO.read(ASSETS.resolve("textures/block/rj45_plug.png").toFile());
        assertEquals(CordColour.PLUG_TEXTURE_SIZE, texture.getWidth());
        assertEquals(CordColour.PLUG_TEXTURE_SIZE, texture.getHeight());
        for (CordColour colour : CordColour.values()) {
            assertTrue(Files.exists(ASSETS.resolve("models/" + colour.plugModel() + ".json")), "plug model of " + colour);
            int expected = Integer.parseInt(cords.getAsJsonObject(colour.serializedName()).get("base").getAsString().substring(1), 16);
            int actual = texture.getRGB(colour.baseTexelX(), colour.baseTexelY()) & 0xFFFFFF;
            assertEquals(expected, actual, "base texel of " + colour + ": " + Integer.toHexString(actual));
        }
    }
}
