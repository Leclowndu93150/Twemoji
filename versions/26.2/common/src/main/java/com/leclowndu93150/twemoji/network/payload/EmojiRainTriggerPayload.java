package com.leclowndu93150.twemoji.network.payload;

import com.leclowndu93150.twemoji.Twemoji;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EmojiRainTriggerPayload(String emojiName, int seconds) implements CustomPacketPayload {

    public static final Type<EmojiRainTriggerPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Twemoji.MOD_ID, "emoji_rain_trigger"));

    public static final StreamCodec<ByteBuf, EmojiRainTriggerPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, EmojiRainTriggerPayload::emojiName,
        ByteBufCodecs.VAR_INT, EmojiRainTriggerPayload::seconds,
        EmojiRainTriggerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
