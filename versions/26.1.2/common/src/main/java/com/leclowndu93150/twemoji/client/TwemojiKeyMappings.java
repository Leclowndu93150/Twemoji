package com.leclowndu93150.twemoji.client;

import com.leclowndu93150.twemoji.Twemoji;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class TwemojiKeyMappings {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(Twemoji.MOD_ID, "main")
    );

    public static final KeyMapping OPEN_PICKER = new KeyMapping(
        "twemoji.key.picker",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_E,
        CATEGORY
    );

    private TwemojiKeyMappings() {}
}
