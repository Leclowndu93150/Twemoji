package com.leclowndu93150.twemoji;

import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import com.leclowndu93150.twemoji.server.ServerEmojiLoader;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class TwemojiFabricRegistries {

    public static final IdentifiableResourceReloadListener EMOJI_REGISTRY = new ClientReloader();
    public static final IdentifiableResourceReloadListener SERVER_EMOJI_LOADER = new ServerReloader();

    private TwemojiFabricRegistries() {}

    private static final class ClientReloader implements IdentifiableResourceReloadListener {
        private static final ResourceLocation ID = new ResourceLocation(Twemoji.MOD_ID, "emoji_registry");

        @Override
        public ResourceLocation getFabricId() {
            return ID;
        }

        @Override
        public Collection<ResourceLocation> getFabricDependencies() {
            return List.of(ResourceReloadListenerKeys.FONTS);
        }

        @Override
        public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager,
                                              ProfilerFiller preparationProfiler, ProfilerFiller reloadProfiler,
                                              Executor backgroundExecutor, Executor gameExecutor) {
            return EmojiRegistry.INSTANCE.reload(barrier, manager, preparationProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
        }
    }

    private static final class ServerReloader implements IdentifiableResourceReloadListener {
        private static final ResourceLocation ID = new ResourceLocation(Twemoji.MOD_ID, "server_emoji_loader");

        @Override
        public ResourceLocation getFabricId() {
            return ID;
        }

        @Override
        public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager,
                                              ProfilerFiller preparationProfiler, ProfilerFiller reloadProfiler,
                                              Executor backgroundExecutor, Executor gameExecutor) {
            return ServerEmojiLoader.INSTANCE.reload(barrier, manager, preparationProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
        }
    }
}
