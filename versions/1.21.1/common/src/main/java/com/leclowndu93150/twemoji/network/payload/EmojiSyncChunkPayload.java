package com.leclowndu93150.twemoji.network.payload;

import com.leclowndu93150.twemoji.Twemoji;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

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

    public static final Type<EmojiSyncChunkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Twemoji.MOD_ID, "emoji_sync_chunk"));

    public static final StreamCodec<ByteBuf, EmojiSyncChunkPayload> STREAM_CODEC = new StreamCodec<>() {
        private final StreamCodec<ByteBuf, byte[]> byteArrayCodec = ByteBufCodecs.byteArray(MAX_PNG_PART_BYTES);

        @Override
        public EmojiSyncChunkPayload decode(ByteBuf buf) {
            long sessionId = ByteBufCodecs.VAR_LONG.decode(buf);
            int emojiIndex = ByteBufCodecs.VAR_INT.decode(buf);
            String name = ByteBufCodecs.STRING_UTF8.decode(buf);
            int codepoint = ByteBufCodecs.VAR_INT.decode(buf);
            boolean animated = ByteBufCodecs.BOOL.decode(buf);
            String mcmetaJson = ByteBufCodecs.STRING_UTF8.decode(buf);
            int pngPartIndex = ByteBufCodecs.VAR_INT.decode(buf);
            int pngPartTotal = ByteBufCodecs.VAR_INT.decode(buf);
            byte[] pngBytes = byteArrayCodec.decode(buf);
            String category = ByteBufCodecs.STRING_UTF8.decode(buf);
            String aliases = ByteBufCodecs.STRING_UTF8.decode(buf);
            return new EmojiSyncChunkPayload(sessionId, emojiIndex, name, codepoint, animated, mcmetaJson,
                pngPartIndex, pngPartTotal, pngBytes, category, aliases);
        }

        @Override
        public void encode(ByteBuf buf, EmojiSyncChunkPayload payload) {
            ByteBufCodecs.VAR_LONG.encode(buf, payload.sessionId);
            ByteBufCodecs.VAR_INT.encode(buf, payload.emojiIndex);
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.name);
            ByteBufCodecs.VAR_INT.encode(buf, payload.codepoint);
            ByteBufCodecs.BOOL.encode(buf, payload.animated);
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.mcmetaJson);
            ByteBufCodecs.VAR_INT.encode(buf, payload.pngPartIndex);
            ByteBufCodecs.VAR_INT.encode(buf, payload.pngPartTotal);
            byteArrayCodec.encode(buf, payload.pngBytes);
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.category);
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.aliases);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
