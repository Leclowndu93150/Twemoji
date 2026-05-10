package com.leclowndu93150.twemoji.mixin.client.modlist;

import com.leclowndu93150.twemoji.client.AnimatedLogo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.neoforged.neoforge.client.gui.ModListScreen$InfoPanel")
public class ModListScreenInfoPanelMixin {

    @Redirect(
        method = "drawPanel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitInscribed(Lnet/minecraft/resources/Identifier;IIIIIIZZ)V",
            remap = false
        )
    )
    private void twemoji$animatedLogo(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int boundsW, int boundsH, int rectW, int rectH, boolean centerX, boolean centerY) {
        Identifier replacement = texture;
        int useW = rectW;
        int useH = rectH;
        if (AnimatedLogo.isSelectedOurs() && AnimatedLogo.hasFrames()) {
            Identifier frame = AnimatedLogo.currentFrameId();
            if (frame != null) {
                replacement = frame;
                useW = AnimatedLogo.width();
                useH = AnimatedLogo.height();
            }
        }
        graphics.blitInscribed(replacement, x, y, boundsW, boundsH, useW, useH, centerX, centerY);
    }
}
