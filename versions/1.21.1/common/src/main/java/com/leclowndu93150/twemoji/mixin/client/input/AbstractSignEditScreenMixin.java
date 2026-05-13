package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import com.leclowndu93150.twemoji.client.suggestion.EmojiSuggestionHost;
import com.leclowndu93150.twemoji.client.suggestion.EmojiSuggestions;
import com.leclowndu93150.twemoji.client.tooltip.EmojiTooltip;
import com.leclowndu93150.twemoji.client.tooltip.EmojiTooltipHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
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

    @Shadow @Final private SignBlockEntity sign;
    @Shadow @Final private String[] messages;
    @Shadow private int line;
    @Shadow @Nullable private TextFieldHelper signField;
    @Shadow protected abstract Vector3f getSignTextScale();

    @Unique
    private EmojiSuggestions twemoji$suggestions;

    @Unique
    private float twemoji$signYOffset() {
        if ((Object) this instanceof HangingSignEditScreen) return 125.0F;
        return this.sign.getBlockState().getBlock() instanceof StandingSignBlock ? 90.0F : 125.0F;
    }

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
    private void twemoji$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (this.twemoji$suggestions().keyPressed(keyCode, this::twemoji$applyEmojiSuggestion)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void twemoji$extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
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
        AbstractSignEditScreen self = (AbstractSignEditScreen)(Object)this;
        int x = (int)(self.width / 2.0F + localX * scale.x());
        int y = (int)(this.twemoji$signYOffset() + localY * scale.y());
        this.twemoji$suggestions().update(message, cursor, x, y, self.width);
        this.twemoji$suggestions().render(graphics, mouseX, mouseY);
        EmojiTooltip.render(graphics, Minecraft.getInstance().font, self.width, self.height, hit);
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
            int y = Math.round(this.twemoji$signYOffset() + localY * scale.y());
            EmojiTooltip.Hit hit = EmojiTooltip.hitString(font, message, x, y, scale.x(), scale.y(), mouseX, mouseY);
            if (hit != null) return hit;
        }
        return null;
    }
}
