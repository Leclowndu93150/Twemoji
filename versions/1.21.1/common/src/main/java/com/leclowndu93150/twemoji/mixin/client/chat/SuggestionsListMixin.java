package com.leclowndu93150.twemoji.mixin.client.chat;

import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import com.leclowndu93150.twemoji.client.render.EmojiSprite;
import com.leclowndu93150.twemoji.client.suggestion.EmojiSuggestion;
import com.leclowndu93150.twemoji.mixin.client.chat.accessor.CommandSuggestionsAccessor;
import com.leclowndu93150.twemoji.mixin.client.chat.accessor.SuggestionsListAccessor;
import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CommandSuggestions.SuggestionsList.class)
public class SuggestionsListMixin {

    @Final
    @Shadow private List<Suggestion> suggestionList;
    @Shadow private int offset;
    @Shadow private int current;
    @Dynamic
    @Shadow(aliases = "this$0", remap = false)
    private CommandSuggestions twemoji$outer;

    private static final int EMOJI_SIZE = 10;
    private static final int EMOJI_PAD = 2;
    private static final int EMOJI_COL = EMOJI_SIZE + EMOJI_PAD;
    private static final int MAX_LABEL_WIDTH = 120;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void twemoji$widenForEmojiSuggestions(CommandSuggestions this$0, int p_93957_, int p_93958_, int p_93959_, List p_93960_, boolean p_93961_, CallbackInfo ci) {
        for (Suggestion suggestion : suggestionList) {
            if (suggestion instanceof EmojiSuggestion) {
                ((SuggestionsListAccessor)(Object)this).getRect().setWidth(EMOJI_COL + MAX_LABEL_WIDTH + 1);
                return;
            }
        }
    }

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
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"
        )
    )
    private int twemoji$redirectText(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        int row = (y - 2 - ((SuggestionsListAccessor)(Object)this).getRect().getY()) / 12;
        int idx = row + offset;

        if (idx >= 0 && idx < suggestionList.size() && suggestionList.get(idx) instanceof EmojiSuggestion emoji) {
            EmojiSprite sprite = emoji.getSprite();
            if (sprite != null) sprite.blit(graphics, x, y - 1, EMOJI_SIZE);
            String trimmed = font.plainSubstrByWidth(text, MAX_LABEL_WIDTH);
            if (trimmed.length() < text.length()) trimmed = font.plainSubstrByWidth(text, MAX_LABEL_WIDTH - font.width("...")) + "...";
            return graphics.drawString(font, trimmed, x + EMOJI_COL, y, color);
        }
        return graphics.drawString(font, text, x, y, color);
    }
}
