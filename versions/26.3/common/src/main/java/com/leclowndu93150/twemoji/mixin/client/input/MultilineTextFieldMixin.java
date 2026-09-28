package com.leclowndu93150.twemoji.mixin.client.input;

import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultilineTextField.class)
public abstract class MultilineTextFieldMixin {

    @Shadow public abstract String value();

    @Shadow public abstract void setValue(String value, boolean allowOverflowLineLimit);

    @Unique
    private boolean twemoji$inLiveConvert = false;

    @Inject(method = "insertText", at = @At("TAIL"))
    private void twemoji$onInsertText(String input, CallbackInfo ci) {
        if (twemoji$inLiveConvert) return;
        if (input == null || input.isEmpty()) return;
        char trigger = input.charAt(input.length() - 1);
        if (trigger != ':' && !Character.isWhitespace(trigger)) return;

        String value = value();
        if (value.isEmpty()) return;

        String converted = EmojiRegistry.INSTANCE.applyShortcodes(value, EmojiConfig.get().getSkinTone());
        if (converted.equals(value)) return;

        twemoji$inLiveConvert = true;
        try {
            setValue(converted, true);
        } finally {
            twemoji$inLiveConvert = false;
        }
    }
}
