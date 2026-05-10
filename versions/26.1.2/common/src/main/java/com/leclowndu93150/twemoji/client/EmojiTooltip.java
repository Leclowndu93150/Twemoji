package com.leclowndu93150.twemoji.client;

import com.leclowndu93150.twemoji.client.glyph.EmojiSprite;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

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

    private static @Nullable Screen owner;
    private static EmojiRegistry.@Nullable EmojiEntry entry;
    private static int anchorX;
    private static int anchorY;

    private EmojiTooltip() {
    }

    public static void render(Screen screen, GuiGraphicsExtractor graphics, Font font, int screenWidth, int screenHeight) {
        if (owner != screen || entry == null) return;

        String shortcode = entry.shortcode();
        int textWidth = Math.min(font.width(shortcode), MAX_TEXT_WIDTH);
        int width = Math.max(MIN_WIDTH, PADDING * 2 + EMOJI_SIZE + GAP + textWidth);
        int x = Mth.clamp(anchorX + 8, 4, Math.max(4, screenWidth - width - 4));
        int y = anchorY + 10;
        if (y + HEIGHT > screenHeight - 4) {
            y = anchorY - HEIGHT - 8;
        }
        y = Mth.clamp(y, 4, Math.max(4, screenHeight - HEIGHT - 4));

        graphics.fill(x, y, x + width, y + HEIGHT, PANEL_COLOR);
        graphics.fill(x + 1, y + 1, x + width - 1, y + HEIGHT - 1, FOOTER_COLOR);
        drawBorder(graphics, x, y, width, HEIGHT, BORDER_COLOR);
        renderSprite(graphics, EmojiRegistry.INSTANCE.spriteForTone(entry, EmojiConfig.get().getSkinTone()), x + PADDING, y + (HEIGHT - EMOJI_SIZE) / 2, EMOJI_SIZE);
        graphics.text(font, trimToWidth(font, shortcode, width - PADDING * 2 - EMOJI_SIZE - GAP), x + PADDING + EMOJI_SIZE + GAP, y + 11, TEXT_COLOR, false);

        String name = EmojiRegistry.innerName(entry);
        graphics.text(font, trimToWidth(font, name, width - PADDING * 2 - EMOJI_SIZE - GAP), x + PADDING + EMOJI_SIZE + GAP, y + 22, MUTED_COLOR, false);
    }

    public static boolean click(Screen screen, Hit hit) {
        owner = screen;
        entry = hit.entry();
        anchorX = hit.centerX();
        anchorY = hit.bottom();
        return true;
    }

    public static boolean closeIfOpen(Screen screen) {
        if (owner != screen || entry == null) return false;
        entry = null;
        owner = null;
        return true;
    }

    public static @Nullable Hit hitString(Font font, String text, int x, int y, float scaleX, float scaleY, int mouseX, int mouseY) {
        if (text.isEmpty()) return null;
        int lineHeight = Math.max(1, Math.round(9.0F * scaleY));
        if (mouseY < y || mouseY >= y + lineHeight) return null;

        int cursorX = x;
        for (int offset = 0; offset < text.length(); ) {
            int codepoint = text.codePointAt(offset);
            int charWidth = Math.max(1, Math.round(font.width(new String(Character.toChars(codepoint))) * scaleX));
            EmojiRegistry.EmojiEntry candidate = EmojiRegistry.INSTANCE.entryForCodepoint(codepoint);
            if (candidate != null && mouseX >= cursorX && mouseX < cursorX + charWidth) {
                return new Hit(candidate, cursorX, y, cursorX + charWidth, y + lineHeight);
            }
            cursorX += charWidth;
            offset += Character.charCount(codepoint);
        }
        return null;
    }

    public static @Nullable Hit hitFormatted(Font font, FormattedCharSequence text, int x, int y, int mouseX, int mouseY) {
        ChatCollector collector = new ChatCollector(font, mouseX, mouseY);
        collector.accept(TextAlignment.LEFT, x, y, text);
        return collector.result();
    }

    public static void requestHoverCursor(GuiGraphicsExtractor graphics, @Nullable Hit hit) {
        if (hit == null) return;
        graphics.requestCursor(CursorTypes.POINTING_HAND);
    }

    public static void renderHoverHighlight(GuiGraphicsExtractor graphics, @Nullable Hit hit) {
        if (hit == null) return;
        int size = Math.max(hit.right() - hit.left(), hit.bottom() - hit.top()) + 4;
        int centerX = (hit.left() + hit.right()) / 2;
        int centerY = (hit.top() + hit.bottom()) / 2;
        int left = centerX - size / 2;
        int top = centerY - size / 2;
        graphics.fill(left, top, left + size, top + size, HOVER_COLOR);
    }

    private static void renderSprite(GuiGraphicsExtractor graphics, EmojiSprite sprite, int x, int y, int size) {
        if (sprite == null) return;
        switch (sprite) {
            case EmojiSprite.Sheet sheet -> graphics.blit(sheet.texture(), x, y, x + size, y + size, sheet.u0(), sheet.u1(), sheet.v0(), sheet.v1());
            case EmojiSprite.Custom custom -> graphics.blit(custom.texture(), x, y, x + size, y + size, 0.0F, 1.0F, 0.0F, 1.0F);
            case EmojiSprite.Animated animated -> {
                AnimatedEmojiRegistry.AnimatedEmoji emoji = AnimatedEmojiRegistry.INSTANCE.byCodepoint(animated.codepoint());
                if (emoji != null) graphics.blit(emoji.currentFrame(), x, y, x + size, y + size, 0.0F, 1.0F, 0.0F, 1.0F);
            }
        }
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
        private ActiveTextCollector.Parameters defaultParameters = INITIAL;
        private @Nullable Hit result;

        public ChatCollector(Font font, int mouseX, int mouseY) {
            this.font = font;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
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
            if (localMouse.y() < y || localMouse.y() >= y + 9) return;
            int leftX = alignment.calculateLeft(anchorX, this.font, text);

            int[] cursorX = {leftX};
            text.accept((position, style, codepoint) -> {
                if (this.result != null) return false;
                EmojiRegistry.EmojiEntry candidate = EmojiRegistry.INSTANCE.entryForCodepoint(codepoint);
                int width;
                if (candidate != null) {
                    width = Math.max(1, this.font.width(FormattedCharSequence.forward(new String(Character.toChars(codepoint)), style)));
                    if (localMouse.x() >= cursorX[0] && localMouse.x() < cursorX[0] + width) {
                        ScreenRectangle bounds = new ScreenRectangle(cursorX[0], y, width, 9).transformMaxBounds(parameters.pose());
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
