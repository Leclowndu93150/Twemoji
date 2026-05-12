package com.leclowndu93150.twemoji.mixin.client.font;

import com.leclowndu93150.twemoji.client.render.glyph.UntintedBakedGlyph;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/client/gui/Font$PreparedTextBuilder")
public abstract class FontPreparedTextBuilderMixin {

    @Shadow @Final private boolean includeEmpty;

    @Inject(method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z", at = @At("HEAD"), cancellable = true)
    private void twemoji$skipEmojiOutline(int position, Style style, BakedGlyph glyph, CallbackInfoReturnable<Boolean> cir) {
        if (UntintedBakedGlyph.isOutlineRendering() && !this.includeEmpty && glyph instanceof UntintedBakedGlyph) {
            cir.setReturnValue(true);
        }
    }
}
