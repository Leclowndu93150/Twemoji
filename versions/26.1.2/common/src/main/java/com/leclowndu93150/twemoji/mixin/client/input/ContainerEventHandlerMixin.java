package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.EmojiTooltip;
import com.leclowndu93150.twemoji.client.EmojiTooltipHost;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void twemoji$emojiTooltipClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0 || !((Object)this instanceof Screen screen)) return;
        EmojiTooltip.closeIfOpen(screen);
        if ((Object)this instanceof EmojiTooltipHost host) {
            EmojiTooltip.Hit hit = host.twemoji$emojiTooltipHit((int)event.x(), (int)event.y());
            if (hit != null) {
                cir.setReturnValue(EmojiTooltip.click(screen, hit));
            }
        }
    }
}
