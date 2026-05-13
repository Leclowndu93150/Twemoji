package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.server.EmojiUploadCommand;
import com.leclowndu93150.twemoji.server.ServerEmojiLoader;
import com.leclowndu93150.twemoji.server.ServerEmojiSender;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("twemoji")
public class TwemojiForge {

    public TwemojiForge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, event ->
            event.addListener(ServerEmojiLoader.INSTANCE)
        );

        MinecraftForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, event ->
            event.getPlayers().forEach(TwemojiForge::sendTo)
        );

        MinecraftForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
            event.getDispatcher().register(EmojiUploadCommand.build((server, player) -> sendTo(player)))
        );

        if (FMLEnvironment.dist == Dist.CLIENT) {
            TwemojiForgeClient.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TwemojiNetwork::register);
    }

    private static void sendTo(ServerPlayer player) {
        ServerEmojiSender.send(ServerEmojiLoader.INSTANCE.all(), ServerEmojiLoader.INSTANCE.categoryIcons(), payload -> TwemojiNetwork.sendTo(player, payload));
    }
}
