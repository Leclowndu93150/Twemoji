package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.EmojiSuggestionHost;
import com.leclowndu93150.twemoji.client.EmojiTooltip;
import com.leclowndu93150.twemoji.client.EmojiTooltipHost;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void twemoji$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof ChatScreen) return;
        EmojiSuggestionHost host = this.twemoji$host();
        if (host != null && host.twemoji$suggestions().keyPressed(event, host::twemoji$applyEmojiSuggestion)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void twemoji$extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if ((Object)this instanceof ChatScreen) return;
        Screen self = (Screen)(Object)this;
        EmojiSuggestionHost host = this.twemoji$host();
        if (host != null) {
            host.twemoji$suggestions().render(graphics, mouseX, mouseY);
        }
        if (!(self instanceof EmojiTooltipHost)) {
            EmojiTooltip.render(self, graphics, self.getFont(), self.width, self.height);
        }
    }

    private EmojiSuggestionHost twemoji$host() {
        Object self = this;
        if (self instanceof Screen screen) {
            GuiEventListener focused = screen.getFocused();
            if (focused instanceof EmojiSuggestionHost host) return host;
        }
        return self instanceof EmojiSuggestionHost host ? host : null;
    }
}
