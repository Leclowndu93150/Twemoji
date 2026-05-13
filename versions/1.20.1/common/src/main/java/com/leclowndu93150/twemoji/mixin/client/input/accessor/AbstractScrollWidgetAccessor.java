package com.leclowndu93150.twemoji.mixin.client.input.accessor;

import net.minecraft.client.gui.components.AbstractScrollWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractScrollWidget.class)
public interface AbstractScrollWidgetAccessor {

    @Invoker("scrollAmount")
    double twemoji$scrollAmount();
}
