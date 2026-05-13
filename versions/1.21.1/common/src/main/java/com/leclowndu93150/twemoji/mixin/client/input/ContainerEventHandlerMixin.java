package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.tooltip.EmojiTooltip;
import com.leclowndu93150.twemoji.client.tooltip.EmojiTooltipHost;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void twemoji$emojiTooltipClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 || !((Object)this instanceof Screen)) return;
        if ((Object)this instanceof EmojiTooltipHost host) {
            EmojiTooltip.Hit hit = host.twemoji$emojiTooltipHit((int)mouseX, (int)mouseY);
            if (hit != null) {
                cir.setReturnValue(EmojiTooltip.copyShortcode(hit));
            }
        }
    }
}
