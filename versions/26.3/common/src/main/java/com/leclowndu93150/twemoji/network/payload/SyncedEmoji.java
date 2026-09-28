package com.leclowndu93150.twemoji.network.payload;

import java.util.List;

public record SyncedEmoji(String name, int codepoint, boolean animated, String mcmetaJson, byte[] pngBytes, String category, List<String> aliases) {

    public SyncedEmoji(String name, int codepoint, boolean animated, String mcmetaJson, byte[] pngBytes) {
        this(name, codepoint, animated, mcmetaJson, pngBytes, "", List.of());
    }
}
