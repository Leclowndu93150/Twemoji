package com.leclowndu93150.twemoji.server;

import com.leclowndu93150.twemoji.network.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.EmojiSyncStartPayload;
import com.leclowndu93150.twemoji.network.SyncedEmoji;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class ServerEmojiSender {

    private static final AtomicLong SESSION_COUNTER = new AtomicLong();

    private ServerEmojiSender() {}

    public static void send(List<SyncedEmoji> emojis, Consumer<CustomPacketPayload> sink) {
        long sessionId = SESSION_COUNTER.incrementAndGet();
        sink.accept(new EmojiSyncStartPayload(sessionId, emojis.size()));

        for (int i = 0; i < emojis.size(); i++) {
            SyncedEmoji emoji = emojis.get(i);
            List<byte[]> parts = split(emoji.pngBytes(), EmojiSyncChunkPayload.MAX_PNG_PART_BYTES);
            for (int p = 0; p < parts.size(); p++) {
                sink.accept(new EmojiSyncChunkPayload(
                    sessionId,
                    i,
                    emoji.name(),
                    emoji.codepoint(),
                    emoji.animated(),
                    emoji.mcmetaJson(),
                    p,
                    parts.size(),
                    parts.get(p)
                ));
            }
        }

        sink.accept(new EmojiSyncEndPayload(sessionId));
    }

    private static List<byte[]> split(byte[] data, int maxSize) {
        if (data.length == 0) return List.of(new byte[0]);
        if (data.length <= maxSize) return List.of(data);
        List<byte[]> parts = new ArrayList<>();
        int offset = 0;
        while (offset < data.length) {
            int len = Math.min(maxSize, data.length - offset);
            byte[] part = new byte[len];
            System.arraycopy(data, offset, part, 0, len);
            parts.add(part);
            offset += len;
        }
        return parts;
    }
}
