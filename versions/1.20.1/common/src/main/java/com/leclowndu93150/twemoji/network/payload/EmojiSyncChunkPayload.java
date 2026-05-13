package com.leclowndu93150.twemoji.network.payload;

import com.leclowndu93150.twemoji.Twemoji;
import net.minecraft.network.FriendlyByteBuf;
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
) {

    public static final int MAX_PNG_PART_BYTES = 768 * 1024;

    public static final ResourceLocation ID = new ResourceLocation(Twemoji.MOD_ID, "emoji_sync_chunk");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarLong(sessionId);
        buf.writeVarInt(emojiIndex);
        buf.writeUtf(name);
        buf.writeVarInt(codepoint);
        buf.writeBoolean(animated);
        buf.writeUtf(mcmetaJson);
        buf.writeVarInt(pngPartIndex);
        buf.writeVarInt(pngPartTotal);
        buf.writeByteArray(pngBytes);
        buf.writeUtf(category);
        buf.writeUtf(aliases);
    }

    public static EmojiSyncChunkPayload read(FriendlyByteBuf buf) {
        long sessionId = buf.readVarLong();
        int emojiIndex = buf.readVarInt();
        String name = buf.readUtf();
        int codepoint = buf.readVarInt();
        boolean animated = buf.readBoolean();
        String mcmetaJson = buf.readUtf();
        int pngPartIndex = buf.readVarInt();
        int pngPartTotal = buf.readVarInt();
        byte[] pngBytes = buf.readByteArray(MAX_PNG_PART_BYTES);
        String category = buf.readUtf();
        String aliases = buf.readUtf();
        return new EmojiSyncChunkPayload(sessionId, emojiIndex, name, codepoint, animated, mcmetaJson,
            pngPartIndex, pngPartTotal, pngBytes, category, aliases);
    }
}
