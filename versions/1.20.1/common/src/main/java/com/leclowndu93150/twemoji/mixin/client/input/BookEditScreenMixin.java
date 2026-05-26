package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import com.leclowndu93150.twemoji.client.suggestion.EmojiSuggestionHost;
import com.leclowndu93150.twemoji.client.suggestion.EmojiSuggestions;
import com.leclowndu93150.twemoji.client.tooltip.EmojiTooltip;
import com.leclowndu93150.twemoji.client.tooltip.EmojiTooltipHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen implements EmojiSuggestionHost, EmojiTooltipHost {

    private BookEditScreenMixin(Component title) { super(title); }

    @Shadow private int currentPage;
    @Shadow @Final private List<String> pages;
    @Shadow @Final private TextFieldHelper pageEdit;
    @Shadow private boolean isSigning;

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
        this.pageEdit.setSelectionRange(replacement.start(), replacement.end());
        this.pageEdit.insertText(replacement.text());
    }

    @ModifyVariable(method = "setCurrentPageText", at = @At("HEAD"), argsOnly = true)
    private String twemoji$convertOnSetPage(String text) {
        if (text.isEmpty()) return text;
        char last = text.charAt(text.length() - 1);
        if (last != ':' && !Character.isWhitespace(last)) return text;

        String prefix;
        String suffix;
        if (last == ':') {
            prefix = text;
            suffix = "";
        } else {
            prefix = text.substring(0, text.length() - 1);
            suffix = String.valueOf(last);
        }

        String converted = EmojiRegistry.INSTANCE.applyShortcodes(prefix, EmojiConfig.get().getSkinTone());
        if (converted.equals(prefix)) return text;

        return converted + suffix;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void twemoji$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (this.twemoji$suggestions().keyPressed(keyCode, this::twemoji$applyEmojiSuggestion)) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            EmojiTooltip.Hit hit = this.twemoji$emojiTooltipHit((int) mouseX, (int) mouseY);
            if (hit != null) return EmojiTooltip.copyShortcode(hit);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void twemoji$renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.isSigning) {
            this.twemoji$suggestions().hide();
            return;
        }

        EmojiTooltip.Hit hit = this.twemoji$emojiTooltipHit(mouseX, mouseY);
        EmojiTooltip.renderHoverHighlight(graphics, hit);
        EmojiTooltip.requestHoverCursor(graphics, hit);

        BookEditScreen self = (BookEditScreen)(Object)this;
        String text = this.twemoji$currentText();
        int cursor = this.pageEdit.getCursorPos();

        Font font = Minecraft.getInstance().font;
        StringSplitter splitter = font.getSplitter();
        int bookLeft = (self.width - 192) / 2 + 36;
        int bookTop = 32;

        int anchorX = bookLeft;
        int anchorY = bookTop + 9;
        int[] lineStarts = twemoji$findLineStarts(splitter, text);
        for (int i = 0; i < lineStarts.length; i++) {
            int lineEnd = i + 1 < lineStarts.length ? lineStarts[i + 1] : text.length();
            if (cursor >= lineStarts[i] && cursor <= lineEnd) {
                anchorX = bookLeft + font.width(text.substring(lineStarts[i], cursor));
                anchorY = bookTop + i * 9 + 9;
                break;
            }
        }

        this.twemoji$suggestions().update(text, cursor, anchorX, anchorY, self.width);
        this.twemoji$suggestions().render(graphics, mouseX, mouseY);
        EmojiTooltip.render(graphics, font, self.width, self.height, hit);
    }

    @Override
    public @Nullable EmojiTooltip.Hit twemoji$emojiTooltipHit(int mouseX, int mouseY) {
        BookEditScreen self = (BookEditScreen)(Object)this;
        Font font = Minecraft.getInstance().font;
        StringSplitter splitter = font.getSplitter();
        String text = this.twemoji$currentText();
        int bookLeft = (self.width - 192) / 2 + 36;
        int bookTop = 32;

        int[] lineStarts = twemoji$findLineStarts(splitter, text);
        for (int i = 0; i < lineStarts.length; i++) {
            int lineEnd = i + 1 < lineStarts.length ? lineStarts[i + 1] : text.length();
            String line = text.substring(lineStarts[i], lineEnd).replaceAll("\\n$", "");
            int y = bookTop + i * 9;
            EmojiTooltip.Hit hit = EmojiTooltip.hitString(font, line, bookLeft, y, 1.0F, 1.0F, mouseX, mouseY);
            if (hit != null) return hit;
        }
        return null;
    }

    @Unique
    private String twemoji$currentText() {
        return this.currentPage >= 0 && this.currentPage < this.pages.size() ? this.pages.get(this.currentPage) : "";
    }

    @Unique
    private static int[] twemoji$findLineStarts(StringSplitter splitter, String text) {
        if (text.isEmpty()) return new int[]{0};
        IntList starts = new IntArrayList();
        splitter.splitLines(text, 114, Style.EMPTY, true, (style, start, end) -> starts.add(start));
        if (starts.isEmpty()) starts.add(0);
        return starts.toIntArray();
    }
}
