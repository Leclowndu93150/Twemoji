package com.leclowndu93150.twemoji.client.picker;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class TwemojiKeyMappings {

    public static final String CATEGORY = "twemoji.key.category.main";

    public static final KeyMapping OPEN_PICKER = new KeyMapping(
        "twemoji.key.picker",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_E,
        CATEGORY
    );

    private TwemojiKeyMappings() {}
}
