package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.client.EmojiConfig;
import com.leclowndu93150.twemoji.client.EmojiRegistry;
import com.leclowndu93150.twemoji.network.ClientEmojiSync;
import com.leclowndu93150.twemoji.network.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.EmojiSyncStartPayload;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class TwemojiFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EmojiConfig.init(FabricLoader.getInstance().getConfigDir());

        Identifier emojiRegistryId = Identifier.fromNamespaceAndPath(Twemoji.MOD_ID, "emoji_registry");
        ResourceLoader loader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        loader.registerReloadListener(emojiRegistryId, EmojiRegistry.INSTANCE);
        loader.addListenerOrdering(emojiRegistryId, ResourceReloaderKeys.Client.FONTS);

        ClientPlayNetworking.registerGlobalReceiver(EmojiSyncStartPayload.TYPE, (payload, ctx) ->
            ctx.client().execute(() -> ClientEmojiSync.INSTANCE.onStart(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(EmojiSyncChunkPayload.TYPE, (payload, ctx) ->
            ctx.client().execute(() -> ClientEmojiSync.INSTANCE.onChunk(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(EmojiSyncEndPayload.TYPE, (payload, ctx) ->
            ctx.client().execute(() -> ClientEmojiSync.INSTANCE.onEnd(payload))
        );

        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> ClientEmojiSync.INSTANCE.onDisconnect());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
            dispatcher.register(
                ClientCommands.literal("twemoji")
                    .then(ClientCommands.literal("skin")
                        .then(ClientCommands.argument("tone", IntegerArgumentType.integer(0, 5))
                            .executes(ctx -> {
                                int tone = IntegerArgumentType.getInteger(ctx, "tone");
                                EmojiConfig.get().setSkinTone(tone);
                                String msg = tone == 0 ? "Emoji skin tone reset to default." : "Emoji skin tone set to " + tone + ".";
                                ctx.getSource().sendFeedback(Component.literal(msg));
                                return 1;
                            })
                        )
                    )
            )
        );
    }
}
