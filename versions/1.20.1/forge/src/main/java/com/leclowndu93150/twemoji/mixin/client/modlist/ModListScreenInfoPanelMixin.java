package com.leclowndu93150.twemoji.mixin.client.modlist;

import com.leclowndu93150.twemoji.client.render.AnimatedLogo;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraftforge.client.gui.ModListScreen$InfoPanel")
public class ModListScreenInfoPanelMixin {

    @Redirect(
        method = "drawPanel(Lnet/minecraft/client/gui/GuiGraphics;IILcom/mojang/blaze3d/vertex/Tesselator;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitInscribed(Lnet/minecraft/resources/ResourceLocation;IIIIIIZZ)V",
            remap = false
        ),
        remap = false
    )
    private void twemoji$animatedLogo(GuiGraphics graphics, ResourceLocation texture, int x, int y, int boundsW, int boundsH, int rectW, int rectH, boolean centerX, boolean centerY) {
        ResourceLocation replacement = texture;
        int useW = rectW;
        int useH = rectH;
        if (AnimatedLogo.isSelectedOurs() && AnimatedLogo.hasFrames()) {
            ResourceLocation frame = AnimatedLogo.currentFrameId();
            if (frame != null) {
                replacement = frame;
                useW = AnimatedLogo.width();
                useH = AnimatedLogo.height();
            }
        }
        graphics.blitInscribed(replacement, x, y, boundsW, boundsH, useW, useH, centerX, centerY);
    }
}
