package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.EmojiPicker;
import com.leclowndu93150.twemoji.client.EmojiSuggestionHost;
import com.leclowndu93150.twemoji.client.EmojiSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EditBox.class)
public abstract class EditBoxMixin implements EmojiSuggestionHost {

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

    @Inject(method = "extractWidgetRenderState", at = @At("TAIL"))
    private void twemoji$updateEmojiSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        EditBox self = (EditBox)(Object)this;
        if (!self.isFocused() || !self.isVisible()) {
            this.twemoji$suggestions().hide();
            return;
        }
        this.twemoji$suggestions()
            .update(self.getValue(), self.getCursorPosition(), self.getScreenX(self.getCursorPosition()), self.getY() + self.getHeight() + 2, Minecraft.getInstance().getWindow().getGuiScaledWidth());
    }
}
