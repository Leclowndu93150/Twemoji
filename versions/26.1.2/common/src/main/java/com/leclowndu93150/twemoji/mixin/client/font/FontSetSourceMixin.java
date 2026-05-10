package com.leclowndu93150.twemoji.mixin.client.font;

import com.leclowndu93150.twemoji.client.AnimatedEmojiRegistry;
import com.leclowndu93150.twemoji.client.EmojiRegistry;
import com.leclowndu93150.twemoji.client.glyph.AnimatedBakedGlyph;
import com.leclowndu93150.twemoji.client.glyph.StaticBakedGlyph;
import com.leclowndu93150.twemoji.client.glyph.TwemojiSheets;
import com.leclowndu93150.twemoji.client.glyph.UntintedBakedGlyph;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/client/gui/font/FontSet$Source")
public class FontSetSourceMixin {

    @Inject(method = "getGlyph(I)Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;", at = @At("HEAD"), cancellable = true)
    private void twemoji$injectCustomGlyph(int codepoint, CallbackInfoReturnable<BakedGlyph> cir) {
        if (!AnimatedEmojiRegistry.INSTANCE.hasAny() && !EmojiRegistry.INSTANCE.hasSyncedStaticGlyphs()) return;
        AnimatedBakedGlyph animated = AnimatedEmojiRegistry.INSTANCE.bakedGlyph(codepoint);
        if (animated != null) {
            cir.setReturnValue(animated);
            return;
        }
        StaticBakedGlyph staticGlyph = EmojiRegistry.INSTANCE.syncedStaticGlyph(codepoint);
        if (staticGlyph != null) cir.setReturnValue(staticGlyph);
    }

    @Inject(method = "getGlyph(I)Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;", at = @At("RETURN"), cancellable = true)
    private void twemoji$untintSheetGlyph(int codepoint, CallbackInfoReturnable<BakedGlyph> cir) {
        BakedGlyph glyph = cir.getReturnValue();
        if (TwemojiSheets.isTwemojiCodepoint(codepoint) && !(glyph instanceof UntintedBakedGlyph)) {
            cir.setReturnValue(new UntintedBakedGlyph(glyph));
        }
    }
}
