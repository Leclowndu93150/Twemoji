package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.suggestion.EmojiSuggestionHost;
import com.leclowndu93150.twemoji.client.suggestion.EmojiSuggestions;
import com.leclowndu93150.twemoji.client.tooltip.EmojiTooltip;
import com.leclowndu93150.twemoji.mixin.client.input.accessor.AbstractScrollWidgetAccessor;
import com.leclowndu93150.twemoji.mixin.client.input.accessor.MultilineTextFieldStringViewAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiLineEditBox.class)
public abstract class MultiLineEditBoxMixin implements EmojiSuggestionHost {

    @Shadow @Final private Font font;
    @Shadow @Final private MultilineTextField textField;

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
        MultiLineEditBox self = (MultiLineEditBox)(Object)this;
        String value = self.getValue();
        String updated = new StringBuilder(value).replace(replacement.start(), replacement.end(), replacement.text()).toString();
        int cursor = replacement.start() + replacement.text().length();
        self.setValue(updated);
        this.textField.setSelecting(false);
        this.textField.seekCursor(Whence.ABSOLUTE, cursor);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void twemoji$emojiTooltipClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;
        EmojiTooltip.Hit hit = this.twemoji$emojiTooltipHit((int) mouseX, (int) mouseY);
        if (hit != null) {
            EmojiTooltip.copyShortcode(hit);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "renderContents", at = @At("TAIL"))
    private void twemoji$updateEmojiSuggestions(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        MultiLineEditBox self = (MultiLineEditBox)(Object)this;
        EmojiTooltip.Hit hit = this.twemoji$emojiTooltipHit(mouseX, mouseY);
        EmojiTooltip.renderHoverHighlight(graphics, hit);
        EmojiTooltip.requestHoverCursor(graphics, hit);
        EmojiTooltip.setPending(hit);
        if (!self.isFocused() || !self.visible) {
            this.twemoji$suggestions().hide();
            return;
        }
        String value = self.getValue();
        int cursor = this.textField.cursor();
        int anchorX = self.getX() + 4;
        int anchorY = self.getY() + 4 + 11 - (int)((AbstractScrollWidgetAccessor)(Object)this).twemoji$scrollAmount();
        int lineTop = self.getY() + 4;
        for (Object lineView : this.textField.iterateLines()) {
            MultilineTextFieldStringViewAccessor view = (MultilineTextFieldStringViewAccessor)lineView;
            if (cursor >= view.twemoji$beginIndex() && cursor <= view.twemoji$endIndex()) {
                anchorX += this.font.width(value.substring(view.twemoji$beginIndex(), cursor));
                anchorY = lineTop + 11 - (int)((AbstractScrollWidgetAccessor)(Object)this).twemoji$scrollAmount();
                break;
            }
            lineTop += 9;
        }
        this.twemoji$suggestions().update(value, cursor, anchorX, anchorY, Minecraft.getInstance().getWindow().getGuiScaledWidth());
    }

    @Unique
    private EmojiTooltip.Hit twemoji$emojiTooltipHit(int mouseX, int mouseY) {
        MultiLineEditBox self = (MultiLineEditBox)(Object)this;
        if (!self.visible) return null;
        if (mouseX < self.getX() || mouseX >= self.getX() + self.getWidth()
            || mouseY < self.getY() || mouseY >= self.getY() + self.getHeight()) return null;
        String value = self.getValue();
        int lineTop = self.getY() + 4 - (int)((AbstractScrollWidgetAccessor)(Object)this).twemoji$scrollAmount();
        for (Object lineView : this.textField.iterateLines()) {
            MultilineTextFieldStringViewAccessor view = (MultilineTextFieldStringViewAccessor)lineView;
            if (mouseY >= lineTop && mouseY < lineTop + 9) {
                String line = value.substring(view.twemoji$beginIndex(), view.twemoji$endIndex());
                EmojiTooltip.Hit hit = EmojiTooltip.hitString(this.font, line, self.getX() + 4, lineTop, 1.0F, 1.0F, mouseX, mouseY);
                if (hit != null) return hit;
            }
            lineTop += 9;
        }
        return null;
    }
}
