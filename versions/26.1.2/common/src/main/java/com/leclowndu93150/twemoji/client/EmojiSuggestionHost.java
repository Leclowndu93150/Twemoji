package com.leclowndu93150.twemoji.client;

public interface EmojiSuggestionHost {
    EmojiSuggestions twemoji$suggestions();

    void twemoji$applyEmojiSuggestion(EmojiSuggestions.Replacement replacement);
}
