package com.leclowndu93150.twemoji.mixin.client.font;

import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BakedGlyph.class)
public interface BakedGlyphAccessor {

    @Accessor("renderTypes")
    GlyphRenderTypes twemoji$renderTypes();

    @Accessor("u0")
    float twemoji$u0();

    @Accessor("u1")
    float twemoji$u1();

    @Accessor("v0")
    float twemoji$v0();

    @Accessor("v1")
    float twemoji$v1();

    @Accessor("left")
    float twemoji$left();

    @Accessor("right")
    float twemoji$right();

    @Accessor("up")
    float twemoji$up();

    @Accessor("down")
    float twemoji$down();
}
