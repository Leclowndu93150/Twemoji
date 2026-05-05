package com.leclowndu93150.twemoji.network;

public record SyncedEmoji(String name, int codepoint, boolean animated, String mcmetaJson, byte[] pngBytes) {}
