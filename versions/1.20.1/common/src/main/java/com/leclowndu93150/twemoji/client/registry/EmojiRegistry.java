package com.leclowndu93150.twemoji.client.registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.leclowndu93150.twemoji.Twemoji;
import com.leclowndu93150.twemoji.server.ServerEmojiLoader;
import com.leclowndu93150.twemoji.client.render.EmojiSprite;
import com.leclowndu93150.twemoji.client.render.TwemojiSheets;
import com.leclowndu93150.twemoji.client.render.glyph.StaticBakedGlyph;
import com.leclowndu93150.twemoji.network.payload.SyncedEmoji;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.leclowndu93150.twemoji.client.config.EmojiConfig;

public class EmojiRegistry extends SimplePreparableReloadListener<EmojiRegistry.LoadedData> {

    public static final EmojiRegistry INSTANCE = new EmojiRegistry();

    private static final Gson GSON = new Gson();
    private static final ResourceLocation SHORTCODES_ID = new ResourceLocation("emoji_shortcodes", "lang/en_us.json");
    private static final ResourceLocation REMAPPINGS_ID = new ResourceLocation("emoji_remappings", "lang/en_us.json");
    private static final ResourceLocation EMOJILIB_ID = new ResourceLocation("twemoji", "emojilib.json");
    private static final ResourceLocation FONT_ID = new ResourceLocation("twemoji", "font/emoji.json");
    private static final ResourceLocation EMOJI_SHEET = new ResourceLocation("twemoji", "textures/font/emoji.png");
    private static final ResourceLocation FLAGS_SHEET = new ResourceLocation("twemoji", "textures/font/flags.png");
    private static final int SHEET_W = 288;
    private static final int EMOJI_H = 4032;
    private static final int FLAGS_H = 306;

    private Map<String, EmojiEntry> builtinEntries = Collections.emptyMap();
    private Map<String, List<EmojiEntry>> builtinCategoryEntries = Collections.emptyMap();
    private Set<Integer> builtinCodepoints = Collections.emptySet();
    private Map<String, EmojiEntry> syncedStaticEntries = Collections.emptyMap();
    private List<EmojiEntry> syncedStaticPrimaryEntries = List.of();
    private Map<String, List<EmojiEntry>> syncedStaticCategoryEntries = Collections.emptyMap();
    private Map<String, EmojiEntry> combinedEntries = Collections.emptyMap();
    private Map<Integer, EmojiEntry> codepointEntries = Collections.emptyMap();
    private List<EmojiEntry> customEntries = List.of();
    private Map<String, List<EmojiEntry>> customCategoryEntries = Collections.emptyMap();
    private Map<String, String> categoryIcons = Collections.emptyMap();
    private ShapingTable shapingTable = ShapingTable.EMPTY;
    private Map<Integer, String> puaToRgi = Collections.emptyMap();
    private Set<Integer> vs16Shapeable = Collections.emptySet();
    private final Map<Integer, StaticBakedGlyph> syncedStaticGlyphs = new HashMap<>();
    private final List<DynamicTexture> ownedTextures = new ArrayList<>();
    private String[] suggestionInnerNames = new String[0];
    private EmojiEntry[] suggestionEntries = new EmojiEntry[0];
    private long suggestionVersion;
    private String lastSuggestionPrefix;
    private int lastSuggestionTone = -1;
    private long lastSuggestionVersion = -1;
    private List<EmojiEntry> lastSuggestionResult = List.of();

    private static final Map<String, String> EMOTICONS = Map.ofEntries(
        Map.entry(":)", "slight_smile"),
        Map.entry(":-)", "slight_smile"),
        Map.entry("(:", "slight_smile"),
        Map.entry(":(", "slight_frown"),
        Map.entry(":-(", "slight_frown"),
        Map.entry(":D", "smiley"),
        Map.entry(":-D", "smiley"),
        Map.entry(":P", "stuck_out_tongue"),
        Map.entry(":-P", "stuck_out_tongue"),
        Map.entry(":p", "stuck_out_tongue"),
        Map.entry(":-p", "stuck_out_tongue"),
        Map.entry(";)", "wink"),
        Map.entry(";-)", "wink"),
        Map.entry(":o", "open_mouth"),
        Map.entry(":O", "open_mouth"),
        Map.entry(":-o", "open_mouth"),
        Map.entry(":-O", "open_mouth"),
        Map.entry(":/", "confused"),
        Map.entry(":-/", "confused"),
        Map.entry(":\\", "confused"),
        Map.entry(":-\\", "confused"),
        Map.entry(":|", "neutral_face"),
        Map.entry(":-|", "neutral_face"),
        Map.entry(":'(", "cry"),
        Map.entry(":'-(", "cry"),
        Map.entry(":'D", "joy"),
        Map.entry("XD", "laughing"),
        Map.entry("xD", "laughing"),
        Map.entry("<3", "heart"),
        Map.entry("</3", "broken_heart"),
        Map.entry(">:(", "angry"),
        Map.entry(">:-(", "angry"),
        Map.entry("o/", "wave"),
        Map.entry("8)", "sunglasses"),
        Map.entry("B)", "sunglasses"),
        Map.entry(":*", "kissing_heart"),
        Map.entry(":-*", "kissing_heart")
    );

    private EmojiRegistry() {}

    record LoadedData(Map<String, EmojiEntry> entries, Map<String, List<EmojiEntry>> categoryEntries) {}

    public record EmojiEntry(String shortcode, String character, EmojiSprite sprite, List<EmojiSprite> toneSprites) {
        public EmojiSprite spriteForTone(int tone) {
            if (tone >= 1 && tone <= 5 && !toneSprites.isEmpty()) {
                int idx = Math.min(tone - 1, toneSprites.size() - 1);
                return toneSprites.get(idx);
            }
            return sprite;
        }
    }

    @Override
    protected LoadedData prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, String> shortcodes = loadJson(manager, SHORTCODES_ID);
        Map<String, EmojiSprite> charToSprite = buildCharSpriteMap(manager);

        Map<String, EmojiEntry> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> sc : shortcodes.entrySet()) {
            String code = sc.getKey();
            String ch = sc.getValue();
            EmojiSprite sprite = charToSprite.get(ch);
            if (sprite == null) continue;

            String inner = code.substring(1, code.length() - 1);
            String safeName = sanitizePath(inner);

            List<EmojiSprite> tones = new ArrayList<>();
            for (int t = 1; t <= 5; t++) {
                ResourceLocation toneId = new ResourceLocation("twemoji", "textures/emoji/" + safeName + "_tone" + t + ".png");
                if (resourceExists(manager, toneId)) {
                    tones.add(new EmojiSprite.Custom(toneId));
                }
            }

            result.put(inner, new EmojiEntry(code, ch, sprite, tones));
        }
        return new LoadedData(result, loadCategoryEntries(manager, result));
    }

    @Override
    protected void apply(LoadedData data, ResourceManager manager, ProfilerFiller profiler) {
        this.builtinEntries = data.entries();
        this.builtinCategoryEntries = data.categoryEntries();
        this.builtinCodepoints = collectBuiltinCodepoints(data.entries().values());
        rebuildCombined();
        rebuildShapingTable(manager);
    }

    private static Set<Integer> collectBuiltinCodepoints(Collection<EmojiEntry> entries) {
        Set<Integer> result = new HashSet<>();
        for (EmojiEntry entry : entries) {
            String character = entry.character();
            int i = 0;
            int len = character.length();
            while (i < len) {
                int cp = character.codePointAt(i);
                result.add(cp);
                i += Character.charCount(cp);
            }
        }
        return Set.copyOf(result);
    }

    public boolean isBuiltinCodepoint(int codepoint) {
        return builtinCodepoints.contains(codepoint);
    }

    private ResourceManager cachedResourceManager;

    private void rebuildShapingTable(ResourceManager manager) {
        this.cachedResourceManager = manager;
        Map<String, String> remap = loadJson(manager, REMAPPINGS_ID);
        if (remap.isEmpty()) {
            this.shapingTable = ShapingTable.EMPTY;
            this.puaToRgi = Collections.emptyMap();
            this.vs16Shapeable = Collections.emptySet();
            return;
        }
        boolean flagsEnabled = com.leclowndu93150.twemoji.client.config.EmojiConfig.get().isFlagsEnabled();
        Map<String, Integer> rgiToPua = new HashMap<>();
        Map<Integer, String> puaToRgi = new HashMap<>();
        Set<Integer> vs16Shapeable = new HashSet<>();
        for (Map.Entry<String, String> entry : remap.entrySet()) {
            String real = entry.getKey();
            String pua = entry.getValue();
            if (pua.isEmpty() || real.isEmpty() || real.equals(pua)) continue;
            int puaCp = pua.codePointAt(0);
            if (Character.charCount(puaCp) != pua.length()) continue;
            if (!flagsEnabled && containsRegionalIndicator(real)) continue;
            rgiToPua.putIfAbsent(real, puaCp);
            puaToRgi.putIfAbsent(puaCp, real);
            int firstCp = real.codePointAt(0);
            int firstWidth = Character.charCount(firstCp);
            if (real.length() == firstWidth + 1 && real.charAt(firstWidth) == 0xFE0F) {
                vs16Shapeable.add(firstCp);
            }
        }
        this.shapingTable = ShapingTable.build(rgiToPua);
        this.puaToRgi = Map.copyOf(puaToRgi);
        this.vs16Shapeable = Set.copyOf(vs16Shapeable);
    }

    private static boolean containsRegionalIndicator(String s) {
        int i = 0;
        int len = s.length();
        while (i < len) {
            int cp = s.codePointAt(i);
            if (cp >= 0x1F1E6 && cp <= 0x1F1FF) return true;
            i += Character.charCount(cp);
        }
        return false;
    }

    public void refreshShapingTable() {
        if (this.cachedResourceManager != null) rebuildShapingTable(this.cachedResourceManager);
    }

    public boolean rendersInEmojiFont(int codepoint) {
        if (TwemojiSheets.isStandardEmojiCodepoint(codepoint)) return true;
        if (codepoint < 0xF0000) return false;
        return AnimatedEmojiRegistry.INSTANCE.bakedGlyph(codepoint) != null || syncedStaticGlyphs.containsKey(codepoint);
    }

    public boolean needsShaping(String text) {
        ShapingTable table = this.shapingTable;
        int len = text.length();
        for (int i = 0; i < len; ) {
            int codepoint = text.codePointAt(i);
            if (table.isRoot(codepoint) || rendersInEmojiFont(codepoint)) return true;
            i += Character.charCount(codepoint);
        }
        return false;
    }

    public ShapingTable shapingTable() {
        return shapingTable;
    }

    public String unshape(String text) {
        StringBuilder out = null;
        int i = 0;
        int len = text.length();
        while (i < len) {
            int cp = text.codePointAt(i);
            int width = Character.charCount(cp);
            String rgi = puaToRgi.get(cp);
            if (rgi != null) {
                out = ensureBuilder(out, text, i, len);
                out.append(rgi);
            } else if (vs16Shapeable.contains(cp) && EmojiData.isDefaultTextPresentation(cp) && !nextIsVS16(text, i + width)) {
                out = ensureBuilder(out, text, i, len);
                out.appendCodePoint(cp);
                out.appendCodePoint(0xFE0F);
            } else if (out != null) {
                out.appendCodePoint(cp);
            }
            i += width;
        }
        return out == null ? text : out.toString();
    }

    private static StringBuilder ensureBuilder(StringBuilder out, String text, int upTo, int len) {
        if (out != null) return out;
        StringBuilder builder = new StringBuilder(len + 8);
        builder.append(text, 0, upTo);
        return builder;
    }

    private static boolean nextIsVS16(String text, int index) {
        return index < text.length() && text.charAt(index) == 0xFE0F;
    }

    public synchronized void applyServerSync(List<SyncedEmoji> staticEmojis, Map<String, String> categoryIcons) {
        this.categoryIcons = Map.copyOf(categoryIcons);
        clearSyncedTextures();
        Map<String, EmojiEntry> entries = new LinkedHashMap<>();
        List<EmojiEntry> primaryEntries = new ArrayList<>();
        Map<String, List<EmojiEntry>> categoryMap = new LinkedHashMap<>();
        Set<Integer> usedCodepoints = new HashSet<>(builtinCodepoints);
        for (SyncedEmoji emoji : staticEmojis) {
            try {
                int codepoint = resolveUniqueSyncedCodepoint(emoji.codepoint(), usedCodepoints);
                usedCodepoints.add(codepoint);

                NativeImage image;
                try (InputStream is = new ByteArrayInputStream(emoji.pngBytes())) {
                    image = NativeImage.read(NativeImage.Format.RGBA, is);
                }
                DynamicTexture texture = new DynamicTexture(image);
                ResourceLocation textureId = new ResourceLocation(Twemoji.MOD_ID, "synced/static/" + codepoint);
                Minecraft.getInstance().getTextureManager().register(textureId, texture);
                ownedTextures.add(texture);

                String character = new String(Character.toChars(codepoint));
                EmojiSprite sprite = new EmojiSprite.Custom(textureId);
                String resolvedName = resolveUniqueSyncedName(emoji.name(), entries);
                EmojiEntry entry = new EmojiEntry(":" + resolvedName + ":", character, sprite, List.of());
                entries.put(resolvedName, entry);
                primaryEntries.add(entry);
                for (String alias : emoji.aliases()) {
                    if (alias.isBlank()) continue;
                    if (builtinEntries.containsKey(alias) || entries.containsKey(alias)) continue;
                    entries.put(alias, entry);
                }

                if (!emoji.category().isBlank()) {
                    categoryMap.computeIfAbsent(emoji.category(), k -> new ArrayList<>()).add(entry);
                }

                syncedStaticGlyphs.put(codepoint, new StaticBakedGlyph(textureId));
            } catch (Exception e) {
                Twemoji.LOGGER.warn("Failed to apply synced static emoji {}", emoji.name(), e);
            }
        }
        this.syncedStaticEntries = entries;
        this.syncedStaticPrimaryEntries = List.copyOf(primaryEntries);
        this.syncedStaticCategoryEntries = copyCategoryMap(categoryMap);
        rebuildCombined();
    }

    private int resolveUniqueSyncedCodepoint(int requested, Set<Integer> used) {
        if (!used.contains(requested) && !isBuiltinCodepoint(requested)) return requested;
        for (int cp = ServerEmojiLoader.STATIC_CODEPOINT_START; cp <= ServerEmojiLoader.CODEPOINT_LIMIT; cp++) {
            if (!used.contains(cp) && !isBuiltinCodepoint(cp)) return cp;
        }
        return requested;
    }

    private String resolveUniqueSyncedName(String name, Map<String, EmojiEntry> staged) {
        if (!builtinEntries.containsKey(name) && !staged.containsKey(name)) return name;
        for (int i = 2; i < 1000; i++) {
            String candidate = name + "_" + i;
            if (!builtinEntries.containsKey(candidate) && !staged.containsKey(candidate)) return candidate;
        }
        return name + "_" + System.nanoTime();
    }

    public EmojiEntry entryForTone(EmojiEntry entry, int skinTone) {
        if (entry == null || skinTone <= 0 || isToneVariant(entry)) return entry;
        if (!entry.toneSprites().isEmpty()) return entry;
        String inner = innerName(entry);
        EmojiEntry toned = combinedEntries.get(inner + "_tone" + skinTone);
        return toned != null ? toned : entry;
    }

    public String characterForTone(EmojiEntry entry, int skinTone) {
        EmojiEntry toned = entryForTone(entry, skinTone);
        return toned != null ? unshape(toned.character()) : "";
    }

    public EmojiEntry get(String name) {
        return combinedEntries.get(name);
    }

    public EmojiEntry entryForCodepoint(int codepoint) {
        return codepointEntries.get(codepoint);
    }

    public EmojiSprite spriteForTone(EmojiEntry entry, int skinTone) {
        EmojiEntry toned = entryForTone(entry, skinTone);
        return toned != null ? toned.spriteForTone(skinTone) : null;
    }

    private void clearSyncedTextures() {
        for (DynamicTexture t : ownedTextures) t.close();
        ownedTextures.clear();
        syncedStaticGlyphs.clear();
        syncedStaticEntries = Collections.emptyMap();
        syncedStaticPrimaryEntries = List.of();
        syncedStaticCategoryEntries = Collections.emptyMap();
        customCategoryEntries = Collections.emptyMap();
        categoryIcons = Collections.emptyMap();
    }

    private void rebuildCombined() {
        Map<String, EmojiEntry> combined = new LinkedHashMap<>(builtinEntries);
        combined.putAll(syncedStaticEntries);
        List<EmojiEntry> custom = new ArrayList<>(syncedStaticPrimaryEntries);
        Map<String, List<EmojiEntry>> animCategoryMap = new LinkedHashMap<>();
        for (Map.Entry<String, AnimatedEmojiRegistry.AnimatedEmoji> animEntry : AnimatedEmojiRegistry.INSTANCE.getAll().entrySet()) {
            String name = animEntry.getKey();
            AnimatedEmojiRegistry.AnimatedEmoji emoji = animEntry.getValue();
            String character = new String(Character.toChars(emoji.codepoint()));
            EmojiSprite sprite = new EmojiSprite.Animated(emoji.codepoint());
            EmojiEntry entry = new EmojiEntry(":" + name + ":", character, sprite, List.of());
            if (!combined.containsKey(name)) {
                combined.put(name, entry);
                custom.add(entry);
            }
            for (String alias : emoji.aliases()) {
                if (!alias.isBlank()) combined.putIfAbsent(alias, entry);
            }
            if (!emoji.category().isBlank()) {
                animCategoryMap.computeIfAbsent(emoji.category(), k -> new ArrayList<>()).add(entry);
            }
        }
        Map<String, List<EmojiEntry>> mergedCustomCategories = copyCategoryMap(syncedStaticCategoryEntries);
        for (Map.Entry<String, List<EmojiEntry>> e : animCategoryMap.entrySet()) {
            mergedCustomCategories.merge(e.getKey(), e.getValue(), (a, b) -> {
                List<EmojiEntry> merged = new ArrayList<>(a);
                merged.addAll(b);
                return merged;
            });
        }
        this.combinedEntries = combined;
        this.codepointEntries = buildCodepointEntries(combined.values());
        this.customEntries = List.copyOf(custom);
        this.customCategoryEntries = mergedCustomCategories;
        rebuildSuggestionIndex();
    }

    private static Map<String, List<EmojiEntry>> copyCategoryMap(Map<String, List<EmojiEntry>> source) {
        Map<String, List<EmojiEntry>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<EmojiEntry>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    private void rebuildSuggestionIndex() {
        List<EmojiEntry> filtered = new ArrayList<>(combinedEntries.size());
        List<String> names = new ArrayList<>(combinedEntries.size());
        for (EmojiEntry entry : combinedEntries.values()) {
            if (isToneVariant(entry)) continue;
            filtered.add(entry);
            names.add(innerName(entry).toLowerCase(Locale.ROOT));
        }
        this.suggestionEntries = filtered.toArray(new EmojiEntry[0]);
        this.suggestionInnerNames = names.toArray(new String[0]);
        this.suggestionVersion++;
        this.lastSuggestionPrefix = null;
        this.lastSuggestionResult = List.of();
    }

    private static Map<Integer, EmojiEntry> buildCodepointEntries(Collection<EmojiEntry> entries) {
        Map<Integer, EmojiEntry> result = new HashMap<>();
        for (EmojiEntry entry : entries) {
            String character = entry.character();
            if (character.codePointCount(0, character.length()) != 1) continue;
            result.putIfAbsent(character.codePointAt(0), entry);
        }
        return Map.copyOf(result);
    }

    public void notifyAnimatedRegistryChanged() {
        rebuildCombined();
    }

    public StaticBakedGlyph syncedStaticGlyph(int codepoint) {
        return syncedStaticGlyphs.get(codepoint);
    }

    public boolean hasSyncedStaticGlyphs() {
        return !syncedStaticGlyphs.isEmpty();
    }

    private static Map<String, EmojiSprite> buildCharSpriteMap(ResourceManager manager) {
        Map<String, EmojiSprite> map = new HashMap<>();
        JsonObject font = loadJsonObject(manager, FONT_ID);
        if (font == null) return map;

        JsonArray providers = font.getAsJsonArray("providers");
        for (JsonElement el : providers) {
            JsonObject provider = el.getAsJsonObject();
            if (!provider.has("chars")) continue;
            String file = provider.get("file").getAsString();

            ResourceLocation sheet;
            int sheetH;
            if (file.equals("twemoji:font/emoji.png")) {
                sheet = EMOJI_SHEET;
                sheetH = EMOJI_H;
            } else if (file.equals("twemoji:font/flags.png")) {
                sheet = FLAGS_SHEET;
                sheetH = FLAGS_H;
            } else {
                continue;
            }

            JsonArray chars = provider.getAsJsonArray("chars");
            for (int row = 0; row < chars.size(); row++) {
                String rowStr = chars.get(row).getAsString();
                int col = 0;
                for (int cp = 0; cp < rowStr.length(); ) {
                    int codePoint = rowStr.codePointAt(cp);
                    String ch = new String(Character.toChars(codePoint));
                    map.put(ch, new EmojiSprite.Sheet(sheet, col, row, SHEET_W, sheetH));
                    col++;
                    cp += Character.charCount(codePoint);
                }
            }
        }
        return map;
    }

    private static String sanitizePath(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '+') sb.append("plus");
            else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/') sb.append(c);
            else sb.append('_');
        }
        return sb.toString();
    }

    private static boolean resourceExists(ResourceManager manager, ResourceLocation id) {
        return manager.getResource(id).isPresent();
    }

    private static Map<String, String> loadJson(ResourceManager manager, ResourceLocation id) {
        try {
            Optional<Resource> res = manager.getResource(id);
            if (res.isEmpty()) return Collections.emptyMap();
            try (InputStream is = res.get().open()) {
                Map<String, String> map = GSON.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>() {}.getType()
                );
                return map != null ? map : Collections.emptyMap();
            }
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    private static JsonObject loadJsonObject(ResourceManager manager, ResourceLocation id) {
        try {
            Optional<Resource> res = manager.getResource(id);
            if (res.isEmpty()) return null;
            try (InputStream is = res.get().open()) {
                return GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static JsonArray loadJsonArray(ResourceManager manager, ResourceLocation id) {
        try {
            Optional<Resource> res = manager.getResource(id);
            if (res.isEmpty()) return null;
            try (InputStream is = res.get().open()) {
                return GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonArray.class);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static Map<String, List<EmojiEntry>> loadCategoryEntries(ResourceManager manager, Map<String, EmojiEntry> entries) {
        JsonArray categories = loadJsonArray(manager, EMOJILIB_ID);
        if (categories == null) return Collections.emptyMap();

        Map<String, List<EmojiEntry>> result = new LinkedHashMap<>();
        for (JsonElement categoryElement : categories) {
            JsonObject category = categoryElement.getAsJsonObject();
            String categoryName = category.get("name").getAsString();
            JsonArray emojis = category.getAsJsonArray("emojis");
            List<EmojiEntry> categoryEntries = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (JsonElement emojiElement : emojis) {
                JsonObject emoji = emojiElement.getAsJsonObject();
                String[] aliases = emoji.get("name").getAsString().split(" ");
                for (String alias : aliases) {
                    EmojiEntry entry = entries.get(alias);
                    if (entry == null || isToneVariant(entry) || !seen.add(innerName(entry))) continue;
                    categoryEntries.add(entry);
                    break;
                }
            }
            result.put(categoryName, List.copyOf(categoryEntries));
        }
        return result;
    }

    public String applyShortcodes(String message, int skinTone) {
        boolean emoticons = EmojiConfig.get().isEmoticonsEnabled();
        if (message.indexOf(':') < 0) return emoticons ? applyEmoticons(message, skinTone) : message;
        StringBuilder out = new StringBuilder(message.length());
        int len = message.length();
        int i = 0;
        while (i < len) {
            int start = message.indexOf(':', i);
            if (start < 0) {
                out.append(message, i, len);
                break;
            }
            out.append(message, i, start);
            int end = message.indexOf(':', start + 1);
            if (end < 0) {
                out.append(message, start, len);
                break;
            }
            String inner = message.substring(start + 1, end);
            EmojiEntry entry = combinedEntries.get(inner);
            if (entry != null) {
                out.append(characterForTone(entry, skinTone));
                i = end + 1;
            } else {
                out.append(':');
                i = start + 1;
            }
        }
        return emoticons ? applyEmoticons(out.toString(), skinTone) : out.toString();
    }

    private String applyEmoticons(String message, int skinTone) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < message.length()) {
            boolean atWordStart = i == 0 || Character.isWhitespace(message.charAt(i - 1));
            String matched = null;
            String matchedChar = null;
            if (atWordStart) {
                for (Map.Entry<String, String> e : EMOTICONS.entrySet()) {
                    String token = e.getKey();
                    if (i + token.length() > message.length()) continue;
                    if (!message.regionMatches(i, token, 0, token.length())) continue;
                    int afterEnd = i + token.length();
                    boolean atWordEnd = afterEnd == message.length() || Character.isWhitespace(message.charAt(afterEnd));
                    if (!atWordEnd) continue;
                    EmojiEntry entry = combinedEntries.get(e.getValue());
                    if (entry == null) continue;
                    if (matched == null || token.length() > matched.length()) {
                        matched = token;
                        matchedChar = characterForTone(entry, skinTone);
                    }
                }
            }
            if (matched != null) {
                result.append(matchedChar);
                i += matched.length();
            } else {
                result.append(message.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    public List<EmojiEntry> getSuggestions(String prefix, int skinTone) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        if (lower.equals(lastSuggestionPrefix) && skinTone == lastSuggestionTone && suggestionVersion == lastSuggestionVersion) {
            return lastSuggestionResult;
        }
        EmojiEntry[] entries = suggestionEntries;
        String[] names = suggestionInnerNames;
        List<EmojiEntry> starts = new ArrayList<>();
        List<EmojiEntry> contains = new ArrayList<>();
        for (int i = 0; i < entries.length; i++) {
            String name = names[i];
            if (name.startsWith(lower)) starts.add(entries[i]);
            else if (name.contains(lower)) contains.add(entries[i]);
        }
        starts.addAll(contains);
        List<EmojiEntry> result = List.copyOf(starts);
        this.lastSuggestionPrefix = lower;
        this.lastSuggestionTone = skinTone;
        this.lastSuggestionVersion = suggestionVersion;
        this.lastSuggestionResult = result;
        return result;
    }

    public List<EmojiEntry> getCategoryEntries(String category) {
        return builtinCategoryEntries.getOrDefault(category, List.of());
    }

    public List<EmojiEntry> getCustomEntries() {
        return customEntries;
    }

    public Map<String, List<EmojiEntry>> getCustomCategoryEntries() {
        return customCategoryEntries;
    }

    public Map<String, String> getCategoryIcons() {
        return categoryIcons;
    }

    public static String innerName(EmojiEntry entry) {
        String shortcode = entry.shortcode();
        return shortcode.substring(1, shortcode.length() - 1);
    }

    public static boolean isToneVariant(EmojiEntry entry) {
        String shortcode = entry.shortcode().toLowerCase(Locale.ROOT);
        return shortcode.matches(".*_tone[1-5](_tone[1-5])?:") || shortcode.contains("_skin_tone:");
    }

    public Map<String, EmojiEntry> getEntries() {
        return combinedEntries;
    }
}
