package com.leclowndu93150.twemoji.client.command;

import com.leclowndu93150.twemoji.Twemoji;
import com.leclowndu93150.twemoji.client.render.EmojiSprite;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import com.leclowndu93150.twemoji.client.registry.AnimatedEmojiRegistry;

public final class EmojiExporter {

    public static final int MAX_SIZE = 8192;
    public static final int DEFAULT_SIZE = 512;
    public static final String OUTPUT_DIR = "twemoji-render";

    private EmojiExporter() {}

    public static Path exportEmoji(String input, int size) throws IOException {
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_SIZE);
        }
        EmojiRegistry.EmojiEntry entry = resolveEntry(input);
        if (entry == null) {
            throw new IOException("Unknown emoji: " + input);
        }
        int skinTone = EmojiConfig.get().getSkinTone();
        EmojiRegistry.EmojiEntry toned = EmojiRegistry.INSTANCE.entryForTone(entry, skinTone);
        if (toned != null) entry = toned;

        Path outputDir = Minecraft.getInstance().gameDirectory.toPath().resolve(OUTPUT_DIR);
        Files.createDirectories(outputDir);

        String fileBase = sanitizeFileName(EmojiRegistry.innerName(entry));
        int codepoint = entry.character().codePointCount(0, entry.character().length()) == 1
            ? entry.character().codePointAt(0)
            : -1;

        AnimatedEmojiRegistry.AnimatedEmoji animated = codepoint >= 0
            ? AnimatedEmojiRegistry.INSTANCE.byCodepoint(codepoint)
            : null;
        if (animated != null) {
            Path output = outputDir.resolve(fileBase + ".gif");
            exportAnimated(animated, size, output);
            return output;
        }

        NativeImage source = readSpriteImage(entry.spriteForTone(skinTone));
        try {
            NativeImage scaled = scaleNearest(source, size);
            try {
                Path output = outputDir.resolve(fileBase + ".png");
                scaled.writeToFile(output);
                return output;
            } finally {
                scaled.close();
            }
        } finally {
            source.close();
        }
    }

    private static EmojiRegistry.EmojiEntry resolveEntry(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() >= 2 && trimmed.charAt(0) == ':' && trimmed.charAt(trimmed.length() - 1) == ':') {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        EmojiRegistry registry = EmojiRegistry.INSTANCE;
        EmojiRegistry.EmojiEntry direct = registry.get(trimmed);
        if (direct != null) return direct;

        String unshaped = registry.unshape(trimmed);
        for (EmojiRegistry.EmojiEntry entry : registry.getEntries().values()) {
            if (entry.character().equals(unshaped) || entry.character().equals(trimmed)) {
                return entry;
            }
        }

        if (trimmed.codePointCount(0, trimmed.length()) == 1) {
            EmojiRegistry.EmojiEntry byCp = registry.entryForCodepoint(trimmed.codePointAt(0));
            if (byCp != null) return byCp;
        }

        String stripped = stripVariationSelectors(trimmed);
        if (!stripped.equals(trimmed)) {
            EmojiRegistry.EmojiEntry recur = resolveEntry(stripped);
            if (recur != null) return recur;
        }
        return null;
    }

    private static String stripVariationSelectors(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            int cp = input.codePointAt(i);
            if (cp != 0xFE0F && cp != 0xFE0E) sb.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private static NativeImage readSpriteImage(EmojiSprite sprite) throws IOException {
        if (sprite instanceof EmojiSprite.Sheet sheet) return readSheetSprite(sheet);
        if (sprite instanceof EmojiSprite.Custom custom) return readIdentifierImage(custom.texture());
        if (sprite instanceof EmojiSprite.Animated) throw new IOException("Animated sprite handled separately");
        throw new IOException("Unknown sprite type: " + sprite);
    }

    private static NativeImage readSheetSprite(EmojiSprite.Sheet sheet) throws IOException {
        NativeImage full = readIdentifierImage(sheet.texture());
        try {
            int cellW = full.getWidth() / sheet.sheetW() * 18;
            int cellH = full.getHeight() / sheet.sheetH() * 18;
            int srcX = sheet.col() * cellW;
            int srcY = sheet.row() * cellH;
            NativeImage out = new NativeImage(NativeImage.Format.RGBA, cellW, cellH, false);
            full.copyRect(out, srcX, srcY, 0, 0, cellW, cellH, false, false);
            return out;
        } finally {
            full.close();
        }
    }

    private static NativeImage readIdentifierImage(ResourceLocation id) throws IOException {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        Optional<Resource> res = manager.getResource(id);
        if (res.isPresent()) {
            try (InputStream is = res.get().open()) {
                return NativeImage.read(NativeImage.Format.RGBA, is);
            }
        }
        AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(id);
        if (tex instanceof DynamicTexture dyn && dyn.getPixels() != null) {
            NativeImage src = dyn.getPixels();
            NativeImage copy = new NativeImage(NativeImage.Format.RGBA, src.getWidth(), src.getHeight(), false);
            copy.copyFrom(src);
            return copy;
        }
        throw new IOException("Cannot load image for " + id);
    }

    private static NativeImage scaleNearest(NativeImage src, int targetSize) {
        int sw = src.getWidth();
        int sh = src.getHeight();
        if (sw == targetSize && sh == targetSize) {
            NativeImage copy = new NativeImage(NativeImage.Format.RGBA, sw, sh, false);
            copy.copyFrom(src);
            return copy;
        }
        NativeImage out = new NativeImage(NativeImage.Format.RGBA, targetSize, targetSize, false);
        for (int y = 0; y < targetSize; y++) {
            int sy = y * sh / targetSize;
            for (int x = 0; x < targetSize; x++) {
                int sx = x * sw / targetSize;
                out.setPixelRGBA(x, y, src.getPixelRGBA(sx, sy));
            }
        }
        return out;
    }

    private static void exportAnimated(AnimatedEmojiRegistry.AnimatedEmoji animated, int size, Path output) throws IOException {
        int[] schedule = animated.frameSchedule();
        int frameTimeMs = animated.frameTimeMs();
        BufferedImage[] frames = new BufferedImage[animated.frameTextures().size()];
        for (int i = 0; i < animated.frameTextures().size(); i++) {
            ResourceLocation frameId = animated.frameTextures().get(i);
            NativeImage src = readIdentifierImage(frameId);
            try {
                NativeImage scaled = scaleNearest(src, size);
                try {
                    frames[i] = toBufferedImage(scaled);
                } finally {
                    scaled.close();
                }
            } finally {
                src.close();
            }
        }
        writeAnimatedGif(frames, schedule, frameTimeMs, output);
    }

    private static BufferedImage toBufferedImage(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixels[y * w + x] = abgrToArgb(image.getPixelRGBA(x, y));
            }
        }
        buf.setRGB(0, 0, w, h, pixels, 0, w);
        return buf;
    }

    private static int abgrToArgb(int abgr) {
        return (abgr & 0xFF00FF00) | ((abgr & 0x00FF0000) >>> 16) | ((abgr & 0x000000FF) << 16);
    }

    private static void writeAnimatedGif(BufferedImage[] frames, int[] schedule, int frameTimeMs, Path output) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        if (!writers.hasNext()) {
            throw new IOException("No GIF writer available");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);

        Files.deleteIfExists(output);
        try (FileImageOutputStream out = new FileImageOutputStream(output.toFile())) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);

            for (int i = 0; i < schedule.length; i++) {
                int frameIdx = schedule[i];
                BufferedImage frame = frames[frameIdx];
                int delayCs = Math.max(2, frameTimeMs / 10);

                IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, params);
                String metaFormat = metadata.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormat);

                IIOMetadataNode gce = getOrCreate(root, "GraphicControlExtension");
                gce.setAttribute("disposalMethod", "restoreToBackgroundColor");
                gce.setAttribute("userInputFlag", "FALSE");
                gce.setAttribute("transparentColorFlag", "TRUE");
                gce.setAttribute("delayTime", Integer.toString(delayCs));
                gce.setAttribute("transparentColorIndex", "0");

                if (i == 0) {
                    IIOMetadataNode appExtensions = getOrCreate(root, "ApplicationExtensions");
                    IIOMetadataNode appExtension = new IIOMetadataNode("ApplicationExtension");
                    appExtension.setAttribute("applicationID", "NETSCAPE");
                    appExtension.setAttribute("authenticationCode", "2.0");
                    appExtension.setUserObject(new byte[]{0x1, 0x0, 0x0});
                    appExtensions.appendChild(appExtension);
                }

                metadata.setFromTree(metaFormat, root);
                writer.writeToSequence(new IIOImage(frame, null, metadata), params);
            }

            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private static IIOMetadataNode getOrCreate(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }

    private static String sanitizeFileName(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.length() == 0 ? "emoji" : sb.toString();
    }
}
