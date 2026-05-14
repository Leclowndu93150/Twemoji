package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.network.handler.ClientEmojiSync;
import com.leclowndu93150.twemoji.network.payload.EmojiRainTriggerPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncStartPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class TwemojiNetwork {

    public static final String PROTOCOL_VERSION = "1";
    public static final ResourceLocation CHANNEL = new ResourceLocation(Twemoji.MOD_ID, "main");

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        CHANNEL,
        () -> PROTOCOL_VERSION,
        version -> PROTOCOL_VERSION.equals(version) || NetworkRegistry.ACCEPTVANILLA.equals(version) || NetworkRegistry.ABSENT.equals(version),
        version -> PROTOCOL_VERSION.equals(version) || NetworkRegistry.ACCEPTVANILLA.equals(version) || NetworkRegistry.ABSENT.equals(version)
    );

    private TwemojiNetwork() {}

    public static void register() {
        INSTANCE.messageBuilder(EmojiSyncStartPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(EmojiSyncStartPayload::write)
            .decoder(EmojiSyncStartPayload::read)
            .consumerNetworkThread(TwemojiNetwork::handleStart)
            .add();
        INSTANCE.messageBuilder(EmojiSyncChunkPayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(EmojiSyncChunkPayload::write)
            .decoder(EmojiSyncChunkPayload::read)
            .consumerNetworkThread(TwemojiNetwork::handleChunk)
            .add();
        INSTANCE.messageBuilder(EmojiSyncEndPayload.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(EmojiSyncEndPayload::write)
            .decoder(EmojiSyncEndPayload::read)
            .consumerNetworkThread(TwemojiNetwork::handleEnd)
            .add();
        INSTANCE.messageBuilder(EmojiRainTriggerPayload.class, 3, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(EmojiRainTriggerPayload::write)
            .decoder(EmojiRainTriggerPayload::read)
            .consumerNetworkThread(TwemojiNetwork::handleRainTrigger)
            .add();
    }

    private static void handleStart(EmojiSyncStartPayload payload, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientEmojiSync.INSTANCE.onStart(payload));
        }
        context.setPacketHandled(true);
    }

    private static void handleChunk(EmojiSyncChunkPayload payload, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientEmojiSync.INSTANCE.onChunk(payload));
        }
        context.setPacketHandled(true);
    }

    private static void handleEnd(EmojiSyncEndPayload payload, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientEmojiSync.INSTANCE.onEnd(payload));
        }
        context.setPacketHandled(true);
    }

    private static void handleRainTrigger(EmojiRainTriggerPayload payload, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientEmojiSync.INSTANCE.onRainTrigger(payload));
        }
        context.setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player, Object payload) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
