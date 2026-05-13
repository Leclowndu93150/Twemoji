package com.leclowndu93150.twemoji.client.render;

import com.leclowndu93150.twemoji.client.registry.AnimatedEmojiRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public sealed interface EmojiSprite {

    default void blit(GuiGraphics graphics, int x, int y, int size) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            switch (this) {
                case Sheet sheet -> {
                    int sourceW = (int) Math.round((sheet.u1() - sheet.u0()) * sheet.sheetW());
                    int sourceH = (int) Math.round((sheet.v1() - sheet.v0()) * sheet.sheetH());
                    graphics.blit(
                        sheet.texture(),
                        x, y, size, size,
                        sheet.u0() * sheet.sheetW(),
                        sheet.v0() * sheet.sheetH(),
                        sourceW, sourceH,
                        sheet.sheetW(), sheet.sheetH()
                    );
                }
                case Custom custom -> graphics.blit(custom.texture(), x, y, size, size, 0.0F, 0.0F, size, size, size, size);
                case Animated animated -> {
                    AnimatedEmojiRegistry.AnimatedEmoji emoji = AnimatedEmojiRegistry.INSTANCE.byCodepoint(animated.codepoint());
                    if (emoji != null) graphics.blit(emoji.currentFrame(), x, y, size, size, 0.0F, 0.0F, size, size, size, size);
                }
            }
        } finally {
            RenderSystem.disableBlend();
        }
    }

    record Sheet(ResourceLocation texture, int col, int row, int sheetW, int sheetH) implements EmojiSprite {
        public float u0() { return col * 18f / sheetW; }
        public float u1() { return (col + 1) * 18f / sheetW; }
        public float v0() { return row * 18f / sheetH; }
        public float v1() { return (row + 1) * 18f / sheetH; }
    }

    record Custom(ResourceLocation texture) implements EmojiSprite {}

    record Animated(int codepoint) implements EmojiSprite {}
}
