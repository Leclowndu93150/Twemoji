package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.EmojiPicker;
import com.leclowndu93150.twemoji.client.EmojiSuggestionHost;
import com.leclowndu93150.twemoji.client.EmojiSuggestions;
import com.leclowndu93150.twemoji.client.EmojiTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
    @Shadow private int textX;
    @Shadow private int textY;

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
    private void twemoji$emojiPickerSearch(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (EmojiPicker.handleActiveSearchChar((EditBox)(Object)this, event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true)
    private void twemoji$emojiTooltipClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfo ci) {
        if (event.button() != 0) return;
        EmojiTooltip.Hit hit = this.twemoji$emojiTooltipHit((int)event.x(), (int)event.y());
        if (hit != null) {
            EmojiTooltip.copyShortcode(hit);
            ci.cancel();
        }
    }

    @Inject(method = "extractWidgetRenderState", at = @At("TAIL"))
    private void twemoji$updateEmojiSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
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
        return EmojiTooltip.hitString(this.font, displayed, this.textX, this.textY, 1.0F, 1.0F, mouseX, mouseY);
    }
}
