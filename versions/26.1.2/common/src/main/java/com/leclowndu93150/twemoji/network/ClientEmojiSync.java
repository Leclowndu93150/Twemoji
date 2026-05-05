package com.leclowndu93150.twemoji.network;

import com.leclowndu93150.twemoji.client.AnimatedEmojiRegistry;
import com.leclowndu93150.twemoji.client.EmojiRegistry;
import com.leclowndu93150.twemoji.Twemoji;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientEmojiSync {

    public static final ClientEmojiSync INSTANCE = new ClientEmojiSync();

    private long activeSessionId;
    private boolean sessionActive;
    private int expectedTotal;
    private final Map<Integer, EmojiBuilder> builders = new HashMap<>();

    private ClientEmojiSync() {}

    public synchronized void onStart(EmojiSyncStartPayload payload) {
        activeSessionId = payload.sessionId();
        sessionActive = true;
        expectedTotal = payload.totalEmojis();
        builders.clear();
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
        sessionActive = false;
        builders.clear();
        applyAtomically(assembled);
    }

    public synchronized void onDisconnect() {
        sessionActive = false;
        builders.clear();
        applyAtomically(List.of());
    }

    private void applyAtomically(List<SyncedEmoji> emojis) {
        List<SyncedEmoji> staticEmojis = new ArrayList<>();
        List<SyncedEmoji> animatedEmojis = new ArrayList<>();
        for (SyncedEmoji e : emojis) {
            if (e.animated()) animatedEmojis.add(e);
            else staticEmojis.add(e);
        }
        AnimatedEmojiRegistry.INSTANCE.applyServerSync(animatedEmojis);
        EmojiRegistry.INSTANCE.applyServerSync(staticEmojis);
    }

    private static final class EmojiBuilder {
        final String name;
        final int codepoint;
        final boolean animated;
        final String mcmetaJson;
        final int totalParts;
        final byte[][] parts;
        int receivedParts;

        EmojiBuilder(EmojiSyncChunkPayload first) {
            this.name = first.name();
            this.codepoint = first.codepoint();
            this.animated = first.animated();
            this.mcmetaJson = first.mcmetaJson();
            this.totalParts = first.pngPartTotal();
            this.parts = new byte[totalParts][];
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
            return new SyncedEmoji(name, codepoint, animated, mcmetaJson, out.toByteArray());
        }
    }
}
