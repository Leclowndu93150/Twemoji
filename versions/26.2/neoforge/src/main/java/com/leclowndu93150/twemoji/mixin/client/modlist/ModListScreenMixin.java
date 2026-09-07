package com.leclowndu93150.twemoji.mixin.client.modlist;

import com.leclowndu93150.twemoji.Twemoji;
import com.leclowndu93150.twemoji.client.render.AnimatedLogo;
import net.neoforged.neoforge.client.gui.modlist.ModDisplayInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.neoforged.neoforge.client.gui.modlist.ModListScreen$ModInfoPanel")
public abstract class ModListScreenMixin {

    @Invoker("displayInfo")
    abstract ModDisplayInfo twemoji$displayInfo();

    @Inject(method = "update", at = @At("RETURN"))
    private void twemoji$markSelected(CallbackInfo ci) {
        AnimatedLogo.setSelectedIsOurs(Twemoji.MOD_ID.equals(twemoji$displayInfo().id()));
    }
}
