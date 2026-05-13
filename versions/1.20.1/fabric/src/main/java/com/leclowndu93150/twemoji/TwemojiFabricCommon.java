package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.network.payload.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncStartPayload;
import com.leclowndu93150.twemoji.server.EmojiUploadCommand;
import com.leclowndu93150.twemoji.server.ServerEmojiLoader;
import com.leclowndu93150.twemoji.server.ServerEmojiSender;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;

public class TwemojiFabricCommon implements ModInitializer {

    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(TwemojiFabricRegistries.SERVER_EMOJI_LOADER);

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> sendTo(player));

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
            dispatcher.register(EmojiUploadCommand.build((server, player) -> sendTo(player)))
        );
    }

    private static void sendTo(ServerPlayer player) {
        ServerEmojiSender.send(ServerEmojiLoader.INSTANCE.all(), ServerEmojiLoader.INSTANCE.categoryIcons(), payload -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            ResourceLocation id;
            if (payload instanceof EmojiSyncStartPayload p) {
                id = EmojiSyncStartPayload.ID;
                p.write(buf);
            } else if (payload instanceof EmojiSyncChunkPayload p) {
                id = EmojiSyncChunkPayload.ID;
                p.write(buf);
            } else if (payload instanceof EmojiSyncEndPayload p) {
                id = EmojiSyncEndPayload.ID;
                p.write(buf);
            } else {
                return;
            }
            ServerPlayNetworking.send(player, id, buf);
        });
    }

    public static void sendToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sendTo(player);
    }
}
