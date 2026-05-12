package com.leclowndu93150.twemoji.client.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EmojiConfig {

    private static final Gson GSON = new Gson();
    private static final String CONFIG_FILENAME = "twemoji.json";
    private static final int FREQUENT_LIMIT = 32;

    private int skinTone = 0;
    private boolean emoticonsEnabled = true;
    private boolean hoverTooltipsEnabled = true;
    private boolean pickerButtonVisible = true;
    private final List<String> frequentEmojis = new ArrayList<>();

    private static Path configDir;
    private static EmojiConfig instance;

    public static void init(Path configDirectory) {
        configDir = configDirectory;
        instance = load();
    }

    public static EmojiConfig get() {
        if (instance == null) instance = new EmojiConfig();
        return instance;
    }

    public int getSkinTone() {
        return clampTone(skinTone);
    }

    public void setSkinTone(int tone) {
        this.skinTone = clampTone(tone);
        save();
    }

    public boolean isEmoticonsEnabled() {
        return emoticonsEnabled;
    }

    public void setEmoticonsEnabled(boolean value) {
        this.emoticonsEnabled = value;
        save();
    }

    public boolean isHoverTooltipsEnabled() {
        return hoverTooltipsEnabled;
    }

    public void setHoverTooltipsEnabled(boolean value) {
        this.hoverTooltipsEnabled = value;
        save();
    }

    public boolean isPickerButtonVisible() {
        return pickerButtonVisible;
    }

    public void setPickerButtonVisible(boolean value) {
        this.pickerButtonVisible = value;
        save();
    }

    public List<String> getFrequentEmojis() {
        return List.copyOf(frequentEmojis);
    }

    public void recordEmojiUse(String name) {
        if (name == null || name.isEmpty()) return;
        frequentEmojis.remove(name);
        frequentEmojis.add(0, name);
        while (frequentEmojis.size() > FREQUENT_LIMIT) {
            frequentEmojis.remove(frequentEmojis.size() - 1);
        }
        save();
    }

    private static EmojiConfig load() {
        Path file = configDir.resolve(CONFIG_FILENAME);
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                JsonObject obj = GSON.fromJson(r, JsonObject.class);
                EmojiConfig cfg = new EmojiConfig();
                if (obj != null && obj.has("skinTone")) {
                    cfg.skinTone = clampTone(obj.get("skinTone").getAsInt());
                }
                if (obj != null && obj.has("emoticonsEnabled")) {
                    cfg.emoticonsEnabled = obj.get("emoticonsEnabled").getAsBoolean();
                }
                if (obj != null && obj.has("hoverTooltipsEnabled")) {
                    cfg.hoverTooltipsEnabled = obj.get("hoverTooltipsEnabled").getAsBoolean();
                }
                if (obj != null && obj.has("pickerButtonVisible")) {
                    cfg.pickerButtonVisible = obj.get("pickerButtonVisible").getAsBoolean();
                }
                if (obj != null && obj.has("frequentEmojis")) {
                    JsonArray frequent = obj.getAsJsonArray("frequentEmojis");
                    for (JsonElement element : frequent) {
                        String name = element.getAsString();
                        if (!name.isEmpty() && !cfg.frequentEmojis.contains(name)) {
                            cfg.frequentEmojis.add(name);
                        }
                        if (cfg.frequentEmojis.size() >= FREQUENT_LIMIT) break;
                    }
                }
                return cfg;
            } catch (IOException e) {
                return new EmojiConfig();
            }
        }
        return new EmojiConfig();
    }

    private void save() {
        if (configDir == null) return;
        Path file = configDir.resolve(CONFIG_FILENAME);
        try {
            Files.createDirectories(configDir);
            try (Writer w = Files.newBufferedWriter(file)) {
                JsonObject obj = new JsonObject();
                obj.addProperty("skinTone", skinTone);
                obj.addProperty("emoticonsEnabled", emoticonsEnabled);
                obj.addProperty("hoverTooltipsEnabled", hoverTooltipsEnabled);
                obj.addProperty("pickerButtonVisible", pickerButtonVisible);
                JsonArray frequent = new JsonArray();
                for (String name : frequentEmojis) {
                    frequent.add(name);
                }
                obj.add("frequentEmojis", frequent);
                GSON.toJson(obj, w);
            }
        } catch (IOException ignored) {
        }
    }

    private static int clampTone(int tone) {
        if (tone < 0) return 0;
        return Math.min(tone, 5);
    }
}
