package com.leclowndu93150.twemoji.mixin.client.chat;

import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.picker.EmojiPicker;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import com.leclowndu93150.twemoji.client.tooltip.EmojiTooltip;
import com.leclowndu93150.twemoji.client.picker.TwemojiKeyMappings;
import com.leclowndu93150.twemoji.mixin.client.chat.accessor.ChatComponentAccessor;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow protected EditBox input;
    @Shadow private ChatComponent.DisplayMode displayMode;

    @Unique
    private EmojiPicker twemoji$picker;
    @Unique
    private boolean twemoji$processingInput = false;
    @Unique
    private boolean twemoji$inLiveConvert = false;

    @Inject(method = "init", at = @At("TAIL"))
    private void twemoji$initPicker(CallbackInfo ci) {
        ChatScreen self = (ChatScreen)(Object)this;
        this.input.setWidth(self.width - 24);
        this.twemoji$picker = new EmojiPicker(self.getFont());
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void twemoji$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.twemoji$picker != null && event.hasControlDownWithQuirk() && TwemojiKeyMappings.OPEN_PICKER.matches(event)) {
            this.twemoji$picker.toggle();
            if (this.twemoji$picker.isOpen()) {
                this.twemoji$picker.attachInput(this.input);
            }
            cir.setReturnValue(true);
            return;
        }
        if (this.twemoji$picker != null && this.twemoji$picker.keyPressed(event, this.input)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void twemoji$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (this.twemoji$picker != null) {
            ChatScreen self = (ChatScreen)(Object)this;
            if (this.twemoji$picker.mouseClicked(event, self.width, self.height, this.input)) {
                cir.setReturnValue(true);
                return;
            }
        }
        if (event.button() == 0) {
            EmojiTooltip.Hit hit = this.twemoji$chatMessageHit((int)event.x(), (int)event.y());
            if (hit != null) {
                cir.setReturnValue(EmojiTooltip.copyShortcode(hit));
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void twemoji$mouseScrolled(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (this.twemoji$picker != null) {
            ChatScreen self = (ChatScreen)(Object)this;
            if (this.twemoji$picker.mouseScrolled(x, y, scrollY, self.width, self.height)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void twemoji$renderPicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (this.twemoji$picker != null) {
            ChatScreen self = (ChatScreen)(Object)this;
            this.twemoji$picker.render(graphics, mouseX, mouseY, self.width, self.height);
        }
        ChatScreen self2 = (ChatScreen)(Object)this;
        if (this.twemoji$picker != null && this.twemoji$picker.blocksMouse(mouseX, mouseY, self2.width, self2.height)) return;
        EmojiTooltip.Hit hit = this.twemoji$chatMessageHit(mouseX, mouseY);
        EmojiTooltip.renderHoverHighlight(graphics, hit);
        EmojiTooltip.requestHoverCursor(graphics, hit);
        EmojiTooltip.render(graphics, self2.getFont(), self2.width, self2.height, hit);
    }

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void twemoji$onHandleChatInput(String msg, boolean addToRecent, CallbackInfo ci) {
        if (twemoji$processingInput) return;
        String shortcoded = EmojiRegistry.INSTANCE.applyShortcodes(msg, EmojiConfig.get().getSkinTone());
        String processed = EmojiRegistry.INSTANCE.unshape(shortcoded);
        if (!processed.equals(msg)) {
            ChatScreen self = (ChatScreen)(Object)this;
            ci.cancel();
            twemoji$processingInput = true;
            try {
                self.handleChatInput(processed, addToRecent);
            } finally {
                twemoji$processingInput = false;
            }
        }
    }

    @Inject(method = "onEdited", at = @At("HEAD"))
    private void twemoji$onEdited(String value, CallbackInfo ci) {
        if (twemoji$inLiveConvert) return;
        int cursor = input.getCursorPosition();
        if (cursor == 0 || cursor > value.length()) return;
        char last = value.charAt(cursor - 1);

        String prefix;
        String suffix;
        if (last == ':') {
            prefix = value.substring(0, cursor);
            suffix = value.substring(cursor);
        } else if (Character.isWhitespace(last)) {
            prefix = value.substring(0, cursor - 1);
            suffix = value.substring(cursor - 1);
        } else {
            return;
        }

        String converted = EmojiRegistry.INSTANCE.applyShortcodes(prefix, EmojiConfig.get().getSkinTone());
        if (converted.equals(prefix)) return;

        twemoji$inLiveConvert = true;
        try {
            String newValue = converted + suffix;
            input.setValue(newValue);
            int newCursor = converted.length();
            input.setCursorPosition(Math.min(newCursor, newValue.length()));
            input.setHighlightPos(input.getCursorPosition());
        } finally {
            twemoji$inLiveConvert = false;
        }
    }

    @Unique
    private EmojiTooltip.Hit twemoji$chatMessageHit(int mouseX, int mouseY) {
        ChatScreen self = (ChatScreen)(Object)this;
        Minecraft minecraft = Minecraft.getInstance();
        Map<FormattedCharSequence, GuiMessage.Line> lookup = EmojiTooltip.buildLineLookup(
            ((ChatComponentAccessor) minecraft.gui.getChat()).twemoji$trimmedMessages()
        );
        EmojiTooltip.ChatCollector collector = new EmojiTooltip.ChatCollector(self.getFont(), mouseX, mouseY, lookup);
        minecraft.gui.getChat().captureClickableText(collector, minecraft.getWindow().getGuiScaledHeight(), minecraft.gui.getGuiTicks(), this.displayMode);
        return collector.result();
    }
}
