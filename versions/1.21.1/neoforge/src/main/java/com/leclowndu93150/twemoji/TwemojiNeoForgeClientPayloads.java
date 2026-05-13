package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.network.handler.ClientEmojiSync;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncStartPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TwemojiNeoForgeClientPayloads {

    private TwemojiNeoForgeClientPayloads() {}

    public static void register(PayloadRegistrar registrar) {
        IPayloadHandler<EmojiSyncStartPayload> start = (payload, ctx) -> ClientEmojiSync.INSTANCE.onStart(payload);
        IPayloadHandler<EmojiSyncChunkPayload> chunk = (payload, ctx) -> ClientEmojiSync.INSTANCE.onChunk(payload);
        IPayloadHandler<EmojiSyncEndPayload> end = (payload, ctx) -> ClientEmojiSync.INSTANCE.onEnd(payload);
        registrar.playToClient(EmojiSyncStartPayload.TYPE, EmojiSyncStartPayload.STREAM_CODEC, start);
        registrar.playToClient(EmojiSyncChunkPayload.TYPE, EmojiSyncChunkPayload.STREAM_CODEC, chunk);
        registrar.playToClient(EmojiSyncEndPayload.TYPE, EmojiSyncEndPayload.STREAM_CODEC, end);
    }
}
