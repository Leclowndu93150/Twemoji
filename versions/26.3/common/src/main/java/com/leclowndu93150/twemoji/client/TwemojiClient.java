package com.leclowndu93150.twemoji.client;

import com.leclowndu93150.baguettelib.config.ConfigScreens;
import com.leclowndu93150.twemoji.Twemoji;
import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.config.TwemojiConfigScreen;

import java.nio.file.Path;

public final class TwemojiClient {

    private TwemojiClient() {}

    public static void init(Path configDir) {
        EmojiConfig.init(configDir);
        ConfigScreens.register(Twemoji.MOD_ID, TwemojiConfigScreen::create);
    }
}
