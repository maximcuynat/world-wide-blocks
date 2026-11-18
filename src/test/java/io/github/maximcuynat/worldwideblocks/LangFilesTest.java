package io.github.maximcuynat.worldwideblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/**
 * Both language files are kept together, and every text fits its place with the longest values
 * (see {@code docs/messages.md}): 80 characters for the action bar, 45 for a line of the panel or
 * of the tooltip, 90 for the chat.
 *
 * <p>Every key of these families must have its sample values below: a key added without a
 * sample makes the test fail, so that nobody forgets it.
 */
class LangFilesTest {
    private static final Path LANG = Path.of("src/main/resources/assets/worldwideblocks/lang");

    private static final int ACTION_BAR = 80;
    private static final int PANEL = 45;
    private static final int CHAT = 90;

    private static final String PORT = "5";
    private static final String DISTANCE = "100";
    private static final String MAC = "02:5e:11:a0:7c:1a";
    private static final String COUNT = "99";
    private static final String X = "-1234";
    private static final String Y = "320";
    private static final String Z = "-5678";

    /** Checked key families, with their maximum length. */
    private static final Map<String, Integer> LIMITS = Map.of(
            "message.worldwideblocks.cable.", ACTION_BAR,
            "message.worldwideblocks.network.", CHAT,
            "hud.worldwideblocks.", PANEL,
            "item.worldwideblocks.ethernet_cable.tooltip.", PANEL);

    /** Sample values of each key, built from the longest device name of the language. */
    private static Map<String, List<String>> samples(String name) {
        Map<String, List<String>> samples = new LinkedHashMap<>();
        samples.put("message.worldwideblocks.cable.start", List.of(name, PORT));
        samples.put("message.worldwideblocks.cable.no_free_port", List.of(name));
        samples.put("message.worldwideblocks.cable.cancelled", List.of());
        samples.put("message.worldwideblocks.cable.start_gone", List.of());
        samples.put("message.worldwideblocks.cable.cancelled_dimension", List.of());
        samples.put("message.worldwideblocks.cable.start_taken", List.of());
        samples.put("message.worldwideblocks.cable.connected", List.of(name, PORT, name, PORT, DISTANCE));
        samples.put("message.worldwideblocks.cable.unplugged", List.of(name, PORT));
        samples.put("message.worldwideblocks.cable.nothing_to_unplug", List.of(name));
        samples.put("message.worldwideblocks.cable.end_taken", List.of(name, name, PORT));
        samples.put("message.worldwideblocks.cable.retracted", List.of(name));
        for (String refusal : List.of("not_a_device", "incompatible_port", "same_device", "other_dimension", "too_far")) {
            samples.put("message.worldwideblocks.cable.result." + refusal, List.of(DISTANCE, DISTANCE));
        }
        // A device is written "name [x, y, z]" (key message.worldwideblocks.device).
        samples.put("message.worldwideblocks.network.loop", List.of(name + " [" + X + ", " + Y + ", " + Z + "]"));
        samples.put("message.worldwideblocks.network.saturated", List.of());
        samples.put("message.worldwideblocks.network.dhcp_conflict", List.of(name + " [" + X + ", " + Y + ", " + Z + "]"));
        samples.put("hud.worldwideblocks.port.connected", List.of(PORT));
        samples.put("hud.worldwideblocks.port.free", List.of(PORT));
        samples.put("hud.worldwideblocks.port.line", List.of(PORT));
        samples.put("hud.worldwideblocks.port.connected_learned", List.of(PORT, MAC));
        samples.put("hud.worldwideblocks.port.connected_learned_more", List.of(PORT, MAC, COUNT));
        samples.put("hud.worldwideblocks.mac", List.of(MAC));
        samples.put("hud.worldwideblocks.ports_connected", List.of(COUNT, COUNT));
        samples.put("hud.worldwideblocks.connected", List.of());
        samples.put("hud.worldwideblocks.not_connected", List.of());
        samples.put("hud.worldwideblocks.sneak_for_details", List.of());
        samples.put("item.worldwideblocks.ethernet_cable.tooltip.connect", List.of());
        samples.put("item.worldwideblocks.ethernet_cable.tooltip.unplug", List.of());
        samples.put("item.worldwideblocks.ethernet_cable.tooltip.cancel", List.of());
        return samples;
    }

    @Test
    void bothLanguagesHaveTheSameKeys() throws IOException {
        assertEquals(read("en_us").keySet(), read("fr_fr").keySet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"fr_fr", "en_us"})
    void everyTextFitsItsPlace(String language) throws IOException {
        Map<String, String> texts = read(language);
        String longestName = texts.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("block.worldwideblocks."))
                .map(Map.Entry::getValue)
                .max((a, b) -> Integer.compare(length(a), length(b)))
                .orElseThrow();
        Map<String, List<String>> samples = samples(longestName);
        List<String> problems = new ArrayList<>();
        texts.forEach((key, text) -> LIMITS.forEach((prefix, limit) -> {
            if (!key.startsWith(prefix)) {
                return;
            }
            List<String> args = samples.get(key);
            if (args == null) {
                problems.add(key + ": no sample values in LangFilesTest");
                return;
            }
            String rendered = format(text, args);
            if (length(rendered) > limit) {
                problems.add(key + " : " + length(rendered) + " characters (" + limit + " at most): " + rendered);
            }
        }));
        assertTrue(problems.isEmpty(), language + "\n" + String.join("\n", problems));
    }

    // ---- Helpers ----

    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?s|%%");

    /** Replaces {@code %s} and {@code %1$s} the way Minecraft does. */
    static String format(String text, List<String> args) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        int next = 0;
        while (matcher.find()) {
            String replacement;
            if (matcher.group().equals("%%")) {
                replacement = "%";
            } else {
                int index = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) - 1 : next++;
                replacement = index < args.size() ? args.get(index) : "";
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static int length(String text) {
        return text.codePointCount(0, text.length());
    }

    private static Map<String, String> read(String language) throws IOException {
        return FlatJson.parse(Files.readString(LANG.resolve(language + ".json"), StandardCharsets.UTF_8));
    }

    /** Minimal reader of a flat JSON object of strings, like the language files. */
    private static final class FlatJson {
        private final String text;
        private int pos;

        private FlatJson(String text) {
            this.text = text;
        }

        static Map<String, String> parse(String text) {
            FlatJson json = new FlatJson(text);
            Map<String, String> map = new LinkedHashMap<>();
            json.expect('{');
            if (json.peek() == '}') {
                return map;
            }
            do {
                String key = json.string();
                json.expect(':');
                if (map.put(key, json.string()) != null) {
                    throw new IllegalStateException("Duplicate key: " + key);
                }
            } while (json.next(',', '}') == ',');
            return map;
        }

        private char peek() {
            while (Character.isWhitespace(this.text.charAt(this.pos))) {
                this.pos++;
            }
            return this.text.charAt(this.pos);
        }

        private void expect(char c) {
            if (this.peek() != c) {
                throw new IllegalStateException("Expected " + c + " at position " + this.pos);
            }
            this.pos++;
        }

        private char next(char a, char b) {
            char c = this.peek();
            if (c != a && c != b) {
                throw new IllegalStateException("Expected " + a + " or " + b + " at position " + this.pos);
            }
            this.pos++;
            return c;
        }

        private String string() {
            this.expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                char c = this.text.charAt(this.pos++);
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                char escaped = this.text.charAt(this.pos++);
                switch (escaped) {
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        out.append((char) Integer.parseInt(this.text.substring(this.pos, this.pos + 4), 16));
                        this.pos += 4;
                    }
                    default -> out.append(escaped);
                }
            }
        }
    }
}
