package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.network.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.EmojiSyncStartPayload;
import com.leclowndu93150.twemoji.server.ServerEmojiLoader;
import com.leclowndu93150.twemoji.server.ServerEmojiSender;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;

public class TwemojiFabricCommon implements ModInitializer {

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(EmojiSyncStartPayload.TYPE, EmojiSyncStartPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EmojiSyncChunkPayload.TYPE, EmojiSyncChunkPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EmojiSyncEndPayload.TYPE, EmojiSyncEndPayload.STREAM_CODEC);

        Identifier loaderId = Identifier.fromNamespaceAndPath(Twemoji.MOD_ID, "emoji_data");
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(loaderId, ServerEmojiLoader.INSTANCE);

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> sendTo(player));
    }

    private static void sendTo(ServerPlayer player) {
        ServerEmojiSender.send(ServerEmojiLoader.INSTANCE.all(), payload -> ServerPlayNetworking.send(player, payload));
    }

    public static void sendToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sendTo(player);
    }
}
