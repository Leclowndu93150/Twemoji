package com.leclowndu93150.twemoji.client.tooltip;

import org.jspecify.annotations.Nullable;

public interface EmojiTooltipHost {
    EmojiTooltip.@Nullable Hit twemoji$emojiTooltipHit(int mouseX, int mouseY);
}
