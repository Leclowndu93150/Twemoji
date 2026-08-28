package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.network.payload.EmojiRainTriggerPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncStartPayload;
import com.leclowndu93150.twemoji.server.EmojiRainCommand;
import com.leclowndu93150.twemoji.server.EmojiUploadCommand;
import com.leclowndu93150.twemoji.server.ServerEmojiLoader;
import com.leclowndu93150.twemoji.server.ServerEmojiSender;
import com.leclowndu93150.twemoji.client.TwemojiClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;

public class TwemojiFabricCommon implements ModInitializer {

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            TwemojiClient.init(FabricLoader.getInstance().getConfigDir());
        }

        PayloadTypeRegistry.playS2C().register(EmojiSyncStartPayload.TYPE, EmojiSyncStartPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EmojiSyncChunkPayload.TYPE, EmojiSyncChunkPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EmojiSyncEndPayload.TYPE, EmojiSyncEndPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(EmojiRainTriggerPayload.TYPE, EmojiRainTriggerPayload.STREAM_CODEC);

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(TwemojiFabricRegistries.SERVER_EMOJI_LOADER);

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
