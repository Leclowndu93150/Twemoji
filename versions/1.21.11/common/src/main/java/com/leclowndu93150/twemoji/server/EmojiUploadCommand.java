package com.leclowndu93150.twemoji.server;

import com.leclowndu93150.twemoji.network.payload.SyncedEmoji;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

public final class EmojiUploadCommand {

    public static final String UPLOAD_DIR = "twemoji-uploads";

    private EmojiUploadCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build(BiConsumer<MinecraftServer, ServerPlayer> resyncPlayer) {
        return Commands.literal("emoji")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
            .then(Commands.literal("upload")
                .then(Commands.argument("name", StringArgumentType.word())
                    .then(Commands.argument("file", StringArgumentType.string())
                        .executes(ctx -> runUpload(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "name"),
                            StringArgumentType.getString(ctx, "file"),
                            resyncPlayer
                        ))
                    )
                )
            );
    }

    private static int runUpload(CommandSourceStack source, String name, String file, BiConsumer<MinecraftServer, ServerPlayer> resyncPlayer) {
        MinecraftServer server = source.getServer();
        Path uploadDir = server.getServerDirectory().resolve(UPLOAD_DIR);
        Path target = uploadDir.resolve(file).normalize();
        if (!target.startsWith(uploadDir.normalize())) {
            source.sendSystemMessage(Component.translatable("twemoji.command.upload.failed", "invalid path"));
            return 0;
        }
        try {
            if (!Files.exists(target)) {
                source.sendSystemMessage(Component.translatable("twemoji.command.upload.no_file", target.toString()));
                return 0;
            }
            byte[] bytes = Files.readAllBytes(target);
            SyncedEmoji entry = ServerEmojiLoader.INSTANCE.addOrReplaceStatic(name, bytes, "", List.of());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                resyncPlayer.accept(server, player);
            }
            source.sendSystemMessage(Component.translatable("twemoji.command.upload.success", entry.name()));
            return 1;
        } catch (Exception e) {
            source.sendSystemMessage(Component.translatable("twemoji.command.upload.failed", e.getMessage()));
            return 0;
        }
    }
}
