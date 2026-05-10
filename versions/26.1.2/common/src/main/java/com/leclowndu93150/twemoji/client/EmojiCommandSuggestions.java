package com.leclowndu93150.twemoji.client;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class EmojiCommandSuggestions {

    private EmojiCommandSuggestions() {}

    public static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        int tone = EmojiConfig.get().getSkinTone();
        StringRange range = StringRange.between(builder.getStart(), builder.getStart() + builder.getRemaining().length());

        Map<String, EmojiRegistry.EmojiEntry> entries = EmojiRegistry.INSTANCE.getEntries();
        List<Suggestion> starts = new ArrayList<>();
        List<Suggestion> contains = new ArrayList<>();
        for (Map.Entry<String, EmojiRegistry.EmojiEntry> entry : entries.entrySet()) {
            String name = entry.getKey();
            String lower = name.toLowerCase(Locale.ROOT);
            if (EmojiRegistry.isToneVariant(entry.getValue())) continue;
            if (lower.startsWith(remaining)) {
                starts.add(new EmojiSuggestion(range, entry.getValue(), tone, name));
            } else if (!remaining.isEmpty() && lower.contains(remaining)) {
                contains.add(new EmojiSuggestion(range, entry.getValue(), tone, name));
            }
        }
        starts.addAll(contains);
        return CompletableFuture.completedFuture(new Suggestions(range, starts));
    }
}
