package com.leclowndu93150.twemoji.client;

import com.leclowndu93150.twemoji.client.glyph.EmojiSprite;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;

public class EmojiSuggestion extends Suggestion {

    private final EmojiRegistry.EmojiEntry entry;
    private final int skinTone;

    public EmojiSuggestion(StringRange range, EmojiRegistry.EmojiEntry entry, int skinTone) {
        super(range, entry.shortcode());
        this.entry = entry;
        this.skinTone = skinTone;
    }

    public EmojiSprite getSprite() {
        return EmojiRegistry.INSTANCE.spriteForTone(entry, skinTone);
    }
}
