package com.leclowndu93150.twemoji.mixin.client.chat;

import com.leclowndu93150.twemoji.client.AnimatedEmojiRegistry;
import com.leclowndu93150.twemoji.client.EmojiConfig;
import com.leclowndu93150.twemoji.client.EmojiRegistry;
import com.leclowndu93150.twemoji.client.glyph.EmojiSprite;
import com.leclowndu93150.twemoji.client.EmojiSuggestion;
import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CommandSuggestions.SuggestionsList.class)
public class SuggestionsListMixin {

    @Shadow private List<Suggestion> suggestionList;
    @Shadow private int offset;
    @Shadow private int current;
    @Shadow(aliases = "this$0") private CommandSuggestions twemoji$outer;

    private static final int EMOJI_SIZE = 10;
    private static final int EMOJI_PAD = 2;
    private static final int EMOJI_COL = EMOJI_SIZE + EMOJI_PAD;
    private static final int MAX_LABEL_WIDTH = 120;

    @Inject(method = "useSuggestion", at = @At("TAIL"))
    private void twemoji$onUseSuggestion(CallbackInfo ci) {
        if (!(suggestionList.get(current) instanceof EmojiSuggestion)) return;
        EditBox input = ((CommandSuggestionsAccessor) twemoji$outer).getInput();
        String before = input.getValue();
        String converted = EmojiRegistry.INSTANCE.applyShortcodes(before, EmojiConfig.get().getSkinTone());
        if (!converted.equals(before)) {
            int cursor = input.getCursorPosition();
            input.setValue(converted);
            input.setCursorPosition(Math.min(cursor, converted.length()));
            input.setHighlightPos(input.getCursorPosition());
        }
    }

    @Redirect(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"
        )
    )
    private void twemoji$redirectText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
        int row = (y - 2 - ((SuggestionsListAccessor)(Object)this).getRect().getY()) / 12;
        int idx = row + offset;

        if (idx >= 0 && idx < suggestionList.size() && suggestionList.get(idx) instanceof EmojiSuggestion emoji) {
            EmojiSprite sprite = emoji.getSprite();
            switch (sprite) {
                case EmojiSprite.Sheet s -> graphics.blit(s.texture(), x, y - 1, x + EMOJI_SIZE, y - 1 + EMOJI_SIZE, s.u0(), s.u1(), s.v0(), s.v1());
                case EmojiSprite.Custom c -> graphics.blit(c.texture(), x, y - 1, x + EMOJI_SIZE, y - 1 + EMOJI_SIZE, 0f, 1f, 0f, 1f);
                case EmojiSprite.Animated a -> {
                    AnimatedEmojiRegistry.AnimatedEmoji ae = AnimatedEmojiRegistry.INSTANCE.byCodepoint(a.codepoint());
                    if (ae != null) graphics.blit(ae.currentFrame(), x, y - 1, x + EMOJI_SIZE, y - 1 + EMOJI_SIZE, 0f, 1f, 0f, 1f);
                }
            }
            String trimmed = font.plainSubstrByWidth(text, MAX_LABEL_WIDTH);
            if (trimmed.length() < text.length()) trimmed = font.plainSubstrByWidth(text, MAX_LABEL_WIDTH - font.width("...")) + "...";
            graphics.text(font, trimmed, x + EMOJI_COL, y, color);
        } else {
            graphics.text(font, text, x, y, color);
        }
    }
}
