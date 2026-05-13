package com.leclowndu93150.twemoji.client.render.glyph;

import com.leclowndu93150.twemoji.client.registry.AnimatedEmojiRegistry;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.function.Function;

public final class AnimatedBakedGlyph extends BakedGlyph {

    public static final float ADVANCE = 9.0F;
    public static final float HEIGHT = 8.0F;
    public static final float WIDTH = 8.0F;

    private final AnimatedEmojiRegistry.AnimatedEmoji emoji;

    private final GlyphInfo info = new GlyphInfo() {
        @Override
        public float getAdvance() {
            return ADVANCE;
        }

        @Override
        public float getShadowOffset() {
            return 0.0F;
        }

        @Override
        public float getBoldOffset() {
            return 0.0F;
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> function) {
            return AnimatedBakedGlyph.this;
        }
    };

    public AnimatedBakedGlyph(AnimatedEmojiRegistry.AnimatedEmoji emoji) {
        super(
            GlyphRenderTypes.createForColorTexture(emoji.frameTextures().get(0)),
            0.0F, 1.0F, 0.0F, 1.0F,
            0.0F, WIDTH, 0.0F, HEIGHT
        );
        this.emoji = emoji;
    }

    public GlyphInfo info() {
        return info;
    }

    @Override
    public RenderType renderType(Font.DisplayMode displayMode) {
        ResourceLocation frame = emoji.currentFrame();
        return switch (displayMode) {
            case NORMAL -> RenderType.text(frame);
            case SEE_THROUGH -> RenderType.textSeeThrough(frame);
            case POLYGON_OFFSET -> RenderType.textPolygonOffset(frame);
        };
    }

    @Override
    public void render(boolean italic, float x, float y, Matrix4f pose, VertexConsumer buffer,
                       float red, float green, float blue, float alpha, int packedLight) {
        if (UntintedBakedGlyph.isOutlineRendering()) return;
        super.render(italic, x, y, pose, buffer, 1.0F, 1.0F, 1.0F, 1.0F, packedLight);
    }
}
