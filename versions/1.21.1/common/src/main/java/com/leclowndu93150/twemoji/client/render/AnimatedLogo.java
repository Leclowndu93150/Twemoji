package com.leclowndu93150.twemoji.client.render;

import com.leclowndu93150.twemoji.Twemoji;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AnimatedLogo {

    private static final List<ResourceLocation> FRAME_IDS = new ArrayList<>();
    private static final List<Integer> FRAME_DELAYS_MS = new ArrayList<>();
    private static final List<Integer> CUMULATIVE_MS = new ArrayList<>();
    private static int totalDurationMs = 0;
    private static int frameWidth = 0;
    private static int frameHeight = 0;
    private static boolean loaded = false;
    private static boolean selectedIsOurs = false;

    public static void setSelectedIsOurs(boolean value) {
        selectedIsOurs = value;
    }

    public static boolean isSelectedOurs() {
        return selectedIsOurs;
    }

    private AnimatedLogo() {}

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try (InputStream stream = AnimatedLogo.class.getResourceAsStream("/icon.gif")) {
            if (stream == null) {
                Twemoji.LOGGER.warn("icon.gif not found on classpath");
                return;
            }
            decode(stream);
        } catch (Exception e) {
            Twemoji.LOGGER.warn("Failed to load animated logo", e);
        }
    }

    private static void decode(InputStream stream) throws Exception {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) {
            Twemoji.LOGGER.warn("No GIF reader available for animated logo");
            return;
        }
        ImageReader reader = readers.next();
        try (ImageInputStream input = ImageIO.createImageInputStream(stream)) {
            reader.setInput(input);
            int frameCount = reader.getNumImages(true);
            BufferedImage canvas = null;
            int cumulative = 0;
            for (int i = 0; i < frameCount; i++) {
                BufferedImage frame = reader.read(i);
                int delayCs = readDelayCentiseconds(reader.getImageMetadata(i));
                int delayMs = Math.max(20, delayCs * 10);

                if (canvas == null) {
                    canvas = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    frameWidth = frame.getWidth();
                    frameHeight = frame.getHeight();
                }
                Graphics2D g = canvas.createGraphics();
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(frame, 0, 0, null);
                g.dispose();

                NativeImage image = bufferedToNative(canvas);
                DynamicTexture texture = new DynamicTexture(image);
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Twemoji.MOD_ID, "logo/frame_" + i);
                Minecraft.getInstance().getTextureManager().register(id, texture);

                FRAME_IDS.add(id);
                FRAME_DELAYS_MS.add(delayMs);
                cumulative += delayMs;
                CUMULATIVE_MS.add(cumulative);
            }
            totalDurationMs = Math.max(1, cumulative);
        } finally {
            reader.dispose();
        }
    }

    private static int readDelayCentiseconds(IIOMetadata metadata) {
        if (metadata == null) return 10;
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
        for (int i = 0; i < root.getLength(); i++) {
            if ("GraphicControlExtension".equalsIgnoreCase(root.item(i).getNodeName())) {
                IIOMetadataNode gce = (IIOMetadataNode) root.item(i);
                String delay = gce.getAttribute("delayTime");
                if (delay != null && !delay.isEmpty()) {
                    try {
                        return Integer.parseInt(delay);
                    } catch (NumberFormatException ignored) {}
                }
                return 10;
            }
        }
        return 10;
    }

    private static NativeImage bufferedToNative(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        NativeImage out = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setPixelRGBA(x, y, argbToAbgr(pixels[y * w + x]));
            }
        }
        return out;
    }

    private static int argbToAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb & 0x00FF0000) >>> 16) | ((argb & 0x000000FF) << 16);
    }

    public static ResourceLocation currentFrameId() {
        ensureLoaded();
        if (FRAME_IDS.isEmpty()) return null;
        long t = System.currentTimeMillis() % totalDurationMs;
        for (int i = 0; i < CUMULATIVE_MS.size(); i++) {
            if (t < CUMULATIVE_MS.get(i)) return FRAME_IDS.get(i);
        }
        return FRAME_IDS.get(FRAME_IDS.size() - 1);
    }

    public static boolean hasFrames() {
        ensureLoaded();
        return !FRAME_IDS.isEmpty();
    }

    public static int width() {
        ensureLoaded();
        return frameWidth;
    }

    public static int height() {
        ensureLoaded();
        return frameHeight;
    }
}
