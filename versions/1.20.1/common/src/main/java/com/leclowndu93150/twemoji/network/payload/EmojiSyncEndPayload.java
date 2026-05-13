package com.leclowndu93150.twemoji.network.payload;

import com.leclowndu93150.twemoji.Twemoji;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record EmojiSyncEndPayload(long sessionId) {

    public static final ResourceLocation ID = new ResourceLocation(Twemoji.MOD_ID, "emoji_sync_end");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarLong(sessionId);
    }

    public static EmojiSyncEndPayload read(FriendlyByteBuf buf) {
        return new EmojiSyncEndPayload(buf.readVarLong());
    }
}
