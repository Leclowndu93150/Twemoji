package com.leclowndu93150.twemoji.network.payload;

import com.leclowndu93150.twemoji.Twemoji;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record EmojiSyncStartPayload(long sessionId, int totalEmojis, String categoryIconsJson) {

    public static final ResourceLocation ID = new ResourceLocation(Twemoji.MOD_ID, "emoji_sync_start");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarLong(sessionId);
        buf.writeVarInt(totalEmojis);
        buf.writeUtf(categoryIconsJson);
    }

    public static EmojiSyncStartPayload read(FriendlyByteBuf buf) {
        long sessionId = buf.readVarLong();
        int totalEmojis = buf.readVarInt();
        String categoryIconsJson = buf.readUtf();
        return new EmojiSyncStartPayload(sessionId, totalEmojis, categoryIconsJson);
    }
}
