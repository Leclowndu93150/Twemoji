package com.leclowndu93150.twemoji.server;

import com.leclowndu93150.twemoji.network.payload.EmojiRainTriggerPayload;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Collection;
import java.util.function.BiConsumer;

public final class EmojiRainCommand {

    public static final int DEFAULT_SECONDS = 8;
    public static final int MAX_SECONDS = 60;
    public static final String OWNER_USERNAME = "Leclowndu93150";

    private static final SimpleCommandExceptionType ERROR_NO_PERMISSION = new SimpleCommandExceptionType(Component.translatable("twemoji.command.rain.no_permission"));

    private EmojiRainCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build(BiConsumer<ServerPlayer, EmojiRainTriggerPayload> sender) {
        return Commands.literal("twemoji")
            .then(Commands.literal("rain")
                .then(Commands.argument("emoji", StringArgumentType.word())
                    .executes(ctx -> runSelf(ctx, StringArgumentType.getString(ctx, "emoji"), DEFAULT_SECONDS, sender))
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(1, MAX_SECONDS))
                        .executes(ctx -> runSelf(ctx, StringArgumentType.getString(ctx, "emoji"), IntegerArgumentType.getInteger(ctx, "seconds"), sender))
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(ctx -> runTargets(ctx, EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "emoji"), IntegerArgumentType.getInteger(ctx, "seconds"), sender))
                        )
                    )
                )
            );
    }

    private static int runSelf(CommandContext<CommandSourceStack> ctx, String emoji, int seconds, BiConsumer<ServerPlayer, EmojiRainTriggerPayload> sender) throws CommandSyntaxException {
        ServerPlayer self = ctx.getSource().getPlayerOrException();
        sender.accept(self, new EmojiRainTriggerPayload(emoji, seconds));
        ctx.getSource().sendSystemMessage(Component.translatable("twemoji.command.rain.started", emoji, seconds));
        return 1;
    }

    private static int runTargets(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, String emoji, int seconds, BiConsumer<ServerPlayer, EmojiRainTriggerPayload> sender) throws CommandSyntaxException {
        if (!isAuthorized(ctx.getSource())) throw ERROR_NO_PERMISSION.create();
        EmojiRainTriggerPayload payload = new EmojiRainTriggerPayload(emoji, seconds);
        for (ServerPlayer target : targets) {
            sender.accept(target, payload);
        }
        ctx.getSource().sendSystemMessage(Component.translatable("twemoji.command.rain.triggered", emoji, targets.size()));
        return targets.size();
    }

    private static boolean isAuthorized(CommandSourceStack source) {
        if (source.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) return true;
        return source.getEntity() instanceof ServerPlayer player && OWNER_USERNAME.equals(player.getGameProfile().name());
    }
}
