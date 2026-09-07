package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.tooltip.EmojiTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractTextAreaWidget.class)
public abstract class AbstractTextAreaWidgetMixin {

    @Inject(method = "extractWidgetRenderState", at = @At("TAIL"))
    private void twemoji$renderEmojiTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        EmojiTooltip.renderPending(
            graphics,
            Minecraft.getInstance().getWindow().getGuiScaledWidth(),
            Minecraft.getInstance().getWindow().getGuiScaledHeight()
        );
    }
}
