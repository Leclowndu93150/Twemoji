package com.leclowndu93150.twemoji.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class TwemojiSheets {

    public static final Identifier FONT = Identifier.fromNamespaceAndPath("twemoji", "emoji");
    private static final FontDescription FONT_DESCRIPTION = new FontDescription.Resource(FONT);

    private static final String FONT_PATH = "/assets/twemoji/font/emoji.json";
    private static final int PRIVATE_USE_START = 0xF0000;
    private static final IntSet CODEPOINTS = loadCodepoints();

    private TwemojiSheets() {}

    public static boolean isTwemojiCodepoint(int codepoint) {
        return CODEPOINTS.contains(codepoint);
    }

    public static boolean isStandardEmojiCodepoint(int codepoint) {
        return codepoint < PRIVATE_USE_START && CODEPOINTS.contains(codepoint);
    }

    public static Style withEmojiFont(Style style) {
        return style.withFont(FONT_DESCRIPTION);
    }

    public static boolean isEmojiFont(Style style) {
        return style.getFont() instanceof FontDescription.Resource resource && FONT.equals(resource.id());
    }

    private static IntSet loadCodepoints() {
        IntSet codepoints = new IntOpenHashSet();
        try (InputStream stream = TwemojiSheets.class.getResourceAsStream(FONT_PATH)) {
            if (stream == null) return IntSets.emptySet();
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonElement providers = root.get("providers");
            if (providers == null) return IntSets.emptySet();
            for (JsonElement provider : providers.getAsJsonArray()) {
                JsonElement chars = provider.getAsJsonObject().get("chars");
                if (chars == null) continue;
                for (JsonElement row : chars.getAsJsonArray()) {
                    row.getAsString().codePoints().filter(codepoint -> codepoint != 0).forEach(codepoints::add);
                }
            }
        } catch (RuntimeException ignored) {
            return IntSets.emptySet();
        } catch (Exception ignored) {
            return IntSets.emptySet();
        }
        return IntSets.unmodifiable(codepoints);
    }
}
