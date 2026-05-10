package com.leclowndu93150.twemoji.mixin.client.modmenu;

import com.leclowndu93150.twemoji.Twemoji;
import com.leclowndu93150.twemoji.client.AnimatedLogo;
import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import com.terraformersmc.modmenu.util.mod.Mod;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModListEntry.class)
public class ModListEntryMixin {

    @Shadow @Final public Mod mod;

    @Inject(method = "getIconTexture", at = @At("HEAD"), cancellable = true)
    private void twemoji$animatedIcon(CallbackInfoReturnable<Identifier> cir) {
        if (!Twemoji.MOD_ID.equals(mod.getId())) return;
        if (!AnimatedLogo.hasFrames()) return;
        Identifier frame = AnimatedLogo.currentFrameId();
        if (frame != null) cir.setReturnValue(frame);
    }
}
