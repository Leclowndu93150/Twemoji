package com.leclowndu93150.twemoji.network.payload;

import com.leclowndu93150.twemoji.Twemoji;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record EmojiRainTriggerPayload(String emojiName, int seconds) {

    public static final ResourceLocation ID = new ResourceLocation(Twemoji.MOD_ID, "emoji_rain_trigger");

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(emojiName);
        buf.writeVarInt(seconds);
    }

    public static EmojiRainTriggerPayload read(FriendlyByteBuf buf) {
        String emojiName = buf.readUtf();
        int seconds = buf.readVarInt();
        return new EmojiRainTriggerPayload(emojiName, seconds);
    }
}
