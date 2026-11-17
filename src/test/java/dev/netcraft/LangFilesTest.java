package dev.netcraft;

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
 * Les deux fichiers de langue sont tenus ensemble, et chaque texte tient à sa place avec les valeurs
 * les plus longues (voir {@code docs/messages.md}) : 80 caractères pour la barre d'action, 45 pour une
 * ligne du panneau ou de l'infobulle, 90 pour le chat.
 *
 * <p>Chaque clé de ces familles doit avoir ses valeurs d'exemple ci-dessous : une clé ajoutée sans
 * exemple fait échouer le test, pour qu'on ne l'oublie pas.
 */
class LangFilesTest {
    private static final Path LANG = Path.of("src/main/resources/assets/netcraft/lang");

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

    /** Familles de clés vérifiées, avec leur longueur maximale. */
    private static final Map<String, Integer> LIMITS = Map.of(
            "message.netcraft.cable.", ACTION_BAR,
            "message.netcraft.network.", CHAT,
            "hud.netcraft.", PANEL,
            "item.netcraft.ethernet_cable.tooltip.", PANEL);

    /** Valeurs d'exemple de chaque clé, à partir du nom d'appareil le plus long de la langue. */
    private static Map<String, List<String>> samples(String name) {
        Map<String, List<String>> samples = new LinkedHashMap<>();
        samples.put("message.netcraft.cable.start", List.of(name, PORT));
        samples.put("message.netcraft.cable.no_free_port", List.of(name));
        samples.put("message.netcraft.cable.cancelled", List.of());
        samples.put("message.netcraft.cable.start_gone", List.of());
        samples.put("message.netcraft.cable.cancelled_dimension", List.of());
        samples.put("message.netcraft.cable.start_taken", List.of());
        samples.put("message.netcraft.cable.connected", List.of(name, PORT, name, PORT, DISTANCE));
        samples.put("message.netcraft.cable.unplugged", List.of(name, PORT));
        samples.put("message.netcraft.cable.nothing_to_unplug", List.of(name));
        samples.put("message.netcraft.cable.end_taken", List.of(name, name, PORT));
        samples.put("message.netcraft.cable.retracted", List.of(name));
        for (String refusal : List.of("not_a_device", "incompatible_port", "same_device", "other_dimension", "too_far")) {
            samples.put("message.netcraft.cable.result." + refusal, List.of(DISTANCE, DISTANCE));
        }
        // Un appareil s'écrit « nom [x, y, z] » (clé message.netcraft.device).
        samples.put("message.netcraft.network.loop", List.of(name + " [" + X + ", " + Y + ", " + Z + "]"));
        samples.put("message.netcraft.network.saturated", List.of());
        samples.put("hud.netcraft.port.connected", List.of(PORT));
        samples.put("hud.netcraft.port.free", List.of(PORT));
        samples.put("hud.netcraft.port.line", List.of(PORT));
        samples.put("hud.netcraft.port.connected_learned", List.of(PORT, MAC));
        samples.put("hud.netcraft.port.connected_learned_more", List.of(PORT, MAC, COUNT));
        samples.put("hud.netcraft.mac", List.of(MAC));
        samples.put("hud.netcraft.ports_connected", List.of(COUNT, COUNT));
        samples.put("hud.netcraft.connected", List.of());
        samples.put("hud.netcraft.not_connected", List.of());
        samples.put("hud.netcraft.sneak_for_details", List.of());
        samples.put("item.netcraft.ethernet_cable.tooltip.connect", List.of());
        samples.put("item.netcraft.ethernet_cable.tooltip.unplug", List.of());
        samples.put("item.netcraft.ethernet_cable.tooltip.cancel", List.of());
        return samples;
    }

    @Test
    void lesDeuxLanguesOntLesMemesCles() throws IOException {
        assertEquals(read("en_us").keySet(), read("fr_fr").keySet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"fr_fr", "en_us"})
    void chaqueTexteTientASaPlace(String language) throws IOException {
        Map<String, String> texts = read(language);
        String longestName = texts.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("block.netcraft."))
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
                problems.add(key + " : aucune valeur d'exemple dans LangFilesTest");
                return;
            }
            String rendered = format(text, args);
            if (length(rendered) > limit) {
                problems.add(key + " : " + length(rendered) + " caractères (" + limit + " au plus) : " + rendered);
            }
        }));
        assertTrue(problems.isEmpty(), language + "\n" + String.join("\n", problems));
    }

    // ---- Outils ----

    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?s|%%");

    /** Remplace {@code %s} et {@code %1$s} comme le fait Minecraft. */
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

    /** Lecteur minimal d'un objet JSON plat de chaînes, comme les fichiers de langue. */
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
                    throw new IllegalStateException("Clé en double : " + key);
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
                throw new IllegalStateException("Attendu " + c + " à la position " + this.pos);
            }
            this.pos++;
        }

        private char next(char a, char b) {
            char c = this.peek();
            if (c != a && c != b) {
                throw new IllegalStateException("Attendu " + a + " ou " + b + " à la position " + this.pos);
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
