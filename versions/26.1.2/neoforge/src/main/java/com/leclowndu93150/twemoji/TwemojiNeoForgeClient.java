package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.client.EmojiConfig;
import com.leclowndu93150.twemoji.client.EmojiRegistry;
import com.leclowndu93150.twemoji.network.ClientEmojiSync;
import com.leclowndu93150.twemoji.network.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.EmojiSyncStartPayload;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;
import net.neoforged.neoforge.common.NeoForge;

public final class TwemojiNeoForgeClient {

    private TwemojiNeoForgeClient() {}

    public static void init(IEventBus modBus) {
        EmojiConfig.init(FMLPaths.CONFIGDIR.get());

        Identifier emojiRegistryId = Identifier.fromNamespaceAndPath(Twemoji.MOD_ID, "emoji_registry");
        modBus.addListener(AddClientReloadListenersEvent.class, event -> {
            event.addListener(emojiRegistryId, EmojiRegistry.INSTANCE);
            event.addDependency(VanillaClientListeners.FONTS, emojiRegistryId);
        });

        modBus.addListener(RegisterClientPayloadHandlersEvent.class, event -> {
            event.register(EmojiSyncStartPayload.TYPE, (payload, ctx) -> ClientEmojiSync.INSTANCE.onStart(payload));
            event.register(EmojiSyncChunkPayload.TYPE, (payload, ctx) -> ClientEmojiSync.INSTANCE.onChunk(payload));
            event.register(EmojiSyncEndPayload.TYPE, (payload, ctx) -> ClientEmojiSync.INSTANCE.onEnd(payload));
        });

        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> ClientEmojiSync.INSTANCE.onDisconnect());

        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event ->
            event.getDispatcher().register(
                Commands.literal("twemoji")
                    .then(Commands.literal("skin")
                        .then(Commands.argument("tone", IntegerArgumentType.integer(0, 5))
                            .executes(ctx -> {
                                int tone = IntegerArgumentType.getInteger(ctx, "tone");
                                EmojiConfig.get().setSkinTone(tone);
                                String msg = tone == 0 ? "Emoji skin tone reset to default." : "Emoji skin tone set to " + tone + ".";
                                ctx.getSource().sendSystemMessage(Component.literal(msg));
                                return 1;
                            })
                        )
                    )
            )
        );
    }
}
