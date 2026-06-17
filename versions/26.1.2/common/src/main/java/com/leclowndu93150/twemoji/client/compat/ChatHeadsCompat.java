package com.leclowndu93150.twemoji.client.compat;

import net.minecraft.client.multiplayer.chat.GuiMessage;

import java.lang.reflect.Method;

public final class ChatHeadsCompat {

    private static final boolean AVAILABLE;
    private static final Method GET_HEAD_DATA;
    private static final Method GET_CHAT_OFFSET;

    static {
        Method getHeadData = null;
        Method getChatOffset = null;
        boolean available = false;
        try {
            Class<?> chatHeads = Class.forName("dzwdz.chat_heads.ChatHeads");
            getHeadData = chatHeads.getDeclaredMethod("getHeadData", GuiMessage.Line.class);
            Class<?> headData = Class.forName("dzwdz.chat_heads.HeadData");
            getChatOffset = chatHeads.getDeclaredMethod("getChatOffset", headData);
            getHeadData.setAccessible(true);
            getChatOffset.setAccessible(true);
            available = true;
        } catch (Throwable ignored) {
        }
        GET_HEAD_DATA = getHeadData;
        GET_CHAT_OFFSET = getChatOffset;
        AVAILABLE = available;
    }

    private ChatHeadsCompat() {
    }

    public static int chatOffset(GuiMessage.Line line) {
        if (!AVAILABLE || line == null) return 0;
        try {
            Object headData = GET_HEAD_DATA.invoke(null, line);
            return (int) GET_CHAT_OFFSET.invoke(null, headData);
        } catch (Throwable t) {
            return 0;
        }
    }
}
