package com.leclowndu93150.twemoji.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leclowndu93150.twemoji.Twemoji;
import com.leclowndu93150.twemoji.network.SyncedEmoji;
import net.minecraft.resources.Identifier;
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

    public static final int STATIC_CODEPOINT_START = 0xE000;
    public static final int ANIMATED_CODEPOINT_START = 0xF000;

    private static final String EMOJI_PATH = "twemoji/emoji";
    private static final String[] RECIPE_PATHS = {"recipe", "recipes"};
    private static final String URL_RECIPE_TYPE = "emojiful:emoji_recipe";
    private static final Path URL_CACHE_DIR = Path.of("twemoji_url_cache");
    private static final Duration URL_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_PARALLEL_DOWNLOADS = 8;

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

        List<UrlRecipe> urlRecipes = loadUrlRecipes(manager);
        Twemoji.LOGGER.info("Found {} URL emoji recipe(s)", urlRecipes.size());
        Map<String, FetchedAsset> downloaded = downloadAll(urlRecipes);
        int registered = 0;
        for (UrlRecipe recipe : urlRecipes) {
            FetchedAsset asset = downloaded.get(recipe.url());
            if (asset == null) {
                Twemoji.LOGGER.warn("Skipping URL emoji {} (no bytes from {})", recipe.name(), recipe.url());
                continue;
            }
            String chosenName = resolveName(recipe.name(), nameCounts, recipe.source());
            if (asset.animated()) {
                animatedOut.add(new SyncedEmoji(chosenName, nextAnimatedCodepoint++, true, asset.mcmetaJson(), asset.pngBytes()));
            } else {
                staticOut.add(new SyncedEmoji(chosenName, nextStaticCodepoint++, false, "", asset.pngBytes()));
            }
            registered++;
        }
        if (!urlRecipes.isEmpty()) {
            Twemoji.LOGGER.info("Registered {} URL emoji(s) out of {} recipe(s)", registered, urlRecipes.size());
        }

        return new LoadedData(staticOut, animatedOut);
    }

    private record FetchedAsset(byte[] pngBytes, boolean animated, String mcmetaJson) {}

    private record UrlRecipe(String name, String url, Identifier source) {}

    private static List<UrlRecipe> loadUrlRecipes(ResourceManager manager) {
        Map<Identifier, Resource> recipes = new HashMap<>();
        for (String path : RECIPE_PATHS) {
            recipes.putAll(manager.listResources(path, id -> id.getPath().endsWith(".json")));
        }
        Twemoji.LOGGER.info("Scanning {} recipe json file(s) for emoji compat", recipes.size());
        List<UrlRecipe> result = new ArrayList<>();
        int matched = 0;
        int skippedNotEmoji = 0;
        int skippedMalformed = 0;
        for (Map.Entry<Identifier, Resource> entry : recipes.entrySet()) {
            Identifier id = entry.getKey();
            try (InputStream is = entry.getValue().open()) {
                JsonElement parsed = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                if (!parsed.isJsonObject()) { skippedMalformed++; continue; }
                JsonObject json = parsed.getAsJsonObject();
                if (!json.has("type") || !URL_RECIPE_TYPE.equals(json.get("type").getAsString())) { skippedNotEmoji++; continue; }
                if (!json.has("name") || !json.has("url")) { skippedMalformed++; continue; }
                result.add(new UrlRecipe(json.get("name").getAsString(), json.get("url").getAsString(), id));
                matched++;
            } catch (Exception e) {
                Twemoji.LOGGER.warn("Failed to read recipe {}", id, e);
            }
        }
        Twemoji.LOGGER.info("Recipe scan: {} matched emoji recipes, {} non-emoji, {} malformed", matched, skippedNotEmoji, skippedMalformed);
        return result;
    }

    private static Map<String, FetchedAsset> downloadAll(List<UrlRecipe> recipes) {
        if (recipes.isEmpty()) return Map.of();
        try {
            Files.createDirectories(URL_CACHE_DIR);
        } catch (IOException e) {
            Twemoji.LOGGER.warn("Failed to create url cache dir", e);
        }
        Map<String, UrlRecipe> unique = new HashMap<>();
        for (UrlRecipe r : recipes) unique.putIfAbsent(r.url(), r);
        HttpClient client = HttpClient.newBuilder().connectTimeout(URL_TIMEOUT).build();
        ExecutorService pool = Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS, r -> {
            Thread t = new Thread(r, "TwemojiUrlFetch");
            t.setDaemon(true);
            return t;
        });
        Map<String, FetchedAsset> results = new HashMap<>();
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (UrlRecipe recipe : unique.values()) {
                futures.add(CompletableFuture.runAsync(() -> {
                    FetchedAsset asset = downloadWithCache(client, recipe.url(), recipe.source());
                    if (asset != null) {
                        synchronized (results) {
                            results.put(recipe.url(), asset);
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

    private static FetchedAsset downloadWithCache(HttpClient client, String url, Identifier source) {
        String hash = sha256(url);
        Path cachedPng = URL_CACHE_DIR.resolve(hash + ".png");
        Path cachedMeta = URL_CACHE_DIR.resolve(hash + ".mcmeta");
        if (Files.exists(cachedPng)) {
            try {
                byte[] bytes = Files.readAllBytes(cachedPng);
                if (isPng(bytes)) {
                    String mcmeta = Files.exists(cachedMeta) ? Files.readString(cachedMeta, StandardCharsets.UTF_8) : "";
                    boolean animated = !mcmeta.isEmpty();
                    Twemoji.LOGGER.debug("Cache hit: {} ({} bytes, animated={}) for {}", cachedPng.getFileName(), bytes.length, animated, url);
                    return new FetchedAsset(bytes, animated, mcmeta);
                }
                Twemoji.LOGGER.info("Cached file {} is not a valid PNG, redownloading", cachedPng.getFileName());
                Files.deleteIfExists(cachedPng);
                Files.deleteIfExists(cachedMeta);
            } catch (IOException e) {
                Twemoji.LOGGER.warn("Failed to read cached emoji {}", cachedPng, e);
            }
        }
        try {
            Twemoji.LOGGER.info("Downloading emoji from {} (recipe {})", url, source);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(URL_TIMEOUT).GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2) {
                Twemoji.LOGGER.warn("Failed to download {} (recipe {}): HTTP {}", url, source, response.statusCode());
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
                Twemoji.LOGGER.info("Cached {} ({} bytes, animated={}) -> {}", url, asset.pngBytes().length, asset.animated(), cachedPng.getFileName());
            } catch (IOException e) {
                Twemoji.LOGGER.warn("Failed to write cache {}", cachedPng, e);
            }
            return asset;
        } catch (Exception e) {
            Twemoji.LOGGER.warn("Failed to download {} (recipe {})", url, source, e);
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
            Twemoji.LOGGER.info("Converted {} to PNG ({} -> {} bytes)", url, data.length, out.size());
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
            Twemoji.LOGGER.info("Converted GIF {} to {}-frame sheet ({}x{}, {} bytes)", url, frameCount, frameSize, frameSize * frameCount, pngOut.size());
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
