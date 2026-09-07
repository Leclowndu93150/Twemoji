package com.leclowndu93150.twemoji.mixin.client.modlist;

import com.leclowndu93150.twemoji.client.render.AnimatedLogo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.gui.widget.ResizableTextureImageWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ResizableTextureImageWidget.class)
public class ModListBannerMixin {

    @Redirect(
        method = "extractWidgetRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V",
            remap = false
        )
    )
    private void twemoji$animatedLogo(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
                                      int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        Identifier replacement = texture;
        if (AnimatedLogo.isSelectedOurs() && AnimatedLogo.hasFrames()) {
            Identifier frame = AnimatedLogo.currentFrameId();
            if (frame != null) replacement = frame;
        }
        graphics.blit(pipeline, replacement, x, y, u, v, width, height, textureWidth, textureHeight);
    }
}
