package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.tooltip.EmojiTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractScrollWidget.class)
public abstract class AbstractScrollWidgetMixin {

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void twemoji$renderEmojiTooltip(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        EmojiTooltip.renderPending(
            graphics,
            Minecraft.getInstance().getWindow().getGuiScaledWidth(),
            Minecraft.getInstance().getWindow().getGuiScaledHeight()
        );
    }
}
