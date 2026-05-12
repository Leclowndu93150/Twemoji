package com.leclowndu93150.twemoji.client.render.glyph;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;

public final class StaticBakedGlyph implements BakedGlyph {

    public static final float ADVANCE = 9.0F;
    public static final float HEIGHT = 8.0F;
    public static final float WIDTH = 8.0F;

    private static final GlyphInfo INFO = new GlyphInfo() {
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
    };

    private final Identifier texture;

    public StaticBakedGlyph(Identifier texture) {
        this.texture = texture;
    }

    @Override
    public GlyphInfo info() {
        return INFO;
    }

    @Override
    public TextRenderable.@Nullable Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
        return new Instance(texture, x, y, style);
    }

    private record Instance(Identifier texture, float x, float y, Style style) implements TextRenderable.Styled {

        @Override
        public float left() {
            return x;
        }

        @Override
        public float top() {
            return y;
        }

        @Override
        public float right() {
            return x + WIDTH;
        }

        @Override
        public float bottom() {
            return y + HEIGHT;
        }

        @Override
        public float activeRight() {
            return x + ADVANCE;
        }

        @Override
        public void render(Matrix4f pose, VertexConsumer buffer, int packedLightCoords, boolean flat) {
            float x0 = x;
            float x1 = x + WIDTH;
            float y0 = y;
            float y1 = y + HEIGHT;
            int color = 0xFFFFFFFF;
            int fullBright = 0x00F000F0;
            buffer.addVertex(pose, x0, y0, 0.0F).setColor(color).setUv(0.0F, 0.0F).setLight(fullBright);
            buffer.addVertex(pose, x0, y1, 0.0F).setColor(color).setUv(0.0F, 1.0F).setLight(fullBright);
            buffer.addVertex(pose, x1, y1, 0.0F).setColor(color).setUv(1.0F, 1.0F).setLight(fullBright);
            buffer.addVertex(pose, x1, y0, 0.0F).setColor(color).setUv(1.0F, 0.0F).setLight(fullBright);
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            return switch (displayMode) {
                case NORMAL -> RenderTypes.text(texture);
                case SEE_THROUGH -> RenderTypes.textSeeThrough(texture);
                case POLYGON_OFFSET -> RenderTypes.textPolygonOffset(texture);
            };
        }

        @Override
        public GpuTextureView textureView() {
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
            return tex.getTextureView();
        }

        @Override
        public RenderPipeline guiPipeline() {
            return RenderPipelines.GUI_TEXT;
        }
    }
}
