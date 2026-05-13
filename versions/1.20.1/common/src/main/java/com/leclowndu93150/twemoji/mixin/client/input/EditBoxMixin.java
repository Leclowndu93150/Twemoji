package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.picker.EmojiPicker;
import com.leclowndu93150.twemoji.client.suggestion.EmojiSuggestionHost;
import com.leclowndu93150.twemoji.client.suggestion.EmojiSuggestions;
import com.leclowndu93150.twemoji.client.tooltip.EmojiTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EditBox.class)
public abstract class EditBoxMixin implements EmojiSuggestionHost {

    @Shadow @Final private Font font;
    @Shadow private int displayPos;
    @Shadow private boolean bordered;

    @Unique
    private EmojiSuggestions twemoji$suggestions;

    @Override
    public EmojiSuggestions twemoji$suggestions() {
        if (this.twemoji$suggestions == null) {
            this.twemoji$suggestions = new EmojiSuggestions(Minecraft.getInstance().font);
        }
        return this.twemoji$suggestions;
    }

    @Override
    public void twemoji$applyEmojiSuggestion(EmojiSuggestions.Replacement replacement) {
        EditBox self = (EditBox)(Object)this;
        String value = self.getValue();
        String updated = new StringBuilder(value).replace(replacement.start(), replacement.end(), replacement.text()).toString();
        int cursor = replacement.start() + replacement.text().length();
        self.setValue(updated);
        self.setCursorPosition(cursor);
        self.setHighlightPos(cursor);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void twemoji$emojiPickerSearch(char c, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (EmojiPicker.handleActiveSearchChar((EditBox)(Object)this, c)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true)
    private void twemoji$emojiTooltipClicked(double mouseX, double mouseY, CallbackInfo ci) {
        EmojiTooltip.Hit hit = this.twemoji$emojiTooltipHit((int) mouseX, (int) mouseY);
        if (hit != null) {
            EmojiTooltip.copyShortcode(hit);
            ci.cancel();
        }
    }

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void twemoji$updateEmojiSuggestions(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        EditBox self = (EditBox)(Object)this;
        EmojiTooltip.Hit hit = this.twemoji$emojiTooltipHit(mouseX, mouseY);
        EmojiTooltip.renderHoverHighlight(graphics, hit);
        EmojiTooltip.requestHoverCursor(graphics, hit);
        EmojiTooltip.render(graphics, this.font, Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight(), hit);
        if (!self.isFocused() || !self.isVisible()) {
            this.twemoji$suggestions().hide();
            return;
        }
        this.twemoji$suggestions()
            .update(self.getValue(), self.getCursorPosition(), self.getScreenX(self.getCursorPosition()), self.getY() + self.getHeight() + 2, Minecraft.getInstance().getWindow().getGuiScaledWidth());
    }

    @Unique
    private EmojiTooltip.Hit twemoji$emojiTooltipHit(int mouseX, int mouseY) {
        EditBox self = (EditBox)(Object)this;
        if (!self.isVisible()) return null;
        if (mouseX < self.getX() || mouseX >= self.getX() + self.getWidth()
            || mouseY < self.getY() || mouseY >= self.getY() + self.getHeight()) return null;
        String value = self.getValue();
        if (this.displayPos > value.length()) return null;
        String displayed = this.font.plainSubstrByWidth(value.substring(this.displayPos), self.getInnerWidth());
        int textX = this.bordered ? self.getX() + 4 : self.getX();
        int textY = this.bordered ? self.getY() + (self.getHeight() - 8) / 2 : self.getY();
        return EmojiTooltip.hitString(this.font, displayed, textX, textY, 1.0F, 1.0F, mouseX, mouseY);
    }
}
