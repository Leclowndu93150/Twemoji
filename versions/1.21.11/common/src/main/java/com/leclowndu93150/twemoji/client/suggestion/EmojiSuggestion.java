package com.leclowndu93150.twemoji.client.suggestion;

import com.leclowndu93150.twemoji.client.render.EmojiSprite;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;

public class EmojiSuggestion extends Suggestion {

    private final EmojiRegistry.EmojiEntry entry;
    private final int skinTone;

    public EmojiSuggestion(StringRange range, EmojiRegistry.EmojiEntry entry, int skinTone) {
        this(range, entry, skinTone, entry.shortcode());
    }

    public EmojiSuggestion(StringRange range, EmojiRegistry.EmojiEntry entry, int skinTone, String text) {
        super(range, text);
        this.entry = entry;
        this.skinTone = skinTone;
    }

    public EmojiSprite getSprite() {
        return EmojiRegistry.INSTANCE.spriteForTone(entry, skinTone);
    }
}
