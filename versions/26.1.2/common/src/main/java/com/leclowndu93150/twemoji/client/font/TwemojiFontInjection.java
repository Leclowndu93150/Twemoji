package com.leclowndu93150.twemoji.client.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.leclowndu93150.twemoji.Twemoji;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class TwemojiFontInjection {

    private static final Identifier INTERNAL_FONT_ID = Identifier.fromNamespaceAndPath(Twemoji.MOD_ID, "twemoji_internal/default.json");
    private static final String INTERNAL_FONT_PATH = "/assets/twemoji/twemoji_internal/default.json";

    private static volatile CompletableFuture<List<GlyphProvider.Conditional>> pending = CompletableFuture.completedFuture(List.of());
    private static volatile List<GlyphProvider.Conditional> lastLoaded = List.of();

    private TwemojiFontInjection() {}

    public static void prepare(ResourceManager resourceManager, Executor executor) {
        pending = CompletableFuture.supplyAsync(() -> loadProviders(resourceManager), executor)
            .exceptionally(error -> {
                Twemoji.LOGGER.warn("Failed to prepare injected Twemoji font providers", error);
                return List.of();
            });
    }

    public static List<GlyphProvider.Conditional> awaitProvidersToInject() {
        try {
            List<GlyphProvider.Conditional> loaded = pending.join();
            lastLoaded = loaded;
            return loaded;
        } catch (Exception e) {
            Twemoji.LOGGER.warn("Failed to await injected Twemoji font providers", e);
            return List.of();
        }
    }

    public static List<GlyphProvider> drainLoadedProvidersForClose() {
        List<GlyphProvider.Conditional> snapshot = lastLoaded;
        if (snapshot.isEmpty()) return List.of();
        lastLoaded = List.of();
        List<GlyphProvider> raw = new ArrayList<>(snapshot.size());
        for (GlyphProvider.Conditional c : snapshot) raw.add(c.provider());
        return raw;
    }

    private static List<GlyphProvider.Conditional> loadProviders(ResourceManager resourceManager) {
        List<GlyphProvider.Conditional> built = new ArrayList<>();
        JsonElement providersElement = readProviders(resourceManager);
        if (providersElement == null) return built;
        for (JsonElement entry : providersElement.getAsJsonArray()) {
            GlyphProviderDefinition.Conditional conditional = GlyphProviderDefinition.Conditional.CODEC
                .parse(JsonOps.INSTANCE, entry)
                .resultOrPartial(error -> Twemoji.LOGGER.warn("Failed to parse injected font provider: {}", error))
                .orElse(null);
            if (conditional == null) continue;
            Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpacked = conditional.definition().unpack();
            GlyphProviderDefinition.Loader loader = unpacked.left().orElse(null);
            if (loader == null) continue;
            try {
                GlyphProvider provider = loader.load(resourceManager);
                built.add(new GlyphProvider.Conditional(provider, conditional.filter()));
            } catch (Exception e) {
                Twemoji.LOGGER.warn("Failed to load injected font provider", e);
            }
        }
        return built;
    }

    private static JsonElement readProviders(ResourceManager resourceManager) {
        try (Reader reader = resourceManager.openAsReader(INTERNAL_FONT_ID)) {
            JsonElement providersElement = JsonParser.parseReader(reader).getAsJsonObject().get("providers");
            if (providersElement != null) return providersElement;
            Twemoji.LOGGER.warn("Injected Twemoji font JSON has no providers array: {}", INTERNAL_FONT_ID);
        } catch (Exception e) {
            Twemoji.LOGGER.warn("Failed to read injected Twemoji font JSON from resource manager: {}", INTERNAL_FONT_ID, e);
        }

        try (InputStream stream = TwemojiFontInjection.class.getResourceAsStream(INTERNAL_FONT_PATH)) {
            if (stream == null) {
                Twemoji.LOGGER.warn("Injected Twemoji font JSON not found on classpath: {}", INTERNAL_FONT_PATH);
                return null;
            }
            JsonElement providersElement = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().get("providers");
            if (providersElement != null) return providersElement;
            Twemoji.LOGGER.warn("Injected Twemoji classpath font JSON has no providers array: {}", INTERNAL_FONT_PATH);
        } catch (Exception e) {
            Twemoji.LOGGER.warn("Failed to read injected Twemoji font JSON from classpath: {}", INTERNAL_FONT_PATH, e);
        }
        return null;
    }
}
