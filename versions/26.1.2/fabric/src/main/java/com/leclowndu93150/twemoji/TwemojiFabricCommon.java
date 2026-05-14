package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.network.payload.EmojiRainTriggerPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncStartPayload;
import com.leclowndu93150.twemoji.server.EmojiRainCommand;
import com.leclowndu93150.twemoji.server.EmojiUploadCommand;
import com.leclowndu93150.twemoji.server.ServerEmojiLoader;
import com.leclowndu93150.twemoji.server.ServerEmojiSender;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
        PayloadTypeRegistry.clientboundPlay().register(EmojiRainTriggerPayload.TYPE, EmojiRainTriggerPayload.STREAM_CODEC);

        Identifier loaderId = Identifier.fromNamespaceAndPath(Twemoji.MOD_ID, "emoji_data");
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(loaderId, ServerEmojiLoader.INSTANCE);

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> sendTo(player));

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            dispatcher.register(EmojiUploadCommand.build((server, player) -> sendTo(player)));
            dispatcher.register(EmojiRainCommand.build(ServerPlayNetworking::send));
        });
    }

    private static void sendTo(ServerPlayer player) {
        ServerEmojiSender.send(ServerEmojiLoader.INSTANCE.all(), ServerEmojiLoader.INSTANCE.categoryIcons(), payload -> ServerPlayNetworking.send(player, payload));
    }

    public static void sendToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sendTo(player);
    }
}
