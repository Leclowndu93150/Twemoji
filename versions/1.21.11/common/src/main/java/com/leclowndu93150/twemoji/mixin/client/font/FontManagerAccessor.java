package com.leclowndu93150.twemoji.mixin.client.font;

import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(FontManager.class)
public interface FontManagerAccessor {

    @Accessor("providersToClose")
    List<GlyphProvider> twemoji$providersToClose();
}
