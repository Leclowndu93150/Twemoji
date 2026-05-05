package com.leclowndu93150.twemoji.mixin.client.input;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net/minecraft/client/gui/components/MultilineTextField$StringView")
public interface MultilineTextFieldStringViewAccessor {
    @Accessor("beginIndex")
    int twemoji$beginIndex();

    @Accessor("endIndex")
    int twemoji$endIndex();
}
