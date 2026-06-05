package com.leclowndu93150.twemoji.mixin.client.font;

import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlyphStitcher.class)
public interface GlyphStitcherAccessor {

    @Accessor("texturePrefix")
    Identifier twemoji$texturePrefix();
}
