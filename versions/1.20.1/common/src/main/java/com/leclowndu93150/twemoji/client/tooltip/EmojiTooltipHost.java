package com.leclowndu93150.twemoji.client.tooltip;

import org.jetbrains.annotations.Nullable;

public interface EmojiTooltipHost {
    @Nullable
    EmojiTooltip.Hit twemoji$emojiTooltipHit(int mouseX, int mouseY);
}
