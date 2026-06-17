package com.leclowndu93150.twemoji.mixin.client.font;

import com.leclowndu93150.twemoji.client.font.TwemojiFontInjection;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(FontManager.class)
public abstract class FontManagerMixin {

    @Inject(
        method = "reload(Lnet/minecraft/server/packs/resources/PreparableReloadListener$SharedState;Ljava/util/concurrent/Executor;Lnet/minecraft/server/packs/resources/PreparableReloadListener$PreparationBarrier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
        at = @At("HEAD")
    )
    private void twemoji$prepareInjection(
        PreparableReloadListener.SharedState state,
        Executor taskExecutor,
        PreparableReloadListener.PreparationBarrier barrier,
        Executor reloadExecutor,
        CallbackInfoReturnable<CompletableFuture<Void>> cir
    ) {
        TwemojiFontInjection.prepare(state.resourceManager(), taskExecutor);
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void twemoji$registerForClose(CallbackInfo ci) {
        ((FontManagerAccessor) this).twemoji$providersToClose().addAll(TwemojiFontInjection.drainLoadedProvidersForClose());
    }
}
