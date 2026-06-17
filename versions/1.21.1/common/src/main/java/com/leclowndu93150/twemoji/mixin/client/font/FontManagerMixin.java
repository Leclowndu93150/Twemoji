package com.leclowndu93150.twemoji.mixin.client.font;

import com.leclowndu93150.twemoji.client.font.TwemojiFontInjection;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(FontManager.class)
public abstract class FontManagerMixin {

    @Inject(method = "reload", at = @At("HEAD"))
    private void twemoji$prepareInjection(
        PreparableReloadListener.PreparationBarrier barrier,
        ResourceManager resourceManager,
        ProfilerFiller prepProfiler,
        ProfilerFiller reloadProfiler,
        Executor prepExecutor,
        Executor reloadExecutor,
        CallbackInfoReturnable<CompletableFuture<Void>> cir
    ) {
        TwemojiFontInjection.prepare(resourceManager, prepExecutor);
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void twemoji$registerForClose(CallbackInfo ci) {
        ((FontManagerAccessor) this).twemoji$providersToClose().addAll(TwemojiFontInjection.drainLoadedProvidersForClose());
    }
}
