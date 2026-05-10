package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.client.EmojiCommandSuggestions;
import com.leclowndu93150.twemoji.client.EmojiConfig;
import com.leclowndu93150.twemoji.client.EmojiExporter;
import com.leclowndu93150.twemoji.client.EmojiRegistry;
import com.leclowndu93150.twemoji.network.ClientEmojiSync;
import com.leclowndu93150.twemoji.network.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.EmojiSyncStartPayload;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
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
                    .then(Commands.literal("render")
                        .then(Commands.argument("size", IntegerArgumentType.integer(1, EmojiExporter.MAX_SIZE))
                            .then(Commands.argument("emoji", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> EmojiCommandSuggestions.suggest(builder))
                                .executes(ctx -> runRender(ctx.getSource(), StringArgumentType.getString(ctx, "emoji"), IntegerArgumentType.getInteger(ctx, "size")))
                            )
                        )
                        .then(Commands.argument("emoji", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> EmojiCommandSuggestions.suggest(builder))
                            .executes(ctx -> runRender(ctx.getSource(), StringArgumentType.getString(ctx, "emoji"), EmojiExporter.DEFAULT_SIZE))
                        )
                    )
            )
        );
    }

    private static int runRender(CommandSourceStack source, String name, int size) {
        try {
            Path path = EmojiExporter.exportEmoji(name, size);
            Path absolute = path.toAbsolutePath();
            Component link = Component.literal(absolute.toString()).withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenFile(absolute))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open"))));
            source.sendSystemMessage(Component.literal("Rendered emoji to ").append(link));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.literal("Failed to render emoji: " + e.getMessage()));
            return 0;
        }
    }
}
