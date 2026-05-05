package com.leclowndu93150.twemoji.mixin.client.chat;

import com.leclowndu93150.twemoji.client.EmojiConfig;
import com.leclowndu93150.twemoji.client.EmojiRegistry;
import com.leclowndu93150.twemoji.client.EmojiSuggestion;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public class CommandSuggestionsMixin {

    private static final int EMOJI_COL = 12;
    private static final int MAX_SUGGESTION_WIDTH = 120;

    @Inject(method = "updateCommandInfo", at = @At("TAIL"))
    private void twemoji$injectEmojiSuggestions(CallbackInfo ci) {
        CommandSuggestions self = (CommandSuggestions)(Object)this;
        CommandSuggestionsAccessor acc = (CommandSuggestionsAccessor) self;
        EditBox input = acc.getInput();
        String text = input.getValue();
        int cursor = input.getCursorPosition();
        if (cursor > text.length()) return;

        String beforeCursor = text.substring(0, cursor);
        int colonStart = -1;
        for (int i = beforeCursor.length() - 1; i >= 0; i--) {
            char c = beforeCursor.charAt(i);
            if (c == ':') { colonStart = i; break; }
            if (c == ' ') break;
        }
        if (colonStart == -1) return;

        String prefix = beforeCursor.substring(colonStart + 1);
        if (prefix.contains(":") || prefix.contains(" ")) return;

        int tone = EmojiConfig.get().getSkinTone();
        List<EmojiRegistry.EmojiEntry> matches = EmojiRegistry.INSTANCE.getSuggestions(prefix, tone);
        if (matches.isEmpty()) return;

        StringRange range = StringRange.between(colonStart, cursor);
        List<Suggestion> suggestions = new ArrayList<>(matches.size());
        for (EmojiRegistry.EmojiEntry match : matches) {
            suggestions.add(new EmojiSuggestion(range, match, tone));
        }

        Suggestions built = new Suggestions(range, suggestions);
        acc.setPendingSuggestions(CompletableFuture.completedFuture(built));
        self.showSuggestions(false);

        CommandSuggestions.SuggestionsList list = acc.getSuggestionsList();
        if (list != null) {
            SuggestionsListAccessor listAcc = (SuggestionsListAccessor) list;
            listAcc.getRect().setWidth(EMOJI_COL + MAX_SUGGESTION_WIDTH + 1);
        }
    }
}
