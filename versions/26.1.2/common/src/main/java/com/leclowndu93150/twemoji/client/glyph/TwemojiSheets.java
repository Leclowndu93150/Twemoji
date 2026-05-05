package com.leclowndu93150.twemoji.client.glyph;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class TwemojiSheets {

    private static final String SHORTCODES_PATH = "/assets/emoji_shortcodes/lang/en_us.json";
    private static final IntSet CODEPOINTS = loadCodepoints();

    private TwemojiSheets() {}

    public static boolean isTwemojiCodepoint(int codepoint) {
        return CODEPOINTS.contains(codepoint);
    }

    private static IntSet loadCodepoints() {
        IntSet codepoints = new IntOpenHashSet();
        try (InputStream stream = TwemojiSheets.class.getResourceAsStream(SHORTCODES_PATH)) {
            if (stream == null) return IntSets.emptySet();
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                entry.getValue().getAsString().codePoints().filter(codepoint -> codepoint != 0).forEach(codepoints::add);
            }
        } catch (RuntimeException ignored) {
            return IntSets.emptySet();
        } catch (Exception ignored) {
            return IntSets.emptySet();
        }
        return IntSets.unmodifiable(codepoints);
    }
}
