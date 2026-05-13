package com.leclowndu93150.twemoji.client.render.glyph;

import com.leclowndu93150.twemoji.mixin.client.font.BakedGlyphAccessor;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.joml.Matrix4f;

public class UntintedBakedGlyph extends BakedGlyph {

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

    public static UntintedBakedGlyph wrap(BakedGlyph delegate) {
        BakedGlyphAccessor accessor = (BakedGlyphAccessor) delegate;
        return new UntintedBakedGlyph(
            accessor.twemoji$renderTypes(),
            accessor.twemoji$u0(), accessor.twemoji$u1(),
            accessor.twemoji$v0(), accessor.twemoji$v1(),
            accessor.twemoji$left(), accessor.twemoji$right(),
            accessor.twemoji$up(), accessor.twemoji$down()
        );
    }

    private UntintedBakedGlyph(GlyphRenderTypes renderTypes,
                               float u0, float u1, float v0, float v1,
                               float left, float right, float up, float down) {
        super(renderTypes, u0, u1, v0, v1, left, right, up, down);
    }

    @Override
    public void render(boolean italic, float x, float y, Matrix4f pose, VertexConsumer buffer,
                       float red, float green, float blue, float alpha, int packedLight) {
        if (isOutlineRendering()) return;
        super.render(italic, x, y, pose, buffer, 1.0F, 1.0F, 1.0F, 1.0F, packedLight);
    }
}
