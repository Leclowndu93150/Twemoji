package com.leclowndu93150.twemoji.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leclowndu93150.twemoji.Twemoji;
import com.leclowndu93150.twemoji.network.payload.SyncedEmoji;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ServerEmojiLoader extends SimplePreparableReloadListener<ServerEmojiLoader.LoadedData> {

    public static final ServerEmojiLoader INSTANCE = new ServerEmojiLoader();

    public static final int STATIC_CODEPOINT_START = 0xF1000;
    public static final int ANIMATED_CODEPOINT_START = 0xF5000;
    public static final int CODEPOINT_LIMIT = 0xFFFFD;

    private static final String EMOJI_PATH = "twemoji/emoji";
    private static final Path URL_CACHE_DIR = Path.of("twemoji_url_cache");
    private static final Duration URL_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_PARALLEL_DOWNLOADS = 8;

    private volatile List<SyncedEmoji> staticEmojis = List.of();
    private volatile List<SyncedEmoji> animatedEmojis = List.of();
    private volatile Map<String, String> categoryIcons = Map.of();

    private ServerEmojiLoader() {}

    public record LoadedData(List<SyncedEmoji> staticEmojis, List<SyncedEmoji> animatedEmojis, Map<String, String> categoryIcons) {}

    @Override
    protected LoadedData prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Resource> jsonFiles = manager.listResources(EMOJI_PATH, id -> id.getPath().endsWith(".json") && !id.getPath().endsWith("categories.json"));
        Map<ResourceLocation, Resource> pngs = manager.listResources(EMOJI_PATH, id -> id.getPath().endsWith(".png"));
        Map<ResourceLocation, Resource> mcmetas = manager.listResources(EMOJI_PATH, id -> id.getPath().endsWith(".png.mcmeta"));

        Set<ResourceLocation> animatedPngIds = new HashSet<>();
        Map<ResourceLocation, ResourceLocation> mcmetaByPng = new HashMap<>();
        for (ResourceLocation metaId : mcmetas.keySet()) {
            String path = metaId.getPath();
            String pngPath = path.substring(0, path.length() - ".mcmeta".length());
            ResourceLocation pngId = ResourceLocation.fromNamespaceAndPath(metaId.getNamespace(), pngPath);
            animatedPngIds.add(pngId);
            mcmetaByPng.put(pngId, metaId);
        }

        Map<String, Integer> nameCounts = new HashMap<>();
        List<SyncedEmoji> staticOut = new ArrayList<>();
        List<SyncedEmoji> animatedOut = new ArrayList<>();
        int nextStaticCodepoint = STATIC_CODEPOINT_START;
        int nextAnimatedCodepoint = ANIMATED_CODEPOINT_START;

        Set<String> claimedByDescriptor = new HashSet<>();

        List<EmojiDescriptor> descriptors = loadDescriptors(manager, jsonFiles);
        List<String> urlsToFetch = new ArrayList<>();
        for (EmojiDescriptor d : descriptors) {
            if (d.url != null) urlsToFetch.add(d.url);
        }
        Map<String, FetchedAsset> downloaded = downloadAll(urlsToFetch);

        for (EmojiDescriptor d : descriptors) {
            byte[] pngBytes = null;
            boolean animated = false;
            String mcmeta = "";

            if (d.url != null) {
                FetchedAsset asset = downloaded.get(d.url);
                if (asset == null) {
                    Twemoji.LOGGER.warn("Skipping emoji '{}' — failed to fetch {}", d.name, d.url);
                    continue;
                }
                pngBytes = asset.pngBytes();
                animated = asset.animated();
                mcmeta = asset.mcmetaJson();
            } else if (d.image != null) {
                ResourceLocation imageId = resolveImageId(d.image);
                if (imageId == null) {
                    Twemoji.LOGGER.warn("Skipping emoji '{}' — invalid image reference '{}'", d.name, d.image);
                    continue;
                }
                Optional<Resource> res = manager.getResource(imageId);
                if (res.isEmpty()) {
                    Twemoji.LOGGER.warn("Skipping emoji '{}' — image not found: {}", d.name, imageId);
                    continue;
                }
                try (InputStream is = res.get().open()) {
                    pngBytes = readAll(is);
                } catch (IOException e) {
                    Twemoji.LOGGER.warn("Failed to read image for emoji '{}'", d.name, e);
                    continue;
                }
                ResourceLocation pngId = imageId;
                animated = animatedPngIds.contains(pngId);
                if (animated) {
                    ResourceLocation metaId = mcmetaByPng.get(pngId);
                    if (metaId != null) {
                        Optional<Resource> metaRes = manager.getResource(metaId);
                        if (metaRes.isPresent()) {
                            try (InputStream is = metaRes.get().open()) {
                                mcmeta = new String(readAll(is), StandardCharsets.UTF_8);
                            } catch (IOException e) {
                                Twemoji.LOGGER.warn("Failed to read mcmeta for emoji '{}'", d.name, e);
                            }
                        }
                    }
                }
                claimedByDescriptor.add(imageId.toString());
            } else {
                Twemoji.LOGGER.warn("Skipping emoji '{}' — descriptor has neither 'url' nor 'image'", d.name);
                continue;
            }

            if (d.animated != null) animated = d.animated;

            String chosenName = resolveName(d.name, nameCounts, d.source);
            List<String> aliases = d.aliases != null ? d.aliases : List.of();
            String category = d.category != null ? d.category : "";

            if (animated) {
                animatedOut.add(new SyncedEmoji(chosenName, nextAnimatedCodepoint++, true, mcmeta, pngBytes, category, aliases));
            } else {
                staticOut.add(new SyncedEmoji(chosenName, nextStaticCodepoint++, false, "", pngBytes, category, aliases));
            }
        }

        List<ResourceLocation> sortedPngs = new ArrayList<>(pngs.keySet());
        sortedPngs.sort(Comparator.comparing(ResourceLocation::toString));

        for (ResourceLocation pngId : sortedPngs) {
            if (claimedByDescriptor.contains(pngId.toString())) continue;
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
                ResourceLocation metaId = mcmetaByPng.get(pngId);
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

        Map<String, String> categoryIcons = loadCategoryIcons(manager);
        return new LoadedData(staticOut, animatedOut, categoryIcons);
    }

    private static List<EmojiDescriptor> loadDescriptors(ResourceManager manager, Map<ResourceLocation, Resource> jsonFiles) {
        List<ResourceLocation> sorted = new ArrayList<>(jsonFiles.keySet());
        sorted.sort(Comparator.comparing(ResourceLocation::toString));

        List<EmojiDescriptor> result = new ArrayList<>();
        for (ResourceLocation id : sorted) {
            String path = id.getPath();
            int prefixLen = (EMOJI_PATH + "/").length();
            if (!path.startsWith(EMOJI_PATH + "/")) continue;
            String filename = path.substring(prefixLen, path.length() - ".json".length());
            if (filename.contains("/")) continue;

            try (InputStream is = jsonFiles.get(id).open()) {
                JsonElement parsed = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                if (!parsed.isJsonObject()) continue;
                JsonObject json = parsed.getAsJsonObject();

                EmojiDescriptor d = new EmojiDescriptor();
                d.source = id;
                d.name = json.has("name") ? json.get("name").getAsString() : filename;
                d.url = json.has("url") ? json.get("url").getAsString() : null;
                d.image = json.has("image") ? json.get("image").getAsString() : null;
                d.category = json.has("category") ? json.get("category").getAsString() : null;
                d.animated = json.has("animated") ? json.get("animated").getAsBoolean() : null;
                if (json.has("aliases")) {
                    JsonArray arr = json.getAsJsonArray("aliases");
                    List<String> aliases = new ArrayList<>();
                    for (JsonElement el : arr) aliases.add(el.getAsString());
                    d.aliases = aliases;
                }
                result.add(d);
            } catch (Exception e) {
                Twemoji.LOGGER.warn("Failed to parse emoji descriptor {}", id, e);
            }
        }
        return result;
    }

    private static ResourceLocation resolveImageId(String image) {
        if (image.contains(":")) {
            String[] parts = image.split(":", 2);
            return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        }
        return null;
    }

    private record FetchedAsset(byte[] pngBytes, boolean animated, String mcmetaJson) {}

    private static class EmojiDescriptor {
        ResourceLocation source;
        String name;
        String url;
        String image;
        String category;
        Boolean animated;
        List<String> aliases;
    }

    private static Map<String, FetchedAsset> downloadAll(List<String> urls) {
        if (urls.isEmpty()) return Map.of();
        try {
            Files.createDirectories(URL_CACHE_DIR);
        } catch (IOException e) {
            Twemoji.LOGGER.warn("Failed to create url cache dir", e);
        }
        Set<String> unique = new HashSet<>(urls);
        HttpClient client = HttpClient.newBuilder().connectTimeout(URL_TIMEOUT).build();
        ExecutorService pool = Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS, r -> {
            Thread t = new Thread(r, "TwemojiUrlFetch");
            t.setDaemon(true);
            return t;
        });
        Map<String, FetchedAsset> results = new HashMap<>();
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String url : unique) {
                futures.add(CompletableFuture.runAsync(() -> {
                    FetchedAsset asset = downloadWithCache(client, url);
                    if (asset != null) {
                        synchronized (results) {
                            results.put(url, asset);
                        }
                    }
                }, pool));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } finally {
            pool.shutdown();
            try {
                pool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        return results;
    }

    private static FetchedAsset downloadWithCache(HttpClient client, String url) {
        String hash = sha256(url);
        Path cachedPng = URL_CACHE_DIR.resolve(hash + ".png");
        Path cachedMeta = URL_CACHE_DIR.resolve(hash + ".mcmeta");
        if (Files.exists(cachedPng)) {
            try {
                byte[] bytes = Files.readAllBytes(cachedPng);
                if (isPng(bytes)) {
                    String mcmeta = Files.exists(cachedMeta) ? Files.readString(cachedMeta, StandardCharsets.UTF_8) : "";
                    boolean animated = !mcmeta.isEmpty();
                    return new FetchedAsset(bytes, animated, mcmeta);
                }
                Files.deleteIfExists(cachedPng);
                Files.deleteIfExists(cachedMeta);
            } catch (IOException e) {
                Twemoji.LOGGER.warn("Failed to read cached emoji for {}", url, e);
            }
        }
        try {
            Twemoji.LOGGER.info("Downloading emoji from {}", url);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(URL_TIMEOUT).GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2) {
                Twemoji.LOGGER.warn("Failed to download {}: HTTP {}", url, response.statusCode());
                return null;
            }
            FetchedAsset asset = ensurePng(response.body(), url);
            if (asset == null) return null;
            try {
                Files.write(cachedPng, asset.pngBytes());
                if (asset.animated()) {
                    Files.writeString(cachedMeta, asset.mcmetaJson(), StandardCharsets.UTF_8);
                } else {
                    Files.deleteIfExists(cachedMeta);
                }
            } catch (IOException e) {
                Twemoji.LOGGER.warn("Failed to write cache for {}", url, e);
            }
            return asset;
        } catch (Exception e) {
            Twemoji.LOGGER.warn("Failed to download {}", url, e);
            return null;
        }
    }

    private static FetchedAsset ensurePng(byte[] data, String url) {
        if (isGif(data)) {
            FetchedAsset sheet = gifToSpriteSheet(data, url);
            if (sheet != null) return sheet;
        }
        if (isPng(data)) return new FetchedAsset(data, false, "");
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                Twemoji.LOGGER.warn("Cannot decode image from {} (unsupported format)", url);
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return new FetchedAsset(out.toByteArray(), false, "");
        } catch (IOException e) {
            Twemoji.LOGGER.warn("Failed to convert {} to PNG", url, e);
            return null;
        }
    }

    private static FetchedAsset gifToSpriteSheet(byte[] gifData, String url) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(gifData))) {
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(iis, false);
            int frameCount = reader.getNumImages(true);
            if (frameCount <= 0) return null;

            int canvasWidth = 0;
            int canvasHeight = 0;
            for (int i = 0; i < frameCount; i++) {
                IIOMetadata meta = reader.getImageMetadata(i);
                int[] offsets = readGifOffsets(meta);
                BufferedImage frame = reader.read(i);
                canvasWidth = Math.max(canvasWidth, offsets[0] + frame.getWidth());
                canvasHeight = Math.max(canvasHeight, offsets[1] + frame.getHeight());
            }
            int frameSize = Math.max(canvasWidth, canvasHeight);

            BufferedImage canvas = new BufferedImage(frameSize, frameSize, BufferedImage.TYPE_INT_ARGB);
            BufferedImage previousCanvas = null;
            BufferedImage sheet = new BufferedImage(frameSize, frameSize * frameCount, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sheetG = sheet.createGraphics();
            int xOffset = (frameSize - canvasWidth) / 2;
            int yOffset = (frameSize - canvasHeight) / 2;
            List<Integer> delaysCs = new ArrayList<>(frameCount);

            for (int i = 0; i < frameCount; i++) {
                IIOMetadata meta = reader.getImageMetadata(i);
                int[] offsets = readGifOffsets(meta);
                String disposal = readGifDisposal(meta);
                delaysCs.add(readGifDelayCs(meta));
                BufferedImage frame = reader.read(i);

                if ("restoreToPrevious".equals(disposal) && previousCanvas != null) {
                    Graphics2D restoreG = canvas.createGraphics();
                    restoreG.setComposite(java.awt.AlphaComposite.Src);
                    restoreG.drawImage(previousCanvas, 0, 0, null);
                    restoreG.dispose();
                } else if ("restoreToBackgroundColor".equals(disposal)) {
                    Graphics2D bgG = canvas.createGraphics();
                    bgG.setComposite(java.awt.AlphaComposite.Clear);
                    bgG.fillRect(0, 0, frameSize, frameSize);
                    bgG.dispose();
                }

                if ("restoreToPrevious".equals(disposal)) {
                    previousCanvas = deepCopy(canvas);
                }

                Graphics2D drawG = canvas.createGraphics();
                drawG.drawImage(frame, xOffset + offsets[0], yOffset + offsets[1], null);
                drawG.dispose();

                sheetG.drawImage(canvas, 0, i * frameSize, null);
            }
            sheetG.dispose();
            reader.dispose();

            ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
            ImageIO.write(sheet, "png", pngOut);
            String mcmeta = buildMcmeta(delaysCs);
            Twemoji.LOGGER.info("Converted GIF {} to {}-frame sheet", url, frameCount);
            return new FetchedAsset(pngOut.toByteArray(), true, mcmeta);
        } catch (Exception e) {
            Twemoji.LOGGER.warn("Failed to decode GIF {}", url, e);
            return null;
        }
    }

    private static BufferedImage deepCopy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        Graphics2D g = copy.createGraphics();
        g.setComposite(java.awt.AlphaComposite.Src);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static int[] readGifOffsets(IIOMetadata meta) {
        try {
            IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(meta.getNativeMetadataFormatName());
            IIOMetadataNode descriptor = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
            int x = Integer.parseInt(descriptor.getAttribute("imageLeftPosition"));
            int y = Integer.parseInt(descriptor.getAttribute("imageTopPosition"));
            return new int[] {x, y};
        } catch (Exception e) {
            return new int[] {0, 0};
        }
    }

    private static String readGifDisposal(IIOMetadata meta) {
        try {
            IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(meta.getNativeMetadataFormatName());
            IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
            return gce == null ? "none" : gce.getAttribute("disposalMethod");
        } catch (Exception e) {
            return "none";
        }
    }

    private static int readGifDelayCs(IIOMetadata meta) {
        try {
            IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(meta.getNativeMetadataFormatName());
            IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
            if (gce == null) return 10;
            int cs = Integer.parseInt(gce.getAttribute("delayTime"));
            return cs <= 0 ? 10 : cs;
        } catch (Exception e) {
            return 10;
        }
    }

    private static String buildMcmeta(List<Integer> delaysCs) {
        StringBuilder frames = new StringBuilder();
        frames.append('[');
        for (int i = 0; i < delaysCs.size(); i++) {
            if (i > 0) frames.append(',');
            int ticks = Math.max(1, Math.round(delaysCs.get(i) / 5.0f));
            frames.append("{\"index\":").append(i).append(",\"time\":").append(ticks).append('}');
        }
        frames.append(']');
        return "{\"animation\":{\"frametime\":1,\"frames\":" + frames + "}}";
    }

    private static Map<String, String> loadCategoryIcons(ResourceManager manager) {
        Map<ResourceLocation, Resource> jsonFiles = manager.listResources("twemoji", id -> id.getPath().endsWith("/categories.json"));
        Map<String, String> result = new LinkedHashMap<>();
        List<ResourceLocation> sorted = new ArrayList<>(jsonFiles.keySet());
        sorted.sort(Comparator.comparing(ResourceLocation::toString));
        for (ResourceLocation id : sorted) {
            try (InputStream is = jsonFiles.get(id).open()) {
                JsonElement parsed = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                if (!parsed.isJsonArray()) continue;
                for (JsonElement el : parsed.getAsJsonArray()) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    if (!obj.has("name")) continue;
                    String name = obj.get("name").getAsString();
                    String icon = obj.has("icon") ? obj.get("icon").getAsString() : "";
                    result.putIfAbsent(name, icon);
                }
            } catch (Exception e) {
                Twemoji.LOGGER.warn("Failed to parse categories.json at {}", id, e);
            }
        }
        return result;
    }

    private static boolean isPng(byte[] data) {
        return data.length >= 8
            && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G'
            && data[4] == 0x0D && data[5] == 0x0A && data[6] == 0x1A && data[7] == 0x0A;
    }

    private static boolean isGif(byte[] data) {
        return data.length >= 6
            && data[0] == 'G' && data[1] == 'I' && data[2] == 'F'
            && data[3] == '8' && (data[4] == '7' || data[4] == '9') && data[5] == 'a';
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String resolveName(String desired, Map<String, Integer> counts, ResourceLocation source) {
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
        this.categoryIcons = Map.copyOf(data.categoryIcons());
        Twemoji.LOGGER.info("Loaded {} static and {} animated server emojis", staticEmojis.size(), animatedEmojis.size());
    }

    public List<SyncedEmoji> staticEmojis() {
        return staticEmojis;
    }

    public List<SyncedEmoji> animatedEmojis() {
        return animatedEmojis;
    }

    public Map<String, String> categoryIcons() {
        return categoryIcons;
    }

    public List<SyncedEmoji> all() {
        List<SyncedEmoji> combined = new ArrayList<>(staticEmojis.size() + animatedEmojis.size());
        combined.addAll(staticEmojis);
        combined.addAll(animatedEmojis);
        return combined;
    }

    public synchronized SyncedEmoji addOrReplaceStatic(String name, byte[] pngBytes, String category, List<String> aliases) {
        List<SyncedEmoji> updated = new ArrayList<>(staticEmojis);
        int existingIndex = -1;
        int existingCodepoint = -1;
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).name().equals(name)) {
                existingIndex = i;
                existingCodepoint = updated.get(i).codepoint();
                break;
            }
        }
        int codepoint = existingCodepoint != -1 ? existingCodepoint : nextAvailableStaticCodepoint(updated);
        SyncedEmoji entry = new SyncedEmoji(name, codepoint, false, "", pngBytes, category, aliases);
        if (existingIndex >= 0) {
            updated.set(existingIndex, entry);
        } else {
            updated.add(entry);
        }
        this.staticEmojis = Collections.unmodifiableList(updated);
        return entry;
    }

    private static int nextAvailableStaticCodepoint(List<SyncedEmoji> emojis) {
        int next = STATIC_CODEPOINT_START;
        Set<Integer> used = new HashSet<>();
        for (SyncedEmoji e : emojis) used.add(e.codepoint());
        while (used.contains(next)) next++;
        return next;
    }
}
