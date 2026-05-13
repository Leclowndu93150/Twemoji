package com.leclowndu93150.twemoji.mixin.client.font;

import com.leclowndu93150.twemoji.client.registry.AnimatedEmojiRegistry;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import com.leclowndu93150.twemoji.client.render.TwemojiSheets;
import com.leclowndu93150.twemoji.client.render.glyph.StaticBakedGlyph;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.Font$StringRenderOutput")
public abstract class FontStringRenderOutputMixin {

    @Shadow @Final private boolean dropShadow;
    @Shadow float x;

    @Inject(method = "accept(ILnet/minecraft/network/chat/Style;I)Z", at = @At("HEAD"), cancellable = true)
    private void twemoji$skipEmojiShadow(int position, Style style, int codepoint, CallbackInfoReturnable<Boolean> cir) {
        if (!this.dropShadow) return;
        boolean isEmoji = TwemojiSheets.isTwemojiCodepoint(codepoint)
            || AnimatedEmojiRegistry.INSTANCE.bakedGlyph(codepoint) != null
            || EmojiRegistry.INSTANCE.syncedStaticGlyph(codepoint) != null;
        if (isEmoji) {
            this.x += StaticBakedGlyph.ADVANCE;
            cir.setReturnValue(true);
        }
    }
}
