package com.leclowndu93150.twemoji.client.compat;

import net.minecraft.client.multiplayer.chat.GuiMessage;

import java.lang.reflect.Method;

public final class ChatHeadsCompat {

    private static final boolean AVAILABLE;
    private static final Method GET_HEAD_DATA;
    private static final Method GET_CHAT_OFFSET;

    private static final Method GET_TEXT_WIDTH_DIFFERENCE;
    private static final Method CODE_POINT_INDEX;

    static {
        Method getHeadData = null;
        Method getChatOffset = null;
        Method getTextWidthDifference = null;
        Method codePointIndex = null;
        boolean available = false;
        try {
            Class<?> chatHeads = Class.forName("dzwdz.chat_heads.ChatHeads");
            getHeadData = chatHeads.getDeclaredMethod("getHeadData", GuiMessage.Line.class);
            Class<?> headData = Class.forName("dzwdz.chat_heads.HeadData");
            getChatOffset = chatHeads.getDeclaredMethod("getChatOffset", headData);
            getTextWidthDifference = chatHeads.getDeclaredMethod("getTextWidthDifference", headData);
            codePointIndex = headData.getDeclaredMethod("codePointIndex");
            getHeadData.setAccessible(true);
            getChatOffset.setAccessible(true);
            getTextWidthDifference.setAccessible(true);
            codePointIndex.setAccessible(true);
            available = true;
        } catch (Throwable ignored) {
        }
        GET_HEAD_DATA = getHeadData;
        GET_CHAT_OFFSET = getChatOffset;
        GET_TEXT_WIDTH_DIFFERENCE = getTextWidthDifference;
        CODE_POINT_INDEX = codePointIndex;
        AVAILABLE = available;
    }

    private ChatHeadsCompat() {
    }

    public record HeadInsert(int index, int width) {
        public static final HeadInsert NONE = new HeadInsert(-1, 0);
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

    public static HeadInsert headInsert(GuiMessage.Line line) {
        if (!AVAILABLE || line == null) return HeadInsert.NONE;
        try {
            Object headData = GET_HEAD_DATA.invoke(null, line);
            int inlineWidth = (int) GET_TEXT_WIDTH_DIFFERENCE.invoke(null, headData) - (int) GET_CHAT_OFFSET.invoke(null, headData);
            if (inlineWidth <= 0) return HeadInsert.NONE;
            return new HeadInsert(Math.max((int) CODE_POINT_INDEX.invoke(headData), 0), inlineWidth);
        } catch (Throwable t) {
            return HeadInsert.NONE;
        }
    }
}
