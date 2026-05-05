package com.leclowndu93150.twemoji.mixin.client.chat;

import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public interface CommandSuggestionsAccessor {

    @Accessor Font getFont();
    @Accessor EditBox getInput();
    @Accessor Screen getScreen();
    @Accessor int getFillColor();
    @Accessor int getSuggestionLineLimit();
    @Accessor boolean getAnchorToBottom();

    @Accessor CompletableFuture<Suggestions> getPendingSuggestions();
    @Accessor void setPendingSuggestions(CompletableFuture<Suggestions> pendingSuggestions);

    @Accessor("suggestions") CommandSuggestions.SuggestionsList getSuggestionsList();
    @Accessor("suggestions") void setSuggestionsList(CommandSuggestions.SuggestionsList suggestions);
}
