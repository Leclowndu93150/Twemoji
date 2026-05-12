package com.leclowndu93150.twemoji.client.render;

import com.leclowndu93150.twemoji.client.render.EmojiSprite;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class EmojiRain {

    public static final int MAX_DURATION_SECONDS = 60;
    public static final int MIN_PARTICLE_SIZE = 12;
    public static final int MAX_PARTICLE_SIZE = 24;
    private static final int SPAWN_PER_SECOND = 12;
    private static final float GRAVITY = 0.08f;
    private static final float MAX_FALL_SPEED = 4.5f;

    private static final List<Drop> drops = new ArrayList<>();
    private static final Random random = new Random();
    private static EmojiSprite activeSprite;
    private static long endAtMs;
    private static long lastSpawnMs;
    private static long lastUpdateMs;

    private EmojiRain() {}

    public static void start(EmojiSprite sprite, int durationSeconds) {
        activeSprite = sprite;
        long now = Util.getMillis();
        endAtMs = now + Math.max(1, Math.min(durationSeconds, MAX_DURATION_SECONDS)) * 1000L;
        lastSpawnMs = now;
        lastUpdateMs = now;
    }

    public static boolean isActive() {
        return activeSprite != null && (Util.getMillis() < endAtMs || !drops.isEmpty());
    }

    public static void render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        if (activeSprite == null) return;
        long now = Util.getMillis();
        float deltaSec = (now - lastUpdateMs) / 1000f;
        lastUpdateMs = now;

        if (now < endAtMs) spawn(now, screenWidth);
        update(deltaSec, screenHeight);

        for (Drop drop : drops) {
            activeSprite.blit(graphics, Math.round(drop.x), Math.round(drop.y), drop.size);
        }

        if (drops.isEmpty() && now >= endAtMs) {
            activeSprite = null;
        }
    }

    private static void spawn(long now, int screenWidth) {
        long elapsed = now - lastSpawnMs;
        int target = (int)(elapsed * SPAWN_PER_SECOND / 1000L);
        if (target <= 0) return;
        for (int i = 0; i < target; i++) {
            Drop drop = new Drop();
            drop.size = MIN_PARTICLE_SIZE + random.nextInt(MAX_PARTICLE_SIZE - MIN_PARTICLE_SIZE + 1);
            drop.x = random.nextInt(Math.max(1, screenWidth));
            drop.y = -drop.size;
            drop.vx = (random.nextFloat() - 0.5f) * 0.5f;
            drop.vy = 0.5f + random.nextFloat();
            drops.add(drop);
        }
        lastSpawnMs = now;
    }

    private static void update(float deltaSec, int screenHeight) {
        float steps = Mth.clamp(deltaSec * 60f, 0.1f, 5f);
        Iterator<Drop> it = drops.iterator();
        while (it.hasNext()) {
            Drop drop = it.next();
            drop.vy = Math.min(MAX_FALL_SPEED, drop.vy + GRAVITY * steps);
            drop.x += drop.vx * steps;
            drop.y += drop.vy * steps;
            if (drop.y > screenHeight + drop.size) it.remove();
        }
    }

    private static final class Drop {
        float x;
        float y;
        float vx;
        float vy;
        int size;
    }
}
