package com.leclowndu93150.twemoji.mixin.client.font;

import com.leclowndu93150.twemoji.client.font.TwemojiFontInjection;
import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(FontSet.class)
public abstract class FontSetReloadMixin {

    @Shadow
    @Final
    private GlyphStitcher stitcher;

    @ModifyVariable(
        method = "reload(Ljava/util/List;Ljava/util/Set;)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1
    )
    private List<GlyphProvider.Conditional> twemoji$augmentProviders(List<GlyphProvider.Conditional> providers) {
        Identifier name = ((GlyphStitcherAccessor) this.stitcher).twemoji$texturePrefix();
        if (!Minecraft.DEFAULT_FONT.equals(name)) return providers;
        List<GlyphProvider.Conditional> extra = TwemojiFontInjection.buildProviders();
        if (extra.isEmpty()) return providers;
        List<GlyphProvider.Conditional> combined = new ArrayList<>(extra);
        combined.addAll(providers);
        return combined;
    }
}
