package com.leclowndu93150.twemoji.client.glyph;

import net.minecraft.resources.Identifier;

public sealed interface EmojiSprite {

    record Sheet(Identifier texture, int col, int row, int sheetW, int sheetH) implements EmojiSprite {
        public float u0() { return col * 18f / sheetW; }
        public float u1() { return (col + 1) * 18f / sheetW; }
        public float v0() { return row * 18f / sheetH; }
        public float v1() { return (row + 1) * 18f / sheetH; }
    }

    record Custom(Identifier texture) implements EmojiSprite {}

    record Animated(int codepoint) implements EmojiSprite {}
}
