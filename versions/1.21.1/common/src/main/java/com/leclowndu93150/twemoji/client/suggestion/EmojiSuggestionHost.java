package com.leclowndu93150.twemoji.client.suggestion;

public interface EmojiSuggestionHost {
    EmojiSuggestions twemoji$suggestions();

    void twemoji$applyEmojiSuggestion(EmojiSuggestions.Replacement replacement);
}
