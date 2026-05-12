package com.leclowndu93150.twemoji.mixin.client.font;

import com.leclowndu93150.twemoji.client.render.glyph.UntintedBakedGlyph;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Matrix4fc;

@Mixin(Font.class)
public abstract class FontMixin {

    @Inject(method = "drawInBatch8xOutline", at = @At("HEAD"))
    private void twemoji$beginOutlineRendering(
        FormattedCharSequence str,
        float x,
        float y,
        int color,
        int outlineColor,
        Matrix4fc pose,
        MultiBufferSource bufferSource,
        int packedLightCoords,
        CallbackInfo ci
    ) {
        UntintedBakedGlyph.beginOutlineRendering();
    }

    @Inject(method = "drawInBatch8xOutline", at = @At("RETURN"))
    private void twemoji$endOutlineRendering(
        FormattedCharSequence str,
        float x,
        float y,
        int color,
        int outlineColor,
        Matrix4fc pose,
        MultiBufferSource bufferSource,
        int packedLightCoords,
        CallbackInfo ci
    ) {
        UntintedBakedGlyph.endOutlineRendering();
    }
}
