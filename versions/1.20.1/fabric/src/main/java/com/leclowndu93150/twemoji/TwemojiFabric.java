package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.client.command.EmojiExporter;
import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.picker.TwemojiKeyMappings;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import com.leclowndu93150.twemoji.client.render.EmojiRain;
import com.leclowndu93150.twemoji.client.suggestion.EmojiCommandSuggestions;
import com.leclowndu93150.twemoji.network.handler.ClientEmojiSync;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncChunkPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncEndPayload;
import com.leclowndu93150.twemoji.network.payload.EmojiSyncStartPayload;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.PackType;

import java.nio.file.Path;

public class TwemojiFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EmojiConfig.init(FabricLoader.getInstance().getConfigDir());
        KeyBindingHelper.registerKeyBinding(TwemojiKeyMappings.OPEN_PICKER);

        HudRenderCallback.EVENT.register((GuiGraphics graphics, float tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            EmojiRain.render(graphics, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
        });

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(TwemojiFabricRegistries.EMOJI_REGISTRY);

        ClientPlayNetworking.registerGlobalReceiver(EmojiSyncStartPayload.ID, (client, handler, buf, sender) -> {
            EmojiSyncStartPayload payload = EmojiSyncStartPayload.read(buf);
            client.execute(() -> ClientEmojiSync.INSTANCE.onStart(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(EmojiSyncChunkPayload.ID, (client, handler, buf, sender) -> {
            EmojiSyncChunkPayload payload = EmojiSyncChunkPayload.read(buf);
            client.execute(() -> ClientEmojiSync.INSTANCE.onChunk(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(EmojiSyncEndPayload.ID, (client, handler, buf, sender) -> {
            EmojiSyncEndPayload payload = EmojiSyncEndPayload.read(buf);
            client.execute(() -> ClientEmojiSync.INSTANCE.onEnd(payload));
        });

        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> ClientEmojiSync.INSTANCE.onConnect(currentServerHost(client)));
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> ClientEmojiSync.INSTANCE.onDisconnect());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
            dispatcher.register(
                ClientCommandManager.literal("twemoji")
                    .then(ClientCommandManager.literal("skin")
                        .then(ClientCommandManager.argument("tone", IntegerArgumentType.integer(0, 5))
                            .executes(ctx -> {
                                int tone = IntegerArgumentType.getInteger(ctx, "tone");
                                EmojiConfig.get().setSkinTone(tone);
                                Component msg = tone == 0
                                    ? Component.translatable("twemoji.command.skin.reset")
                                    : Component.translatable("twemoji.command.skin.set", tone);
                                ctx.getSource().sendFeedback(msg);
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommandManager.literal("rain")
                        .then(ClientCommandManager.argument("emoji", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> EmojiCommandSuggestions.suggest(builder))
                            .executes(ctx -> runRain(ctx.getSource(), StringArgumentType.getString(ctx, "emoji"), 8))
                        )
                        .then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(1, EmojiRain.MAX_DURATION_SECONDS))
                            .then(ClientCommandManager.argument("emoji", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> EmojiCommandSuggestions.suggest(builder))
                                .executes(ctx -> runRain(ctx.getSource(), StringArgumentType.getString(ctx, "emoji"), IntegerArgumentType.getInteger(ctx, "seconds")))
                            )
                        )
                    )
                    .then(ClientCommandManager.literal("button")
                        .then(ClientCommandManager.argument("visible", BoolArgumentType.bool())
                            .executes(ctx -> {
                                boolean visible = BoolArgumentType.getBool(ctx, "visible");
                                EmojiConfig.get().setPickerButtonVisible(visible);
                                ctx.getSource().sendFeedback(Component.translatable(visible ? "twemoji.command.button.shown" : "twemoji.command.button.hidden"));
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommandManager.literal("flags")
                        .then(ClientCommandManager.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> {
                                boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                EmojiConfig.get().setFlagsEnabled(enabled);
                                EmojiRegistry.INSTANCE.refreshShapingTable();
                                ctx.getSource().sendFeedback(Component.translatable(enabled ? "twemoji.command.flags.enabled" : "twemoji.command.flags.disabled"));
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommandManager.literal("render")
                        .then(ClientCommandManager.argument("size", IntegerArgumentType.integer(1, EmojiExporter.MAX_SIZE))
                            .then(ClientCommandManager.argument("emoji", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> EmojiCommandSuggestions.suggest(builder))
                                .executes(ctx -> runRender(ctx.getSource(), StringArgumentType.getString(ctx, "emoji"), IntegerArgumentType.getInteger(ctx, "size")))
                            )
                        )
                        .then(ClientCommandManager.argument("emoji", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> EmojiCommandSuggestions.suggest(builder))
                            .executes(ctx -> runRender(ctx.getSource(), StringArgumentType.getString(ctx, "emoji"), EmojiExporter.DEFAULT_SIZE))
                        )
                    )
            )
        );
    }

    private static int runRain(FabricClientCommandSource source, String input, int seconds) {
        EmojiRegistry.EmojiEntry entry = EmojiRegistry.INSTANCE.get(input.trim());
        if (entry == null) {
            source.sendError(Component.translatable("twemoji.command.rain.unknown_emoji", input));
            return 0;
        }
        EmojiRain.start(EmojiRegistry.INSTANCE.spriteForTone(entry, EmojiConfig.get().getSkinTone()), seconds);
        source.sendFeedback(Component.translatable("twemoji.command.rain.started", entry.shortcode(), seconds));
        return 1;
    }

    private static String currentServerHost(Minecraft client) {
        if (client.getCurrentServer() != null) return client.getCurrentServer().ip;
        if (client.hasSingleplayerServer()) return "_singleplayer";
        return "_unknown";
    }

    private static int runRender(FabricClientCommandSource source, String name, int size) {
        try {
            Path path = EmojiExporter.exportEmoji(name, size);
            Path absolute = path.toAbsolutePath();
            Component link = Component.literal(absolute.toString()).withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, absolute.toString()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("twemoji.command.render.link.hover"))));
            source.sendFeedback(Component.translatable("twemoji.command.render.success", link));
            return 1;
        } catch (Exception e) {
            source.sendError(Component.translatable("twemoji.command.render.failed", e.getMessage()));
            return 0;
        }
    }
}
