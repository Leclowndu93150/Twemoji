package com.leclowndu93150.twemoji.client.glyph;

import com.mojang.blaze3d.font.GlyphInfo;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

public record UntintedBakedGlyph(BakedGlyph delegate) implements BakedGlyph {

    private static final ThreadLocal<Boolean> OUTLINE_RENDERING = ThreadLocal.withInitial(() -> false);

    public static void beginOutlineRendering() {
        OUTLINE_RENDERING.set(true);
    }

    public static void endOutlineRendering() {
        OUTLINE_RENDERING.set(false);
    }

    public static boolean isOutlineRendering() {
        return OUTLINE_RENDERING.get();
    }

    @Override
    public GlyphInfo info() {
        return delegate.info();
    }

    @Override
    public TextRenderable.@Nullable Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
        return delegate.createGlyph(x, y, 0xFFFFFFFF, 0, style, boldOffset, shadowOffset);
    }
}
