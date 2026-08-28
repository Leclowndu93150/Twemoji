package com.leclowndu93150.twemoji.mixin.client.font;

import com.leclowndu93150.twemoji.client.font.TwemojiFontInjection;
import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.gui.font.FontSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(FontSet.class)
public abstract class FontSetReloadMixin {

    @ModifyVariable(
        method = "reload(Ljava/util/List;)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1
    )
    private List<GlyphProvider> twemoji$augmentProviders(List<GlyphProvider> providers) {
        List<GlyphProvider> extra = TwemojiFontInjection.awaitProvidersToInject();
        if (extra.isEmpty()) return providers;
        List<GlyphProvider> combined = new ArrayList<>(extra);
        combined.addAll(providers);
        return combined;
    }
}
