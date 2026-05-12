package com.leclowndu93150.twemoji.client.render;

import com.leclowndu93150.twemoji.client.registry.AnimatedEmojiRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public sealed interface EmojiSprite {

    default void blit(GuiGraphicsExtractor graphics, int x, int y, int size) {
        switch (this) {
            case Sheet sheet -> graphics.blit(sheet.texture(), x, y, x + size, y + size, sheet.u0(), sheet.u1(), sheet.v0(), sheet.v1());
            case Custom custom -> graphics.blit(custom.texture(), x, y, x + size, y + size, 0.0F, 1.0F, 0.0F, 1.0F);
            case Animated animated -> {
                AnimatedEmojiRegistry.AnimatedEmoji emoji = AnimatedEmojiRegistry.INSTANCE.byCodepoint(animated.codepoint());
                if (emoji != null) graphics.blit(emoji.currentFrame(), x, y, x + size, y + size, 0.0F, 1.0F, 0.0F, 1.0F);
            }
        }
    }

    record Sheet(Identifier texture, int col, int row, int sheetW, int sheetH) implements EmojiSprite {
        public float u0() { return col * 18f / sheetW; }
        public float u1() { return (col + 1) * 18f / sheetW; }
        public float v0() { return row * 18f / sheetH; }
        public float v1() { return (row + 1) * 18f / sheetH; }
    }

    record Custom(Identifier texture) implements EmojiSprite {}

    record Animated(int codepoint) implements EmojiSprite {}
}
