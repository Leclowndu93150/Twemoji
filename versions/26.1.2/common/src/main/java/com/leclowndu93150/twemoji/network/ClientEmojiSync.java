package com.leclowndu93150.twemoji.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.leclowndu93150.twemoji.client.AnimatedEmojiRegistry;
import com.leclowndu93150.twemoji.client.EmojiRegistry;
import com.leclowndu93150.twemoji.Twemoji;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientEmojiSync {

    public static final ClientEmojiSync INSTANCE = new ClientEmojiSync();

    private static final Gson GSON = new Gson();

    private long activeSessionId;
    private boolean sessionActive;
    private int expectedTotal;
    private Map<String, String> pendingCategoryIcons = Map.of();
    private final Map<Integer, EmojiBuilder> builders = new HashMap<>();

    private ClientEmojiSync() {}

    public synchronized void onStart(EmojiSyncStartPayload payload) {
        activeSessionId = payload.sessionId();
        sessionActive = true;
        expectedTotal = payload.totalEmojis();
        builders.clear();
        try {
            pendingCategoryIcons = GSON.fromJson(payload.categoryIconsJson(), new TypeToken<Map<String, String>>() {}.getType());
            if (pendingCategoryIcons == null) pendingCategoryIcons = Map.of();
        } catch (Exception e) {
            pendingCategoryIcons = Map.of();
        }
    }

    public synchronized void onChunk(EmojiSyncChunkPayload payload) {
        if (!sessionActive || payload.sessionId() != activeSessionId) return;
        EmojiBuilder builder = builders.computeIfAbsent(payload.emojiIndex(), idx -> new EmojiBuilder(payload));
        builder.addPart(payload);
    }

    public synchronized void onEnd(EmojiSyncEndPayload payload) {
        if (!sessionActive || payload.sessionId() != activeSessionId) {
            sessionActive = false;
            builders.clear();
            return;
        }
        if (builders.size() != expectedTotal) {
            Twemoji.LOGGER.warn("Emoji sync: expected {} emojis, got {}, discarding", expectedTotal, builders.size());
            sessionActive = false;
            builders.clear();
            return;
        }
        List<SyncedEmoji> assembled = new ArrayList<>(expectedTotal);
        for (int i = 0; i < expectedTotal; i++) {
            EmojiBuilder builder = builders.get(i);
            if (builder == null || !builder.complete()) {
                Twemoji.LOGGER.warn("Emoji sync: missing or incomplete index {}, discarding", i);
                sessionActive = false;
                builders.clear();
                return;
            }
            assembled.add(builder.build());
        }
        Map<String, String> icons = pendingCategoryIcons;
        sessionActive = false;
        builders.clear();
        pendingCategoryIcons = Map.of();
        applyAtomically(assembled, icons);
    }

    public synchronized void onDisconnect() {
        sessionActive = false;
        builders.clear();
        pendingCategoryIcons = Map.of();
        applyAtomically(List.of(), Map.of());
    }

    private void applyAtomically(List<SyncedEmoji> emojis, Map<String, String> categoryIcons) {
        List<SyncedEmoji> staticEmojis = new ArrayList<>();
        List<SyncedEmoji> animatedEmojis = new ArrayList<>();
        for (SyncedEmoji e : emojis) {
            if (e.animated()) animatedEmojis.add(e);
            else staticEmojis.add(e);
        }
        AnimatedEmojiRegistry.INSTANCE.applyServerSync(animatedEmojis);
        EmojiRegistry.INSTANCE.applyServerSync(staticEmojis, categoryIcons);
    }

    private static final class EmojiBuilder {
        final String name;
        final int codepoint;
        final boolean animated;
        final String mcmetaJson;
        final int totalParts;
        final byte[][] parts;
        final String category;
        final List<String> aliases;
        int receivedParts;

        EmojiBuilder(EmojiSyncChunkPayload first) {
            this.name = first.name();
            this.codepoint = first.codepoint();
            this.animated = first.animated();
            this.mcmetaJson = first.mcmetaJson();
            this.totalParts = first.pngPartTotal();
            this.parts = new byte[totalParts][];
            this.category = first.category();
            this.aliases = first.aliases().isEmpty() ? List.of() : Arrays.asList(first.aliases().split(","));
        }

        void addPart(EmojiSyncChunkPayload payload) {
            if (payload.pngPartIndex() < 0 || payload.pngPartIndex() >= totalParts) return;
            if (parts[payload.pngPartIndex()] != null) return;
            parts[payload.pngPartIndex()] = payload.pngBytes();
            receivedParts++;
        }

        boolean complete() {
            return receivedParts == totalParts;
        }

        SyncedEmoji build() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] part : parts) out.writeBytes(part);
            return new SyncedEmoji(name, codepoint, animated, mcmetaJson, out.toByteArray(), category, aliases);
        }
    }
}
