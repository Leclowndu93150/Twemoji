package com.leclowndu93150.twemoji.client.registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leclowndu93150.twemoji.Twemoji;
import com.leclowndu93150.twemoji.client.render.glyph.AnimatedBakedGlyph;
import com.leclowndu93150.twemoji.network.payload.SyncedEmoji;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

public final class AnimatedEmojiRegistry {

    public static final AnimatedEmojiRegistry INSTANCE = new AnimatedEmojiRegistry();

    private static final Gson GSON = new Gson();

    private final Map<Integer, AnimatedEmoji> byCodepoint = new HashMap<>();
    private final Map<String, AnimatedEmoji> byName = new LinkedHashMap<>();
    private final Map<Integer, AnimatedBakedGlyph> bakedGlyphs = new HashMap<>();
    private final List<DynamicTexture> ownedTextures = new ArrayList<>();

    private AnimatedEmojiRegistry() {}

    public record AnimatedEmoji(String name, int codepoint, List<ResourceLocation> frameTextures, int[] frameSchedule, int frameTimeMs, String category, List<String> aliases) {
        public ResourceLocation currentFrame() {
            int idx = (int) ((System.currentTimeMillis() / frameTimeMs) % frameSchedule.length);
            return frameTextures.get(frameSchedule[idx]);
        }
    }

    public synchronized void applyServerSync(List<SyncedEmoji> animatedEmojis) {
        clearTextures();
        Set<Integer> used = new HashSet<>();
        for (SyncedEmoji emoji : animatedEmojis) {
            try {
                int codepoint = resolveUniqueCodepoint(emoji.codepoint(), used);
                used.add(codepoint);
                AnimatedEmoji loaded = loadEmoji(emoji, codepoint);
                if (loaded == null) continue;
                byCodepoint.put(loaded.codepoint(), loaded);
                byName.put(loaded.name(), loaded);
                bakedGlyphs.put(loaded.codepoint(), new AnimatedBakedGlyph(loaded));
            } catch (Exception e) {
                Twemoji.LOGGER.warn("Failed to apply synced animated emoji {}", emoji.name(), e);
            }
        }
        EmojiRegistry.INSTANCE.notifyAnimatedRegistryChanged();
    }

    private static int resolveUniqueCodepoint(int requested, Set<Integer> used) {
        if (!EmojiRegistry.INSTANCE.isBuiltinCodepoint(requested) && !used.contains(requested)) return requested;
        for (int cp = 0xF000; cp <= 0xF8FF; cp++) {
            if (!EmojiRegistry.INSTANCE.isBuiltinCodepoint(cp) && !used.contains(cp)) return cp;
        }
        for (int cp = 0xE000; cp <= 0xEFFF; cp++) {
            if (!EmojiRegistry.INSTANCE.isBuiltinCodepoint(cp) && !used.contains(cp)) return cp;
        }
        return requested;
    }

    private AnimatedEmoji loadEmoji(SyncedEmoji emoji, int codepoint) throws IOException {
        AnimationMeta meta = parseMeta(emoji.mcmetaJson());

        NativeImage source;
        try (InputStream is = new ByteArrayInputStream(emoji.pngBytes())) {
            source = NativeImage.read(NativeImage.Format.RGBA, is);
        }

        int frameWidth = source.getWidth();
        int frameHeight = frameWidth;
        int frameCount = source.getHeight() / frameHeight;
        if (frameCount < 1) {
            source.close();
            return null;
        }

        List<ResourceLocation> frameTextures = new ArrayList<>(frameCount);
        for (int i = 0; i < frameCount; i++) {
            NativeImage frame = new NativeImage(frameWidth, frameHeight, false);
            source.copyRect(frame, 0, i * frameHeight, 0, 0, frameWidth, frameHeight, false, false);

            DynamicTexture texture = new DynamicTexture(frame);
            ownedTextures.add(texture);

            ResourceLocation textureId = new ResourceLocation(Twemoji.MOD_ID, "synced/anim/" + codepoint + "/frame_" + i);
            Minecraft.getInstance().getTextureManager().register(textureId, texture);
            frameTextures.add(textureId);
        }
        source.close();

        int[] schedule = buildSchedule(meta, frameCount);
        int frameTimeMs = Math.max(1, meta.frametime) * 50;

        return new AnimatedEmoji(emoji.name(), codepoint, frameTextures, schedule, frameTimeMs, emoji.category(), emoji.aliases());
    }

    private static int[] buildSchedule(AnimationMeta meta, int frameCount) {
        if (meta.frames == null || meta.frames.isEmpty()) {
            int[] result = new int[frameCount];
            for (int i = 0; i < frameCount; i++) result[i] = i;
            return result;
        }
        List<Integer> expanded = new ArrayList<>();
        for (FrameEntry frame : meta.frames) {
            int duration = Math.max(1, frame.time);
            for (int i = 0; i < duration; i++) expanded.add(frame.index);
        }
        return expanded.stream().mapToInt(Integer::intValue).toArray();
    }

    private static AnimationMeta parseMeta(String json) {
        AnimationMeta meta = new AnimationMeta();
        if (json == null || json.isEmpty()) return meta;
        JsonObject root = GSON.fromJson(new StringReader(json), JsonObject.class);
        if (root == null || !root.has("animation")) return meta;
        JsonObject anim = root.getAsJsonObject("animation");
        if (anim.has("frametime")) meta.frametime = anim.get("frametime").getAsInt();
        if (anim.has("frames")) {
            JsonArray frames = anim.getAsJsonArray("frames");
            meta.frames = new ArrayList<>();
            for (JsonElement el : frames) {
                if (el.isJsonPrimitive()) {
                    FrameEntry f = new FrameEntry();
                    f.index = el.getAsInt();
                    f.time = meta.frametime;
                    meta.frames.add(f);
                } else {
                    JsonObject obj = el.getAsJsonObject();
                    FrameEntry f = new FrameEntry();
                    f.index = obj.get("index").getAsInt();
                    f.time = obj.has("time") ? obj.get("time").getAsInt() : meta.frametime;
                    meta.frames.add(f);
                }
            }
        }
        return meta;
    }

    public AnimatedEmoji byCodepoint(int codepoint) {
        return byCodepoint.get(codepoint);
    }

    public AnimatedEmoji byName(String name) {
        return byName.get(name);
    }

    public Map<String, AnimatedEmoji> getAll() {
        return byName;
    }

    public boolean isAnimated(int codepoint) {
        return byCodepoint.containsKey(codepoint);
    }

    public AnimatedBakedGlyph bakedGlyph(int codepoint) {
        return bakedGlyphs.get(codepoint);
    }

    public boolean hasAny() {
        return !bakedGlyphs.isEmpty();
    }

    private void clearTextures() {
        byCodepoint.clear();
        byName.clear();
        bakedGlyphs.clear();
        for (DynamicTexture t : ownedTextures) t.close();
        ownedTextures.clear();
    }

    private static final class AnimationMeta {
        int frametime = 1;
        List<FrameEntry> frames;
    }

    private static final class FrameEntry {
        int index;
        int time;
    }
}
