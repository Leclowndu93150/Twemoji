package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.client.suggestion.EmojiCommandSuggestions;
import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.command.EmojiExporter;
import com.leclowndu93150.twemoji.client.render.EmojiRain;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import com.leclowndu93150.twemoji.client.picker.TwemojiKeyMappings;
import com.leclowndu93150.twemoji.network.handler.ClientEmojiSync;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncStartPayload;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
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

        modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(TwemojiKeyMappings.OPEN_PICKER));

        modBus.addListener(RegisterClientPayloadHandlersEvent.class, event -> {
            event.register(EmojiSyncStartPayload.TYPE, (payload, ctx) -> ClientEmojiSync.INSTANCE.onStart(payload));
            event.register(EmojiSyncChunkPayload.TYPE, (payload, ctx) -> ClientEmojiSync.INSTANCE.onChunk(payload));
            event.register(EmojiSyncEndPayload.TYPE, (payload, ctx) -> ClientEmojiSync.INSTANCE.onEnd(payload));
        });

        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, event -> {
            Minecraft client = Minecraft.getInstance();
            EmojiRain.render(event.getGuiGraphics(), client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        });

        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> ClientEmojiSync.INSTANCE.onConnect(currentServerHost()));
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> ClientEmojiSync.INSTANCE.onDisconnect());

        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event ->
            event.getDispatcher().register(
                Commands.literal("twemoji")
                    .then(Commands.literal("skin")
                        .then(Commands.argument("tone", IntegerArgumentType.integer(0, 5))
                            .executes(ctx -> {
                                int tone = IntegerArgumentType.getInteger(ctx, "tone");
                                EmojiConfig.get().setSkinTone(tone);
                                Component msg = tone == 0
                                    ? Component.translatable("twemoji.command.skin.reset")
                                    : Component.translatable("twemoji.command.skin.set", tone);
                                ctx.getSource().sendSystemMessage(msg);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("rain")
                        .then(Commands.argument("emoji", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> EmojiCommandSuggestions.suggest(builder))
                            .executes(ctx -> runRain(ctx.getSource(), StringArgumentType.getString(ctx, "emoji"), 8))
                        )
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, EmojiRain.MAX_DURATION_SECONDS))
                            .then(Commands.argument("emoji", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> EmojiCommandSuggestions.suggest(builder))
                                .executes(ctx -> runRain(ctx.getSource(), StringArgumentType.getString(ctx, "emoji"), IntegerArgumentType.getInteger(ctx, "seconds")))
                            )
                        )
                    )
                    .then(Commands.literal("button")
                        .then(Commands.argument("visible", BoolArgumentType.bool())
                            .executes(ctx -> {
                                boolean visible = BoolArgumentType.getBool(ctx, "visible");
                                EmojiConfig.get().setPickerButtonVisible(visible);
                                ctx.getSource().sendSystemMessage(Component.translatable(visible ? "twemoji.command.button.shown" : "twemoji.command.button.hidden"));
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

    private static int runRain(CommandSourceStack source, String input, int seconds) {
        EmojiRegistry.EmojiEntry entry = EmojiRegistry.INSTANCE.get(input.trim());
        if (entry == null) {
            source.sendSystemMessage(Component.translatable("twemoji.command.rain.unknown_emoji", input));
            return 0;
        }
        EmojiRain.start(EmojiRegistry.INSTANCE.spriteForTone(entry, EmojiConfig.get().getSkinTone()), seconds);
        source.sendSystemMessage(Component.translatable("twemoji.command.rain.started", entry.shortcode(), seconds));
        return 1;
    }

    private static String currentServerHost() {
        Minecraft client = Minecraft.getInstance();
        if (client.getCurrentServer() != null) return client.getCurrentServer().ip;
        if (client.hasSingleplayerServer()) return "_singleplayer";
        return "_unknown";
    }

    private static int runRender(CommandSourceStack source, String name, int size) {
        try {
            Path path = EmojiExporter.exportEmoji(name, size);
            Path absolute = path.toAbsolutePath();
            Component link = Component.literal(absolute.toString()).withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenFile(absolute))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("twemoji.command.render.link.hover"))));
            source.sendSystemMessage(Component.translatable("twemoji.command.render.success", link));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.translatable("twemoji.command.render.failed", e.getMessage()));
            return 0;
        }
    }
}
