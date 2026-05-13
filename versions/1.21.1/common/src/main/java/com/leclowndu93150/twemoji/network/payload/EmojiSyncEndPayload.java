package com.leclowndu93150.twemoji.network.payload;

import com.leclowndu93150.twemoji.Twemoji;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EmojiSyncEndPayload(long sessionId) implements CustomPacketPayload {

    public static final Type<EmojiSyncEndPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Twemoji.MOD_ID, "emoji_sync_end"));

    public static final StreamCodec<ByteBuf, EmojiSyncEndPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG, EmojiSyncEndPayload::sessionId,
        EmojiSyncEndPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
