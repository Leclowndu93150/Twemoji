package com.leclowndu93150.twemoji.mixin.client.font;

import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FontSet.class)
public interface FontSetAccessor {

    @Accessor("name")
    ResourceLocation twemoji$name();
}
