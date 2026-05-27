package com.leclowndu93150.twemoji.client.suggestion;

import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import com.leclowndu93150.twemoji.client.render.EmojiSprite;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Consumer;

public final class EmojiSuggestions {

    private static final int ROW_HEIGHT = 12;
    private static final int LINE_LIMIT = 8;
    private static final int EMOJI_SIZE = 10;
    private static final int EMOJI_PAD = 2;
    private static final int EMOJI_COL = EMOJI_SIZE + EMOJI_PAD;
    private static final int MAX_LABEL_WIDTH = 120;
    private static final int FILL_COLOR = 0xE0101010;

    private static final int KEY_UP = 265;
    private static final int KEY_DOWN = 264;
    private static final int KEY_TAB = 258;
    private static final int KEY_ENTER = 257;
    private static final int KEY_NUMPAD_ENTER = 335;
    private static final int KEY_ESCAPE = 256;

    private final Font font;
    private List<EmojiRegistry.EmojiEntry> suggestions = List.of();
    private int rangeStart;
    private int rangeEnd;
    private int x;
    private int y;
    private int width;
    private int offset;
    private int current;

    public EmojiSuggestions(Font font) {
        this.font = font;
    }

    public void update(String value, int cursor, int anchorX, int anchorY, int screenWidth) {
        Match match = findMatch(value, cursor);
        if (match == null) {
            this.hide();
            return;
        }

        int tone = EmojiConfig.get().getSkinTone();
        List<EmojiRegistry.EmojiEntry> matches = EmojiRegistry.INSTANCE.getSuggestions(match.prefix(), tone);
        if (matches.isEmpty()) {
            this.hide();
            return;
        }

        this.suggestions = matches;
        this.rangeStart = match.start();
        this.rangeEnd = cursor;
        this.current = Mth.clamp(this.current, 0, matches.size() - 1);
        this.offset = Mth.clamp(this.offset, 0, Math.max(matches.size() - LINE_LIMIT, 0));
        this.width = EMOJI_COL + MAX_LABEL_WIDTH + 1;
        this.x = Mth.clamp(anchorX, 0, Math.max(0, screenWidth - this.width));
        this.y = anchorY;
    }

    public void hide() {
        this.suggestions = List.of();
        this.current = 0;
        this.offset = 0;
    }

    public boolean isVisible() {
        return !this.suggestions.isEmpty();
    }

    public boolean keyPressed(int keyCode, Consumer<Replacement> consumer) {
        if (!this.isVisible()) return false;
        if (keyCode == KEY_UP) {
            this.cycle(-1);
            return true;
        }
        if (keyCode == KEY_DOWN) {
            this.cycle(1);
            return true;
        }
        if (keyCode == KEY_TAB || keyCode == KEY_ENTER || keyCode == KEY_NUMPAD_ENTER) {
            this.useSuggestion(consumer);
            return true;
        }
        if (keyCode == KEY_ESCAPE) {
            this.hide();
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, Consumer<Replacement> consumer) {
        if (!this.isVisible() || !this.contains((int) mouseX, (int) mouseY)) return false;
        int line = ((int) mouseY - this.y) / ROW_HEIGHT + this.offset;
        if (line >= 0 && line < this.suggestions.size()) {
            this.current = line;
            this.useSuggestion(consumer);
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!this.isVisible() || !this.contains((int) mouseX, (int) mouseY)) return false;
        this.offset = Mth.clamp((int) (this.offset - scrollY), 0, Math.max(this.suggestions.size() - LINE_LIMIT, 0));
        return true;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.isVisible()) return;
        int limit = Math.min(this.suggestions.size(), LINE_LIMIT);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        try {
            for (int i = 0; i < limit; i++) {
                int index = i + this.offset;
                int rowY = this.y + ROW_HEIGHT * i;
                graphics.fill(this.x, rowY, this.x + this.width, rowY + ROW_HEIGHT, FILL_COLOR);
                EmojiRegistry.EmojiEntry entry = this.suggestions.get(index);
                EmojiSprite sprite = EmojiRegistry.INSTANCE.spriteForTone(entry, EmojiConfig.get().getSkinTone());
                if (sprite != null) sprite.blit(graphics, this.x + 1, rowY + 1, EMOJI_SIZE);
                String text = this.trim(entry.shortcode());
                graphics.drawString(this.font, text, this.x + EMOJI_COL, rowY + 2, index == this.current ? -256 : -5592406);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private void useSuggestion(Consumer<Replacement> consumer) {
        EmojiRegistry.EmojiEntry entry = this.suggestions.get(this.current);
        consumer.accept(new Replacement(this.rangeStart, this.rangeEnd, EmojiRegistry.INSTANCE.characterForTone(entry, EmojiConfig.get().getSkinTone())));
        EmojiConfig.get().recordEmojiUse(EmojiRegistry.innerName(entry));
        this.hide();
    }

    private void cycle(int direction) {
        this.current += direction;
        if (this.current < 0) this.current = this.suggestions.size() - 1;
        if (this.current >= this.suggestions.size()) this.current = 0;
        if (this.current < this.offset) {
            this.offset = this.current;
        } else if (this.current >= this.offset + LINE_LIMIT) {
            this.offset = this.current - LINE_LIMIT + 1;
        }
    }

    private boolean contains(int mouseX, int mouseY) {
        int height = Math.min(this.suggestions.size(), LINE_LIMIT) * ROW_HEIGHT;
        return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + height;
    }

    private String trim(String text) {
        String trimmed = this.font.plainSubstrByWidth(text, MAX_LABEL_WIDTH);
        if (trimmed.length() < text.length()) {
            trimmed = this.font.plainSubstrByWidth(text, MAX_LABEL_WIDTH - this.font.width("...")) + "...";
        }
        return trimmed;
    }

    private static Match findMatch(String value, int cursor) {
        if (cursor < 0 || cursor > value.length()) return null;
        String beforeCursor = value.substring(0, cursor);
        int colonStart = -1;
        for (int i = beforeCursor.length() - 1; i >= 0; i--) {
            char c = beforeCursor.charAt(i);
            if (c == ':') {
                colonStart = i;
                break;
            }
            if (Character.isWhitespace(c)) break;
        }
        if (colonStart == -1) return null;
        if (colonStart > 0 && isIdentifierChar(beforeCursor.charAt(colonStart - 1))) return null;
        String prefix = beforeCursor.substring(colonStart + 1);
        if (prefix.contains(":") || prefix.chars().anyMatch(Character::isWhitespace)) return null;
        return new Match(colonStart, prefix);
    }

    private static boolean isIdentifierChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.';
    }

    private record Match(int start, String prefix) {}

    public record Replacement(int start, int end, String text) {}
}
