package com.leclowndu93150.twemoji.server;

import com.leclowndu93150.twemoji.Twemoji;
import com.leclowndu93150.twemoji.network.SyncedEmoji;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ServerEmojiLoader extends SimplePreparableReloadListener<ServerEmojiLoader.LoadedData> {

    public static final ServerEmojiLoader INSTANCE = new ServerEmojiLoader();

    public static final int STATIC_CODEPOINT_START = 0xE000;
    public static final int ANIMATED_CODEPOINT_START = 0xF000;

    private static final String EMOJI_PATH = "twemoji/emoji";

    private volatile List<SyncedEmoji> staticEmojis = List.of();
    private volatile List<SyncedEmoji> animatedEmojis = List.of();

    private ServerEmojiLoader() {}

    public record LoadedData(List<SyncedEmoji> staticEmojis, List<SyncedEmoji> animatedEmojis) {}

    @Override
    protected LoadedData prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, Resource> pngs = manager.listResources(EMOJI_PATH, id -> id.getPath().endsWith(".png"));
        Map<Identifier, Resource> mcmetas = manager.listResources(EMOJI_PATH, id -> id.getPath().endsWith(".png.mcmeta"));

        Set<Identifier> animatedPngIds = new HashSet<>();
        Map<Identifier, Identifier> mcmetaByPng = new HashMap<>();
        for (Identifier metaId : mcmetas.keySet()) {
            String path = metaId.getPath();
            String pngPath = path.substring(0, path.length() - ".mcmeta".length());
            Identifier pngId = Identifier.fromNamespaceAndPath(metaId.getNamespace(), pngPath);
            animatedPngIds.add(pngId);
            mcmetaByPng.put(pngId, metaId);
        }

        List<Identifier> sortedPngs = new ArrayList<>(pngs.keySet());
        sortedPngs.sort(Comparator.comparing(Identifier::toString));

        List<SyncedEmoji> staticOut = new ArrayList<>();
        List<SyncedEmoji> animatedOut = new ArrayList<>();
        Map<String, Integer> nameCounts = new HashMap<>();

        int nextStaticCodepoint = STATIC_CODEPOINT_START;
        int nextAnimatedCodepoint = ANIMATED_CODEPOINT_START;

        for (Identifier pngId : sortedPngs) {
            String path = pngId.getPath();
            int prefixLen = (EMOJI_PATH + "/").length();
            if (!path.startsWith(EMOJI_PATH + "/")) continue;
            String filename = path.substring(prefixLen, path.length() - ".png".length());
            if (filename.contains("/")) continue;

            String chosenName = resolveName(filename, nameCounts, pngId);

            byte[] pngBytes;
            try (InputStream is = pngs.get(pngId).open()) {
                pngBytes = readAll(is);
            } catch (IOException e) {
                Twemoji.LOGGER.warn("Failed to read emoji png {}", pngId, e);
                continue;
            }

            boolean animated = animatedPngIds.contains(pngId);
            String mcmeta = "";
            if (animated) {
                Identifier metaId = mcmetaByPng.get(pngId);
                Optional<Resource> metaRes = manager.getResource(metaId);
                if (metaRes.isPresent()) {
                    try (InputStream is = metaRes.get().open()) {
                        mcmeta = new String(readAll(is), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        Twemoji.LOGGER.warn("Failed to read mcmeta {}", metaId, e);
                    }
                }
            }

            if (animated) {
                animatedOut.add(new SyncedEmoji(chosenName, nextAnimatedCodepoint++, true, mcmeta, pngBytes));
            } else {
                staticOut.add(new SyncedEmoji(chosenName, nextStaticCodepoint++, false, "", pngBytes));
            }
        }

        return new LoadedData(staticOut, animatedOut);
    }

    private String resolveName(String desired, Map<String, Integer> counts, Identifier source) {
        Integer prevCount = counts.get(desired);
        if (prevCount == null) {
            counts.put(desired, 1);
            return desired;
        }
        String chosen = desired + prevCount;
        counts.put(desired, prevCount + 1);
        Twemoji.LOGGER.warn("Duplicate emoji name '{}' from {} renamed to '{}'", desired, source, chosen);
        return chosen;
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }

    @Override
    protected void apply(LoadedData data, ResourceManager manager, ProfilerFiller profiler) {
        this.staticEmojis = Collections.unmodifiableList(data.staticEmojis());
        this.animatedEmojis = Collections.unmodifiableList(data.animatedEmojis());
        Twemoji.LOGGER.info("Loaded {} static and {} animated server emojis", staticEmojis.size(), animatedEmojis.size());
    }

    public List<SyncedEmoji> staticEmojis() {
        return staticEmojis;
    }

    public List<SyncedEmoji> animatedEmojis() {
        return animatedEmojis;
    }

    public List<SyncedEmoji> all() {
        List<SyncedEmoji> combined = new ArrayList<>(staticEmojis.size() + animatedEmojis.size());
        combined.addAll(staticEmojis);
        combined.addAll(animatedEmojis);
        return combined;
    }
}
