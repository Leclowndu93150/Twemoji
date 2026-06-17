package com.leclowndu93150.twemoji.client.tooltip;

import com.leclowndu93150.twemoji.client.compat.ChatHeadsCompat;
import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import com.leclowndu93150.twemoji.client.render.EmojiSprite;
import com.leclowndu93150.twemoji.mixin.client.chat.accessor.ChatComponentAccessor;
import net.minecraft.Util;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class EmojiTooltip {

    private static final int PANEL_COLOR = 0xF0131416;
    private static final int FOOTER_COLOR = 0xF0070709;
    private static final int BORDER_COLOR = 0xFF323337;
    private static final int HOVER_COLOR = 0x26FFFFFF;
    private static final int TEXT_COLOR = 0xFFE9EAEC;
    private static final int MUTED_COLOR = 0xFF9B9DA3;
    private static final int EMOJI_SIZE = 28;
    private static final int PADDING = 6;
    private static final int GAP = 7;
    private static final int HEIGHT = 40;
    private static final int MIN_WIDTH = 112;
    private static final int MAX_TEXT_WIDTH = 150;

    private static long copiedAtMs;
    @Nullable
    private static EmojiRegistry.EmojiEntry copiedEntry;
    private static final int COPY_FLASH_MS = 1200;

    private EmojiTooltip() {
    }

    public static void render(GuiGraphics graphics, Font font, int screenWidth, int screenHeight, @Nullable Hit hit) {
        if (hit == null || !EmojiConfig.get().isHoverTooltipsEnabled()) return;
        EmojiRegistry.EmojiEntry entry = hit.entry();
        String shortcode = entry.shortcode();
        boolean showCopied = copiedEntry == entry && Util.getMillis() - copiedAtMs < COPY_FLASH_MS;
        String label = showCopied ? Component.translatable("twemoji.picker.copied", shortcode).getString() : shortcode;
        int textWidth = Math.min(font.width(label), MAX_TEXT_WIDTH);
        int width = Math.max(MIN_WIDTH, PADDING * 2 + EMOJI_SIZE + GAP + textWidth);
        int anchorX = hit.centerX();
        int anchorY = hit.bottom();
        int x = Mth.clamp(anchorX + 8, 4, Math.max(4, screenWidth - width - 4));
        int y = anchorY + 10;
        if (y + HEIGHT > screenHeight - 4) {
            y = anchorY - HEIGHT - 8;
        }
        y = Mth.clamp(y, 4, Math.max(4, screenHeight - HEIGHT - 4));

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        try {
            graphics.fill(x, y, x + width, y + HEIGHT, PANEL_COLOR);
            graphics.fill(x + 1, y + 1, x + width - 1, y + HEIGHT - 1, FOOTER_COLOR);
            drawBorder(graphics, x, y, width, HEIGHT, BORDER_COLOR);
            renderSprite(graphics, EmojiRegistry.INSTANCE.spriteForTone(entry, EmojiConfig.get().getSkinTone()), x + PADDING, y + (HEIGHT - EMOJI_SIZE) / 2, EMOJI_SIZE);
            int labelColor = showCopied ? 0xFF8AE08A : TEXT_COLOR;
            graphics.drawString(font, trimToWidth(font, label, width - PADDING * 2 - EMOJI_SIZE - GAP), x + PADDING + EMOJI_SIZE + GAP, y + 11, labelColor, false);

            String name = showCopied ? Component.translatable("twemoji.picker.copy_to_clipboard").getString() : EmojiRegistry.innerName(entry);
            graphics.drawString(font, trimToWidth(font, name, width - PADDING * 2 - EMOJI_SIZE - GAP), x + PADDING + EMOJI_SIZE + GAP, y + 22, MUTED_COLOR, false);
        } finally {
            graphics.pose().popPose();
        }
    }

    public static boolean copyShortcode(@Nullable Hit hit) {
        if (hit == null) return false;
        Minecraft.getInstance().keyboardHandler.setClipboard(hit.entry().shortcode());
        copiedEntry = hit.entry();
        copiedAtMs = Util.getMillis();
        return true;
    }

    public static @Nullable Hit hitString(Font font, String text, int x, int y, float scaleX, float scaleY, int mouseX, int mouseY) {
        if (text.isEmpty()) return null;
        int lineHeight = Math.max(1, Math.round(9.0F * scaleY));
        int top = y - 1;
        if (mouseY < top || mouseY >= top + lineHeight) return null;

        int cursorX = x;
        for (int offset = 0; offset < text.length(); ) {
            int codepoint = text.codePointAt(offset);
            int charWidth = Math.max(1, Math.round(font.width(new String(Character.toChars(codepoint))) * scaleX));
            EmojiRegistry.EmojiEntry candidate = EmojiRegistry.INSTANCE.entryForCodepoint(codepoint);
            int right = cursorX + charWidth - 1;
            if (candidate != null && mouseX >= cursorX && mouseX < right) {
                return new Hit(candidate, cursorX, top, right, top + lineHeight);
            }
            cursorX += charWidth;
            offset += Character.charCount(codepoint);
        }
        return null;
    }

    public static @Nullable Hit hitChat(double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        ChatComponent chat = minecraft.gui.getChat();
        if (!chat.isChatFocused()) return null;
        ChatComponentAccessor acc = (ChatComponentAccessor) chat;
        var lines = acc.twemoji$trimmedMessages();
        if (lines.isEmpty()) return null;

        float scale = (float) chat.getScale();
        int chatScrollbarPos = acc.twemoji$chatScrollbarPos();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        int chatWidth = Mth.ceil(chat.getWidth() / scale);

        double chatX = mouseX / scale - 4.0;
        if (chatX < 0 || chatX > chatWidth) return null;

        double spacing = minecraft.options.chatLineSpacing().get();
        int lineHeight = (int) (9.0 * (spacing + 1.0));
        int textBaselineOffset = (int) Math.round(-8.0 * (spacing + 1.0) + 4.0 * spacing);
        int chatBottomLocal = Mth.floor((guiHeight - 40) / scale);

        double localY = mouseY / scale;
        double offsetFromBottom = chatBottomLocal - localY;
        int displayIndex = Mth.floor(offsetFromBottom / lineHeight);
        if (displayIndex < 0 || displayIndex >= chat.getLinesPerPage()) return null;
        int lineIndex = displayIndex + chatScrollbarPos;
        if (lineIndex < 0 || lineIndex >= lines.size()) return null;

        int textTopLocal = chatBottomLocal - displayIndex * lineHeight + textBaselineOffset;

        GuiMessage.Line line = lines.get(lineIndex);
        FormattedCharSequence content = line.content();
        int chatHeadsOffset = ChatHeadsCompat.chatOffset(line);
        double localChatX = chatX - chatHeadsOffset;

        Font font = minecraft.font;
        int[] cursorPx = {0};
        EmojiRegistry.EmojiEntry[] found = {null};
        int[] foundLeftPx = {0};
        int[] foundWidthPx = {0};
        content.accept((position, style, codepoint) -> {
            int width = Math.max(1, font.width(FormattedCharSequence.forward(new String(Character.toChars(codepoint)), style)));
            EmojiRegistry.EmojiEntry candidate = EmojiRegistry.INSTANCE.entryForCodepoint(codepoint);
            if (candidate != null && localChatX >= cursorPx[0] && localChatX < cursorPx[0] + width) {
                found[0] = candidate;
                foundLeftPx[0] = cursorPx[0];
                foundWidthPx[0] = width;
                return false;
            }
            cursorPx[0] += width;
            return true;
        });
        if (found[0] == null) return null;
        int hitLeftScreen = Math.round((4 + chatHeadsOffset + foundLeftPx[0]) * scale);
        int hitRightScreen = Math.round((4 + chatHeadsOffset + foundLeftPx[0] + foundWidthPx[0]) * scale);
        int hitTopScreen = Math.round((textTopLocal - 1) * scale);
        int hitBottomScreen = Math.round((textTopLocal - 1 + 9) * scale);
        return new Hit(found[0], hitLeftScreen, hitTopScreen, hitRightScreen, hitBottomScreen);
    }

    public static void requestHoverCursor(GuiGraphics graphics, @Nullable Hit hit) {
        // No-op in 1.21.1 - GuiGraphics has no cursor API.
    }

    public static void renderHoverHighlight(GuiGraphics graphics, @Nullable Hit hit) {
        if (hit == null) return;
        graphics.fill(hit.left(), hit.top(), hit.right(), hit.bottom(), HOVER_COLOR);
    }

    private static void renderSprite(GuiGraphics graphics, EmojiSprite sprite, int x, int y, int size) {
        if (sprite == null) return;
        sprite.blit(graphics, x, y, size);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static String trimToWidth(Font font, String text, int width) {
        if (font.width(text) <= width) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + suffix) > width) {
            end--;
        }
        return end == 0 ? suffix : text.substring(0, end) + suffix;
    }

    public record Hit(EmojiRegistry.EmojiEntry entry, int left, int top, int right, int bottom) {
        public int centerX() {
            return (this.left + this.right) / 2;
        }
    }
}
