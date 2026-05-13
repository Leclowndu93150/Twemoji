package com.leclowndu93150.twemoji.client.render.glyph;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.function.Function;

public class StaticBakedGlyph extends BakedGlyph {

    public static final float ADVANCE = 9.0F;
    public static final float HEIGHT = 8.0F;
    public static final float WIDTH = 8.0F;

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
            return StaticBakedGlyph.this;
        }
    };

    public StaticBakedGlyph(ResourceLocation texture) {
        super(
            GlyphRenderTypes.createForColorTexture(texture),
            0.0F, 1.0F, 0.0F, 1.0F,
            0.0F, WIDTH, 0.0F, HEIGHT
        );
    }

    public GlyphInfo info() {
        return info;
    }

    @Override
    public void render(boolean italic, float x, float y, Matrix4f pose, VertexConsumer buffer,
                       float red, float green, float blue, float alpha, int packedLight) {
        if (UntintedBakedGlyph.isOutlineRendering()) return;
        super.render(italic, x, y, pose, buffer, 1.0F, 1.0F, 1.0F, 1.0F, packedLight);
    }
}
