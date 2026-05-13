package com.leclowndu93150.twemoji.client.config;

import com.leclowndu93150.twemoji.Twemoji;
import com.leclowndu93150.twemoji.network.payload.SyncedEmoji;
import net.minecraft.client.Minecraft;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ServerEmojiCache {

    private static final String CACHE_DIR = "twemoji-server-cache";
    private static final int MAGIC = 0x54454d4a;
    private static final int VERSION = 1;

    public record Snapshot(List<SyncedEmoji> emojis, Map<String, String> categoryIcons) {}

    private ServerEmojiCache() {}

    public static String sanitizeKey(String host) {
        if (host == null || host.isBlank()) return "_singleplayer";
        StringBuilder sb = new StringBuilder(host.length());
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '.' || c == '_') sb.append(c);
            else sb.append('_');
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    public static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(CACHE_DIR);
    }

    public static Path fileFor(String host) {
        return cacheDir().resolve(sanitizeKey(host) + ".bin");
    }

    public static Snapshot load(String host) {
        Path file = fileFor(host);
        if (!Files.exists(file)) return null;
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            if (in.readInt() != MAGIC) return null;
            int version = in.readInt();
            if (version != VERSION) return null;
            int count = in.readInt();
            List<SyncedEmoji> emojis = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String name = in.readUTF();
                int codepoint = in.readInt();
                boolean animated = in.readBoolean();
                String mcmeta = in.readUTF();
                int pngLen = in.readInt();
                byte[] png = new byte[pngLen];
                in.readFully(png);
                String category = in.readUTF();
                int aliasCount = in.readInt();
                List<String> aliases = new ArrayList<>(aliasCount);
                for (int a = 0; a < aliasCount; a++) aliases.add(in.readUTF());
                emojis.add(new SyncedEmoji(name, codepoint, animated, mcmeta, png, category, aliases));
            }
            int iconCount = in.readInt();
            Map<String, String> icons = new LinkedHashMap<>(iconCount);
            for (int i = 0; i < iconCount; i++) icons.put(in.readUTF(), in.readUTF());
            return new Snapshot(emojis, icons);
        } catch (IOException e) {
            Twemoji.LOGGER.warn("Failed to load emoji cache for {}", host, e);
            return null;
        }
    }

    public static void save(String host, List<SyncedEmoji> emojis, Map<String, String> categoryIcons) {
        try {
            Path dir = cacheDir();
            Files.createDirectories(dir);
            Path file = fileFor(host);
            Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(tmp))) {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.writeInt(emojis.size());
                for (SyncedEmoji e : emojis) {
                    out.writeUTF(e.name());
                    out.writeInt(e.codepoint());
                    out.writeBoolean(e.animated());
                    out.writeUTF(e.mcmetaJson() == null ? "" : e.mcmetaJson());
                    out.writeInt(e.pngBytes().length);
                    out.write(e.pngBytes());
                    out.writeUTF(e.category() == null ? "" : e.category());
                    out.writeInt(e.aliases().size());
                    for (String alias : e.aliases()) out.writeUTF(alias);
                }
                out.writeInt(categoryIcons.size());
                for (Map.Entry<String, String> entry : categoryIcons.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeUTF(entry.getValue());
                }
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Twemoji.LOGGER.warn("Failed to save emoji cache for {}", host, e);
        }
    }
}
