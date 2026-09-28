package com.leclowndu93150.twemoji.mixin.client.font;

import com.leclowndu93150.twemoji.client.render.glyph.UntintedBakedGlyph;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class FontMixin {

    @Inject(method = "prepare8xTextOutline", at = @At("HEAD"))
    private void twemoji$beginOutlineRendering(
        FormattedCharSequence str,
        float x,
        float y,
        int outlineColor,
        CallbackInfoReturnable<Font.PreparedText> cir
    ) {
        UntintedBakedGlyph.beginOutlineRendering();
    }

    @Inject(method = "prepare8xTextOutline", at = @At("RETURN"))
    private void twemoji$endOutlineRendering(
        FormattedCharSequence str,
        float x,
        float y,
        int outlineColor,
        CallbackInfoReturnable<Font.PreparedText> cir
    ) {
        UntintedBakedGlyph.endOutlineRendering();
    }
}
