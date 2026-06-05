package com.leclowndu93150.twemoji.client.font;

import com.leclowndu93150.twemoji.Twemoji;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class TwemojiFontInjection {

    private static final ResourceLocation INTERNAL_FONT_ID = new ResourceLocation(Twemoji.MOD_ID, "twemoji_internal/default.json");
    private static final String INTERNAL_FONT_PATH = "/assets/twemoji/twemoji_internal/default.json";

    private static volatile ResourceManager currentManager;

    private TwemojiFontInjection() {}

    public static void setResourceManager(ResourceManager manager) {
        currentManager = manager;
    }

    public static List<GlyphProvider> buildProviders() {
        ResourceManager manager = currentManager;
        if (manager == null) return List.of();
        return buildProviders(manager);
    }

    public static List<GlyphProvider> buildProviders(ResourceManager resourceManager) {
        List<GlyphProvider> built = new ArrayList<>();
        JsonElement providersElement = readProviders(resourceManager);
        if (providersElement == null) return built;
        for (JsonElement entry : providersElement.getAsJsonArray()) {
            GlyphProviderDefinition definition = GlyphProviderDefinition.CODEC
                .parse(JsonOps.INSTANCE, entry)
                .resultOrPartial(error -> Twemoji.LOGGER.warn("Failed to parse injected font provider: {}", error))
                .orElse(null);
            if (definition == null) continue;
            Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpacked = definition.unpack();
            GlyphProviderDefinition.Loader loader = unpacked.left().orElse(null);
            if (loader == null) continue;
            try {
                built.add(loader.load(resourceManager));
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
