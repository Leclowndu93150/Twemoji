package com.leclowndu93150.twemoji.mixin.client.modlist;

import com.leclowndu93150.twemoji.Twemoji;
import com.leclowndu93150.twemoji.client.render.AnimatedLogo;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.gui.widget.ModListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jetbrains.annotations.Nullable;

@Mixin(ModListScreen.class)
public class ModListScreenMixin {

    @Shadow
    @Nullable
    private ModListWidget.ModEntry selected;

    @Inject(method = "updateCache", at = @At("TAIL"))
    private void twemoji$markSelected(CallbackInfo ci) {
        AnimatedLogo.setSelectedIsOurs(selected != null && Twemoji.MOD_ID.equals(selected.getInfo().getModId()));
    }
}
