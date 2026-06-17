package com.leclowndu93150.twemoji.client.tooltip;

import com.leclowndu93150.twemoji.client.compat.ChatHeadsCompat;
import com.leclowndu93150.twemoji.client.render.EmojiSprite;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
    private static EmojiRegistry.@Nullable EmojiEntry copiedEntry;
    private static final int COPY_FLASH_MS = 1200;

    private EmojiTooltip() {
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight, @Nullable Hit hit) {
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

        graphics.nextStratum();
        graphics.fill(x, y, x + width, y + HEIGHT, PANEL_COLOR);
        graphics.fill(x + 1, y + 1, x + width - 1, y + HEIGHT - 1, FOOTER_COLOR);
        drawBorder(graphics, x, y, width, HEIGHT, BORDER_COLOR);
        renderSprite(graphics, EmojiRegistry.INSTANCE.spriteForTone(entry, EmojiConfig.get().getSkinTone()), x + PADDING, y + (HEIGHT - EMOJI_SIZE) / 2, EMOJI_SIZE);
        int labelColor = showCopied ? 0xFF8AE08A : TEXT_COLOR;
        graphics.text(font, trimToWidth(font, label, width - PADDING * 2 - EMOJI_SIZE - GAP), x + PADDING + EMOJI_SIZE + GAP, y + 11, labelColor, false);

        String name = showCopied ? Component.translatable("twemoji.picker.copy_to_clipboard").getString() : EmojiRegistry.innerName(entry);
        graphics.text(font, trimToWidth(font, name, width - PADDING * 2 - EMOJI_SIZE - GAP), x + PADDING + EMOJI_SIZE + GAP, y + 22, MUTED_COLOR, false);
    }

    public static boolean copyShortcode(Hit hit) {
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

    public static @Nullable Hit hitFormatted(Font font, FormattedCharSequence text, int x, int y, int mouseX, int mouseY) {
        ChatCollector collector = new ChatCollector(font, mouseX, mouseY, null);
        collector.accept(TextAlignment.LEFT, x, y, text);
        return collector.result();
    }

    public static Map<FormattedCharSequence, GuiMessage.Line> buildLineLookup(List<GuiMessage.Line> lines) {
        Map<FormattedCharSequence, GuiMessage.Line> lookup = new IdentityHashMap<>(lines.size());
        for (GuiMessage.Line line : lines) {
            lookup.put(line.content(), line);
        }
        return lookup;
    }

    public static void requestHoverCursor(GuiGraphicsExtractor graphics, @Nullable Hit hit) {
        if (hit == null) return;
        graphics.requestCursor(CursorTypes.POINTING_HAND);
    }

    public static void renderHoverHighlight(GuiGraphicsExtractor graphics, @Nullable Hit hit) {
        if (hit == null) return;
        graphics.fill(hit.left(), hit.top(), hit.right(), hit.bottom(), HOVER_COLOR);
    }

    private static void renderSprite(GuiGraphicsExtractor graphics, EmojiSprite sprite, int x, int y, int size) {
        if (sprite == null) return;
        sprite.blit(graphics, x, y, size);
    }

    private static void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
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

    public static final class ChatCollector implements ActiveTextCollector {
        private static final ActiveTextCollector.Parameters INITIAL = new ActiveTextCollector.Parameters(new Matrix3x2f());

        private final Font font;
        private final int mouseX;
        private final int mouseY;
        private final @Nullable Map<FormattedCharSequence, GuiMessage.Line> lineLookup;
        private ActiveTextCollector.Parameters defaultParameters = INITIAL;
        private @Nullable Hit result;

        public ChatCollector(Font font, int mouseX, int mouseY, @Nullable Map<FormattedCharSequence, GuiMessage.Line> lineLookup) {
            this.font = font;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.lineLookup = lineLookup;
        }

        @Override
        public ActiveTextCollector.Parameters defaultParameters() {
            return this.defaultParameters;
        }

        @Override
        public void defaultParameters(ActiveTextCollector.Parameters newParameters) {
            this.defaultParameters = newParameters;
        }

        @Override
        public void accept(TextAlignment alignment, int anchorX, int y, ActiveTextCollector.Parameters parameters, FormattedCharSequence text) {
            if (this.result != null) return;
            if (parameters.scissor() != null && !parameters.scissor().containsPoint(this.mouseX, this.mouseY)) return;
            Matrix3x2f inverse = parameters.pose().invert(new Matrix3x2f());
            Vector2f localMouse = inverse.transformPosition(this.mouseX, this.mouseY, new Vector2f());
            int top = y - 1;
            if (localMouse.y() < top || localMouse.y() >= top + 9) return;
            int chatHeadsOffset = 0;
            if (this.lineLookup != null) {
                GuiMessage.Line line = this.lineLookup.get(text);
                if (line != null) {
                    chatHeadsOffset = ChatHeadsCompat.chatOffset(line);
                }
            }
            int leftX = alignment.calculateLeft(anchorX, this.font, text) + chatHeadsOffset;

            int[] cursorX = {leftX};
            text.accept((position, style, codepoint) -> {
                if (this.result != null) return false;
                EmojiRegistry.EmojiEntry candidate = EmojiRegistry.INSTANCE.entryForCodepoint(codepoint);
                int width;
                if (candidate != null) {
                    width = Math.max(1, this.font.width(FormattedCharSequence.forward(new String(Character.toChars(codepoint)), style)));
                    int rightLimit = cursorX[0] + width - 1;
                    if (localMouse.x() >= cursorX[0] && localMouse.x() < rightLimit) {
                        ScreenRectangle bounds = new ScreenRectangle(cursorX[0], top, width - 1, 9).transformMaxBounds(parameters.pose());
                        this.result = new Hit(candidate, bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
                        return false;
                    }
                } else {
                    width = Math.max(1, this.font.width(FormattedCharSequence.forward(new String(Character.toChars(codepoint)), style)));
                }
                cursorX[0] += width;
                return true;
            });
        }

        @Override
        public void acceptScrolling(Component message, int centerX, int left, int right, int top, int bottom, ActiveTextCollector.Parameters parameters) {
            int lineWidth = this.font.width(message);
            this.defaultScrollingHelper(message, centerX, left, right, top, bottom, lineWidth, 9, parameters);
        }

        public @Nullable Hit result() {
            return this.result;
        }
    }
}
