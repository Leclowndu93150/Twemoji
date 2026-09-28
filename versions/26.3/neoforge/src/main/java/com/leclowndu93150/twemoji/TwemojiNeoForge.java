package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.network.payload.EmojiRainTriggerPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncStartPayload;
import com.leclowndu93150.twemoji.server.EmojiRainCommand;
import com.leclowndu93150.twemoji.server.EmojiUploadCommand;
import com.leclowndu93150.twemoji.server.ServerEmojiLoader;
import com.leclowndu93150.twemoji.server.ServerEmojiSender;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod("twemoji")
public class TwemojiNeoForge {

    public TwemojiNeoForge(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, this::registerPayloads);

        NeoForge.EVENT_BUS.addListener(AddServerReloadListenersEvent.class, event ->
            event.addListener(Identifier.fromNamespaceAndPath(Twemoji.MOD_ID, "emoji_data"), ServerEmojiLoader.INSTANCE)
        );

        NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, event ->
            event.getRelevantPlayers().forEach(TwemojiNeoForge::sendTo)
        );

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> {
            event.getDispatcher().register(EmojiUploadCommand.build((server, player) -> sendTo(player)));
            event.getDispatcher().register(EmojiRainCommand.build((player, payload) -> PacketDistributor.sendToPlayer(player, payload)));
        });

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            TwemojiNeoForgeClient.init(modBus);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(EmojiSyncStartPayload.TYPE, EmojiSyncStartPayload.STREAM_CODEC);
        registrar.playToClient(EmojiSyncChunkPayload.TYPE, EmojiSyncChunkPayload.STREAM_CODEC);
        registrar.playToClient(EmojiSyncEndPayload.TYPE, EmojiSyncEndPayload.STREAM_CODEC);
        registrar.playToClient(EmojiRainTriggerPayload.TYPE, EmojiRainTriggerPayload.STREAM_CODEC);
    }

    private static void sendTo(ServerPlayer player) {
        ServerEmojiSender.send(ServerEmojiLoader.INSTANCE.all(), ServerEmojiLoader.INSTANCE.categoryIcons(), payload -> PacketDistributor.sendToPlayer(player, payload));
    }
}
