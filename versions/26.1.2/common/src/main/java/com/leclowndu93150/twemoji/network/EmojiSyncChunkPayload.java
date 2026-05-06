package com.leclowndu93150.twemoji.network;

import com.leclowndu93150.twemoji.Twemoji;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EmojiSyncChunkPayload(
    long sessionId,
    int emojiIndex,
    String name,
    int codepoint,
    boolean animated,
    String mcmetaJson,
    int pngPartIndex,
    int pngPartTotal,
    byte[] pngBytes,
    String category,
    String aliases
) implements CustomPacketPayload {

    public static final int MAX_PNG_PART_BYTES = 768 * 1024;

    public static final Type<EmojiSyncChunkPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Twemoji.MOD_ID, "emoji_sync_chunk"));

    public static final StreamCodec<ByteBuf, EmojiSyncChunkPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG, EmojiSyncChunkPayload::sessionId,
        ByteBufCodecs.VAR_INT, EmojiSyncChunkPayload::emojiIndex,
        ByteBufCodecs.STRING_UTF8, EmojiSyncChunkPayload::name,
        ByteBufCodecs.VAR_INT, EmojiSyncChunkPayload::codepoint,
        ByteBufCodecs.BOOL, EmojiSyncChunkPayload::animated,
        ByteBufCodecs.STRING_UTF8, EmojiSyncChunkPayload::mcmetaJson,
        ByteBufCodecs.VAR_INT, EmojiSyncChunkPayload::pngPartIndex,
        ByteBufCodecs.VAR_INT, EmojiSyncChunkPayload::pngPartTotal,
        ByteBufCodecs.byteArray(MAX_PNG_PART_BYTES), EmojiSyncChunkPayload::pngBytes,
        ByteBufCodecs.STRING_UTF8, EmojiSyncChunkPayload::category,
        ByteBufCodecs.STRING_UTF8, EmojiSyncChunkPayload::aliases,
        EmojiSyncChunkPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
