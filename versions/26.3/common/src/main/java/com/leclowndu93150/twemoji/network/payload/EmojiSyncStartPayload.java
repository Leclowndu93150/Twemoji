package com.leclowndu93150.twemoji.network.payload;

import com.leclowndu93150.twemoji.Twemoji;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EmojiSyncStartPayload(long sessionId, int totalEmojis, String categoryIconsJson) implements CustomPacketPayload {

    public static final Type<EmojiSyncStartPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Twemoji.MOD_ID, "emoji_sync_start"));

    public static final StreamCodec<ByteBuf, EmojiSyncStartPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_LONG, EmojiSyncStartPayload::sessionId,
        ByteBufCodecs.VAR_INT, EmojiSyncStartPayload::totalEmojis,
        ByteBufCodecs.STRING_UTF8, EmojiSyncStartPayload::categoryIconsJson,
        EmojiSyncStartPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
