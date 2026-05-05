package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.EmojiSuggestionHost;
import com.leclowndu93150.twemoji.client.EmojiSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        self.setValue(updated, true);
        this.textField.setSelecting(false);
        this.textField.seekCursor(Whence.ABSOLUTE, cursor);
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void twemoji$updateEmojiSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        MultiLineEditBox self = (MultiLineEditBox)(Object)this;
        if (!self.isFocused() || !self.visible) {
            this.twemoji$suggestions().hide();
            return;
        }
        String value = self.getValue();
        int cursor = this.textField.cursor();
        int anchorX = self.getX() + 4;
        int anchorY = self.getY() + 4 + 11 - (int)self.scrollAmount();
        int lineTop = self.getY() + 4;
        for (Object lineView : this.textField.iterateLines()) {
            MultilineTextFieldStringViewAccessor view = (MultilineTextFieldStringViewAccessor)lineView;
            if (cursor >= view.twemoji$beginIndex() && cursor <= view.twemoji$endIndex()) {
                anchorX += this.font.width(value.substring(view.twemoji$beginIndex(), cursor));
                anchorY = lineTop + 11 - (int)self.scrollAmount();
                break;
            }
            lineTop += 9;
        }
        this.twemoji$suggestions().update(value, cursor, anchorX, anchorY, Minecraft.getInstance().getWindow().getGuiScaledWidth());
    }
}
