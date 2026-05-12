package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.EmojiConfig;
import com.leclowndu93150.twemoji.client.EmojiRegistry;
import com.leclowndu93150.twemoji.client.EmojiSuggestionHost;
import com.leclowndu93150.twemoji.client.EmojiSuggestions;
import com.leclowndu93150.twemoji.client.EmojiTooltip;
import com.leclowndu93150.twemoji.client.EmojiTooltipHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin implements EmojiSuggestionHost, EmojiTooltipHost {

    @Shadow @Final protected SignBlockEntity sign;
    @Shadow @Final private String[] messages;
    @Shadow private int line;
    @Shadow private @Nullable TextFieldHelper signField;
    @Shadow protected abstract Vector3f getSignTextScale();
    @Shadow protected abstract float getSignYOffset();

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
        if (this.signField == null) return;
        this.signField.setSelectionRange(replacement.start(), replacement.end());
        this.signField.insertText(replacement.text());
    }

    @ModifyVariable(method = "setMessage", at = @At("HEAD"), argsOnly = true)
    private String twemoji$convertOnSetMessage(String message) {
        if (message.isEmpty()) return message;
        char last = message.charAt(message.length() - 1);
        if (last != ':' && !Character.isWhitespace(last)) return message;

        String prefix;
        String suffix;
        if (last == ':') {
            prefix = message;
            suffix = "";
        } else {
            prefix = message.substring(0, message.length() - 1);
            suffix = String.valueOf(last);
        }

        String converted = EmojiRegistry.INSTANCE.applyShortcodes(prefix, EmojiConfig.get().getSkinTone());
        if (converted.equals(prefix)) return message;

        return converted + suffix;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void twemoji$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.twemoji$suggestions().keyPressed(event, this::twemoji$applyEmojiSuggestion)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void twemoji$extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        EmojiTooltip.Hit hit = this.twemoji$emojiTooltipHit(mouseX, mouseY);
        EmojiTooltip.renderHoverHighlight(graphics, hit);
        EmojiTooltip.requestHoverCursor(graphics, hit);
        if (this.signField == null) {
            this.twemoji$suggestions().hide();
            return;
        }

        String message = this.messages[this.line];
        int cursor = this.signField.getCursorPos();
        int lineHeight = this.sign.getTextLineHeight();
        int signMidpoint = 4 * lineHeight / 2;
        int localX = Minecraft.getInstance().font.width(message.substring(0, Math.min(cursor, message.length()))) - Minecraft.getInstance().font.width(message) / 2;
        int localY = this.line * lineHeight - signMidpoint + lineHeight + 2;
        Vector3f scale = this.getSignTextScale();
        int x = (int)(((AbstractSignEditScreen)(Object)this).width / 2.0F + localX * scale.x());
        int y = (int)(this.getSignYOffset() + localY * scale.y());
        this.twemoji$suggestions().update(message, cursor, x, y, ((AbstractSignEditScreen)(Object)this).width);
        this.twemoji$suggestions().render(graphics, mouseX, mouseY);
        AbstractSignEditScreen self = (AbstractSignEditScreen)(Object)this;
        EmojiTooltip.render(graphics, self.getFont(), self.width, self.height, hit);
    }

    @Override
    public EmojiTooltip.Hit twemoji$emojiTooltipHit(int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        AbstractSignEditScreen self = (AbstractSignEditScreen)(Object)this;
        Vector3f scale = this.getSignTextScale();
        int lineHeight = this.sign.getTextLineHeight();
        int signMidpoint = 4 * lineHeight / 2;
        for (int i = 0; i < this.messages.length; i++) {
            String message = this.messages[i];
            if (message == null || message.isEmpty()) continue;
            if (font.isBidirectional()) {
                message = font.bidirectionalShaping(message);
            }
            int localX = -font.width(message) / 2;
            int localY = i * lineHeight - signMidpoint;
            int x = Math.round(self.width / 2.0F + localX * scale.x());
            int y = Math.round(this.getSignYOffset() + localY * scale.y());
            EmojiTooltip.Hit hit = EmojiTooltip.hitString(font, message, x, y, scale.x(), scale.y(), mouseX, mouseY);
            if (hit != null) return hit;
        }
        return null;
    }
}
