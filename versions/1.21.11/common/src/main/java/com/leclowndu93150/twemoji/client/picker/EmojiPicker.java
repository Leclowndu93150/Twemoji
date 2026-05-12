package com.leclowndu93150.twemoji.client.picker;

import com.leclowndu93150.twemoji.client.render.EmojiSprite;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.leclowndu93150.twemoji.client.config.EmojiConfig;
import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;

public final class EmojiPicker {

    private static final int PANEL_WIDTH = 184;
    private static final int PANEL_HEIGHT = 142;
    private static final int NAV_HEIGHT = 24;
    private static final int CATEGORY_WIDTH = 22;
    private static final int FOOTER_HEIGHT = 24;
    private static final int CATEGORY_HEADER_HEIGHT = 16;
    private static final int CELL = 18;
    private static final int EMOJI_SIZE = 16;
    private static final int CATEGORY_ICON_SIZE = 12;
    private static final int CATEGORY_BUTTON_SIZE = 14;
    private static final int CATEGORY_STEP = 15;
    private static final int BUTTON_SIZE = 9;
    private static final int TONE_BUTTON_SIZE = 18;
    private static final int TONE_OPTION_SIZE = 20;
    private static final int PANEL_COLOR = 0xF0131416;
    private static final int RAIL_COLOR = 0xF0070709;
    private static final int FOOTER_COLOR = 0xF0070709;
    private static final int SEARCH_COLOR = 0x80202022;
    private static final int BORDER_COLOR = 0xFF323337;
    private static final int HOVER_COLOR = 0x26FFFFFF;
    private static final int SELECTED_COLOR = 0x14FFFFFF;
    private static final int SCROLL_TRACK_COLOR = 0x60121416;
    private static final int SCROLL_THUMB_COLOR = 0xCC323337;
    private static final int TEXT_COLOR = 0xFFE9EAEC;
    private static final int MUTED_COLOR = 0xFF9B9DA3;
    private static final int KEY_BACKSPACE = 259;
    private static final int KEY_ENTER = 257;
    private static final int KEY_NUMPAD_ENTER = 335;
    private static final int KEY_LEFT = 263;
    private static final int KEY_RIGHT = 262;
    private static final int KEY_UP = 265;
    private static final int KEY_DOWN = 264;
    private static final int KEY_TAB = 258;
    private static final int KEY_PAGE_UP = 266;
    private static final int KEY_PAGE_DOWN = 267;

    private static EmojiPicker activeSearchPicker;

    private sealed interface PickerCategory permits BuiltinCategory, CustomCategory {}
    private record BuiltinCategory(Category category) implements PickerCategory {}
    private record CustomCategory(String name) implements PickerCategory {}

    private final Font font;
    private boolean open;
    private boolean searchFocused;
    private boolean toneOpen;
    private boolean categoryOpen = true;
    private PickerCategory selected = new BuiltinCategory(Category.PEOPLE);
    private int scroll;
    private int categoryScroll;
    private int cursorIndex = -1;
    private String search = "";
    private EmojiRegistry.EmojiEntry hoveredEntry;
    private EditBox searchOwner;

    public EmojiPicker(Font font) {
        this.font = font;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean blocksMouse(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (EmojiConfig.get().isPickerButtonVisible()
            && contains(mouseX, mouseY, buttonX(screenWidth) - 1, buttonY(screenHeight), BUTTON_SIZE + 2, BUTTON_SIZE)) {
            return true;
        }
        if (!this.open) return false;
        return contains(mouseX, mouseY, panelX(screenWidth), panelY(screenHeight), PANEL_WIDTH, PANEL_HEIGHT);
    }

    public void toggle() {
        this.open = !this.open;
        this.toneOpen = false;
        this.searchFocused = this.open;
        this.scroll = 0;
        this.cursorIndex = -1;
    }

    public void close() {
        this.open = false;
        this.searchFocused = false;
        this.toneOpen = false;
        this.searchOwner = null;
        this.cursorIndex = -1;
        if (activeSearchPicker == this) activeSearchPicker = null;
    }

    public void attachInput(EditBox input) {
        this.searchOwner = input;
        activeSearchPicker = this;
        this.searchFocused = true;
    }

    private void renderButtonTooltip(GuiGraphics graphics, int buttonX, int buttonY, int screenWidth) {
        String binding = TwemojiKeyMappings.OPEN_PICKER.getTranslatedKeyMessage().getString();
        String text = binding.isEmpty()
            ? Component.translatable("twemoji.picker.button.tooltip").getString()
            : Component.translatable("twemoji.picker.button.tooltip.keybind", "Ctrl+" + binding).getString();
        int textW = this.font.width(text);
        int padding = 4;
        int boxW = textW + padding * 2;
        int boxH = 12;
        int x = Math.min(buttonX + BUTTON_SIZE - boxW, screenWidth - boxW - 2);
        if (x < 2) x = 2;
        int y = buttonY - boxH - 3;
        if (y < 2) y = buttonY + BUTTON_SIZE + 3;
        graphics.fill(x, y, x + boxW, y + boxH, PANEL_COLOR);
        drawBorder(graphics, x, y, boxW, boxH, BORDER_COLOR);
        graphics.drawString(this.font, text, x + padding, y + 2, TEXT_COLOR, false);
    }

    public static boolean handleActiveSearchChar(EditBox input, CharacterEvent event) {
        return activeSearchPicker != null && activeSearchPicker.searchOwner == input && activeSearchPicker.charTyped(event);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        boolean buttonVisible = EmojiConfig.get().isPickerButtonVisible();
        int buttonX = buttonX(screenWidth);
        int buttonY = buttonY(screenHeight);
        boolean buttonHovered = buttonVisible && contains(mouseX, mouseY, buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE);
        if (buttonVisible) {
            if (buttonHovered || this.open) {
                graphics.fill(buttonX - 1, buttonY, buttonX + BUTTON_SIZE + 1, buttonY + BUTTON_SIZE, HOVER_COLOR);
            }
            EmojiRegistry.EmojiEntry smiley = EmojiRegistry.INSTANCE.get("smiley");
            if (smiley != null) {
                this.renderSprite(graphics, EmojiRegistry.INSTANCE.spriteForTone(smiley, 0), buttonX, buttonY, BUTTON_SIZE);
            } else {
                renderFace(graphics, buttonX, buttonY, BUTTON_SIZE, 0xFFFFD84D, 0xFF2B2D31);
            }
            if (buttonHovered) graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        if (!this.open) {
            if (buttonHovered) this.renderButtonTooltip(graphics, buttonX, buttonY, screenWidth);
            return;
        }

        int x = panelX(screenWidth);
        int y = panelY(screenHeight);
        this.hoveredEntry = null;
        this.normalizeCategory();

        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, PANEL_COLOR);
        drawBorder(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, 0x40363639);

        int bodyY = y + NAV_HEIGHT;
        graphics.fill(x, bodyY, x + CATEGORY_WIDTH, y + PANEL_HEIGHT, RAIL_COLOR);
        this.renderCategories(graphics, mouseX, mouseY, x, bodyY);

        int contentX = x + CATEGORY_WIDTH;
        graphics.fill(contentX, bodyY, x + PANEL_WIDTH, y + PANEL_HEIGHT - FOOTER_HEIGHT, PANEL_COLOR);
        this.renderGrid(graphics, mouseX, mouseY, x, y);
        this.renderFooter(graphics, x, y);
        this.renderNav(graphics, mouseX, mouseY, x, y);

        if (this.toneOpen) {
            this.renderToneMenu(graphics, mouseX, mouseY, x, y);
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, int screenWidth, int screenHeight, EditBox input) {
        int mouseX = (int)event.x();
        int mouseY = (int)event.y();
        if (EmojiConfig.get().isPickerButtonVisible()
            && contains(mouseX, mouseY, buttonX(screenWidth) - 1, buttonY(screenHeight), BUTTON_SIZE + 2, BUTTON_SIZE)) {
            this.toggle();
            this.searchOwner = this.open ? input : null;
            activeSearchPicker = this.open ? this : null;
            return true;
        }
        if (!this.open) return false;

        int x = panelX(screenWidth);
        int y = panelY(screenHeight);
        if (!contains(mouseX, mouseY, x, y, PANEL_WIDTH, PANEL_HEIGHT)) {
            this.close();
            return false;
        }

        if (contains(mouseX, mouseY, searchX(x), searchY(y), searchWidth(x), 16)) {
            this.searchFocused = true;
            this.searchOwner = input;
            activeSearchPicker = this;
            this.toneOpen = false;
            return true;
        }

        if (contains(mouseX, mouseY, toneButtonX(x), toneButtonY(y), TONE_BUTTON_SIZE, TONE_BUTTON_SIZE)) {
            this.toneOpen = !this.toneOpen;
            this.searchFocused = false;
            this.searchOwner = null;
            return true;
        }

        Integer tone = this.toneAt(mouseX, mouseY, x, y);
        if (tone != null) {
            EmojiConfig.get().setSkinTone(tone);
            this.toneOpen = false;
            return true;
        }
        if (this.isToneInteraction(mouseX, mouseY, x, y)) return true;

        if (this.headerAt(mouseX, mouseY, x, y)) {
            this.categoryOpen = !this.categoryOpen;
            this.scroll = 0;
            this.searchFocused = false;
            this.searchOwner = null;
            return true;
        }

        PickerCategory clickedCategory = this.categoryAt(mouseX, mouseY, x, y + NAV_HEIGHT);
        if (clickedCategory != null) {
            this.selected = clickedCategory;
            this.categoryOpen = true;
            this.search = "";
            this.scroll = 0;
            this.cursorIndex = -1;
            this.searchFocused = false;
            this.searchOwner = null;
            this.toneOpen = false;
            return true;
        }

        EmojiRegistry.EmojiEntry entry = this.entryAt(mouseX, mouseY, x, y);
        if (entry != null) {
            input.insertText(EmojiRegistry.INSTANCE.characterForTone(entry, EmojiConfig.get().getSkinTone()));
            EmojiConfig.get().recordEmojiUse(EmojiRegistry.innerName(entry));
            input.setFocused(true);
            this.searchFocused = false;
            this.searchOwner = null;
            return true;
        }
        return true;
    }

    public boolean mouseScrolled(double x, double y, double scrollY, int screenWidth, int screenHeight) {
        if (!this.open) return false;
        int panelX = panelX(screenWidth);
        int panelY = panelY(screenHeight);
        if (contains((int)x, (int)y, panelX, panelY + NAV_HEIGHT, CATEGORY_WIDTH, PANEL_HEIGHT - NAV_HEIGHT)) {
            int maxScroll = Math.max(0, this.pickerCategories().size() - visibleCategoryCount());
            this.categoryScroll = Mth.clamp((int)(this.categoryScroll - scrollY), 0, maxScroll);
            return true;
        }
        if (!contains((int)x, (int)y, panelX + CATEGORY_WIDTH, panelY + NAV_HEIGHT, PANEL_WIDTH - CATEGORY_WIDTH, PANEL_HEIGHT - NAV_HEIGHT - FOOTER_HEIGHT)) {
            return false;
        }
        int columns = columns();
        int visibleRows = visibleRows();
        int maxScroll = Math.max(0, (this.entries().size() + columns - 1) / columns - visibleRows);
        int nextScroll = (int)(this.scroll - scrollY);
        if (this.search.isEmpty() && this.categoryOpen && nextScroll > maxScroll && this.selectAdjacentCategory(1)) {
            this.scroll = 0;
            return true;
        }
        if (this.search.isEmpty() && this.categoryOpen && nextScroll < 0 && this.selectAdjacentCategory(-1)) {
            this.scroll = this.maxScrollForCurrentCategory();
            return true;
        }
        this.scroll = Mth.clamp(nextScroll, 0, maxScroll);
        return true;
    }

    public boolean keyPressed(KeyEvent event, EditBox input) {
        if (!this.open) return false;
        if (event.isEscape()) {
            this.close();
            return true;
        }
        int key = event.key();
        List<EmojiRegistry.EmojiEntry> entries = this.entries();

        if (key == KEY_LEFT || key == KEY_RIGHT || key == KEY_UP || key == KEY_DOWN
            || key == KEY_PAGE_UP || key == KEY_PAGE_DOWN) {
            if (entries.isEmpty()) return true;
            int cols = columns();
            if (this.cursorIndex < 0) this.cursorIndex = this.scroll * cols;
            int next = switch (key) {
                case KEY_LEFT -> this.cursorIndex - 1;
                case KEY_RIGHT -> this.cursorIndex + 1;
                case KEY_UP -> this.cursorIndex - cols;
                case KEY_DOWN -> this.cursorIndex + cols;
                case KEY_PAGE_UP -> this.cursorIndex - cols * visibleRows();
                case KEY_PAGE_DOWN -> this.cursorIndex + cols * visibleRows();
                default -> this.cursorIndex;
            };
            this.cursorIndex = Mth.clamp(next, 0, entries.size() - 1);
            this.ensureCursorVisible(entries.size());
            return true;
        }

        if (key == KEY_TAB) {
            this.selectAdjacentCategory(event.hasShiftDown() ? -1 : 1);
            this.cursorIndex = -1;
            return true;
        }

        if (key == KEY_ENTER || key == KEY_NUMPAD_ENTER) {
            if (entries.isEmpty()) return true;
            int idx = this.cursorIndex >= 0 ? this.cursorIndex : 0;
            EmojiRegistry.EmojiEntry entry = entries.get(Math.min(idx, entries.size() - 1));
            input.insertText(EmojiRegistry.INSTANCE.characterForTone(entry, EmojiConfig.get().getSkinTone()));
            EmojiConfig.get().recordEmojiUse(EmojiRegistry.innerName(entry));
            input.setFocused(true);
            this.searchFocused = false;
            return true;
        }

        if (!this.searchFocused) return false;
        if (key == KEY_BACKSPACE) {
            if (!this.search.isEmpty()) {
                this.search = this.search.substring(0, this.search.length() - 1);
                this.scroll = 0;
                this.cursorIndex = -1;
            }
            return true;
        }
        return false;
    }

    private void ensureCursorVisible(int total) {
        if (this.cursorIndex < 0) return;
        int cols = columns();
        int row = this.cursorIndex / cols;
        if (row < this.scroll) {
            this.scroll = row;
        } else if (row >= this.scroll + visibleRows()) {
            this.scroll = row - visibleRows() + 1;
        }
        int rows = (total + cols - 1) / cols;
        int maxScroll = Math.max(0, rows - visibleRows());
        this.scroll = Mth.clamp(this.scroll, 0, maxScroll);
    }

    public boolean charTyped(CharacterEvent event) {
        if (!this.open || !this.searchFocused || !event.isAllowedChatCharacter()) return false;
        this.search += event.codepointAsString();
        this.scroll = 0;
        this.cursorIndex = -1;
        return true;
    }

    private void renderNav(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
        int searchX = searchX(x);
        int searchY = searchY(y);
        boolean searchHovered = contains(mouseX, mouseY, searchX, searchY, searchWidth(x), 16);
        graphics.fill(searchX, searchY, searchX + searchWidth(x), searchY + 16, SEARCH_COLOR);
        drawBorder(graphics, searchX, searchY, searchWidth(x), 16, this.searchFocused ? 0xFF3687E9 : BORDER_COLOR);
        String text = this.search.isEmpty() ? this.searchPlaceholder() : this.search;
        int color = this.search.isEmpty() ? MUTED_COLOR : TEXT_COLOR;
        String renderedText = trimToWidth(text, searchWidth(x) - 28);
        graphics.drawString(this.font, renderedText, searchX + 5, searchY + 4, color, false);
        if (this.searchFocused && (Util.getMillis() / 300L) % 2L == 0L) {
            int cursorX = this.search.isEmpty() ? searchX + 5 : searchX + 5 + this.font.width(renderedText) + 1;
            graphics.fill(cursorX, searchY + 4, cursorX + 1, searchY + 13, TEXT_COLOR);
        }
        graphics.drawString(this.font, "o", searchX + searchWidth(x) - 11, searchY + 4, MUTED_COLOR, false);
        graphics.fill(searchX + searchWidth(x) - 5, searchY + 12, searchX + searchWidth(x) - 2, searchY + 13, MUTED_COLOR);
        if (searchHovered) graphics.requestCursor(CursorTypes.IBEAM);

        int toneX = toneButtonX(x);
        int toneY = toneButtonY(y);
        boolean toneHovered = contains(mouseX, mouseY, toneX, toneY, TONE_BUTTON_SIZE, TONE_BUTTON_SIZE);
        if (toneHovered || this.toneOpen) graphics.fill(toneX - 1, toneY - 1, toneX + TONE_BUTTON_SIZE + 1, toneY + TONE_BUTTON_SIZE + 1, HOVER_COLOR);
        this.renderToneEmoji(graphics, EmojiConfig.get().getSkinTone(), toneX, toneY, TONE_BUTTON_SIZE);
        if (toneHovered) graphics.requestCursor(CursorTypes.POINTING_HAND);
    }

    private void renderCategories(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
        int cy = y + 3;
        List<PickerCategory> categories = this.pickerCategories();
        this.categoryScroll = Mth.clamp(this.categoryScroll, 0, Math.max(0, categories.size() - visibleCategoryCount()));
        int end = Math.min(categories.size(), this.categoryScroll + visibleCategoryCount());
        for (int i = this.categoryScroll; i < end; i++) {
            PickerCategory value = categories.get(i);
            boolean isSelected = value.equals(this.selected) && this.search.isEmpty();
            boolean hovered = contains(mouseX, mouseY, x + 4, cy, CATEGORY_BUTTON_SIZE, CATEGORY_BUTTON_SIZE);
            if (isSelected || hovered) {
                graphics.fill(x + 4, cy, x + 4 + CATEGORY_BUTTON_SIZE, cy + CATEGORY_BUTTON_SIZE, isSelected ? SELECTED_COLOR : HOVER_COLOR);
            }
            this.renderPickerCategoryIcon(graphics, value, x + 5, cy + 1);
            if (hovered) graphics.requestCursor(CursorTypes.POINTING_HAND);
            cy += CATEGORY_STEP;
        }
        this.renderCategoryScrollbar(graphics, x, y, categories.size());
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
        List<EmojiRegistry.EmojiEntry> entries = this.entries();
        int gridX = gridX(x);
        int gridY = gridY(y);
        String header = this.search.isEmpty() ? this.selectedLabel() : Component.translatable("twemoji.picker.search_header").getString();
        int headerY = headerY(y);
        graphics.drawString(this.font, header, gridX, headerY, TEXT_COLOR, false);
        graphics.drawString(this.font, this.categoryOpen || !this.search.isEmpty() ? "v" : ">", gridX + this.font.width(header) + 5, headerY, MUTED_COLOR, false);
        if (this.search.isEmpty() && contains(mouseX, mouseY, gridX, headerY - 2, PANEL_WIDTH - CATEGORY_WIDTH - 12, 13)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        if (this.search.isEmpty() && !this.categoryOpen) return;

        int start = this.scroll * columns();
        int end = Math.min(entries.size(), start + visibleRows() * columns());
        for (int i = start; i < end; i++) {
            int cell = i - start;
            int cx = gridX + cell % columns() * CELL;
            int cy = gridY + cell / columns() * CELL;
            boolean hovered = contains(mouseX, mouseY, cx, cy, CELL, CELL) && !this.isToneInteraction(mouseX, mouseY, x, y);
            boolean focused = i == this.cursorIndex;
            if (hovered) {
                graphics.fill(cx, cy, cx + CELL, cy + CELL, HOVER_COLOR);
                graphics.requestCursor(CursorTypes.POINTING_HAND);
                this.hoveredEntry = entries.get(i);
            } else if (focused) {
                graphics.fill(cx, cy, cx + CELL, cy + CELL, SELECTED_COLOR);
                this.hoveredEntry = entries.get(i);
            }
            this.renderSprite(graphics, EmojiRegistry.INSTANCE.spriteForTone(entries.get(i), EmojiConfig.get().getSkinTone()), cx + (CELL - EMOJI_SIZE) / 2, cy + (CELL - EMOJI_SIZE) / 2, EMOJI_SIZE);
        }
        this.renderEmojiScrollbar(graphics, x, y, entries.size());
    }

    private void renderFooter(GuiGraphics graphics, int x, int y) {
        int fy = y + PANEL_HEIGHT - FOOTER_HEIGHT;
        graphics.fill(x + CATEGORY_WIDTH, fy, x + PANEL_WIDTH, y + PANEL_HEIGHT, FOOTER_COLOR);
        EmojiRegistry.EmojiEntry entry = this.hoveredEntry;
        if (entry == null) {
            List<EmojiRegistry.EmojiEntry> entries = this.entries();
            if (!entries.isEmpty()) entry = entries.getFirst();
        }
        if (entry == null) return;
        this.renderSprite(graphics, EmojiRegistry.INSTANCE.spriteForTone(entry, EmojiConfig.get().getSkinTone()), x + CATEGORY_WIDTH + 6, fy + 4, 16);
        graphics.drawString(this.font, trimToWidth(entry.shortcode(), PANEL_WIDTH - CATEGORY_WIDTH - 29), x + CATEGORY_WIDTH + 27, fy + 8, TEXT_COLOR, false);
    }

    private String searchPlaceholder() {
        if (this.hoveredEntry == null) return Component.translatable("twemoji.picker.search.placeholder").getString();
        String shortcode = this.hoveredEntry.shortcode();
        return shortcode.length() > 2 ? shortcode.substring(1, shortcode.length() - 1) : shortcode;
    }

    private void renderToneMenu(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
        int selected = EmojiConfig.get().getSkinTone();
        int tx = toneMenuX(x);
        int ty = toneMenuY(y);
        int height = TONE_OPTION_SIZE * 5 + 6;
        graphics.fill(tx, ty, tx + 26, ty + height, 0xF0070709);
        drawBorder(graphics, tx, ty, 26, height, 0xFF202124);
        int row = 0;
        for (int tone = 0; tone <= 5; tone++) {
            if (tone == selected) continue;
            int cy = ty + 3 + row * TONE_OPTION_SIZE;
            boolean hovered = contains(mouseX, mouseY, tx + 3, cy, 20, 20);
            if (hovered) {
                graphics.fill(tx + 3, cy, tx + 23, cy + 20, HOVER_COLOR);
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
            this.renderToneEmoji(graphics, tone, tx + 5, cy + 2, 16);
            row++;
        }
    }

    private PickerCategory categoryAt(int mouseX, int mouseY, int x, int y) {
        int cy = y + 3;
        List<PickerCategory> categories = this.pickerCategories();
        int end = Math.min(categories.size(), this.categoryScroll + visibleCategoryCount());
        for (int i = this.categoryScroll; i < end; i++) {
            PickerCategory value = categories.get(i);
            if (contains(mouseX, mouseY, x + 4, cy, CATEGORY_BUTTON_SIZE, CATEGORY_BUTTON_SIZE)) return value;
            cy += CATEGORY_STEP;
        }
        return null;
    }

    private Integer toneAt(int mouseX, int mouseY, int x, int y) {
        if (!this.toneOpen) return null;
        int selected = EmojiConfig.get().getSkinTone();
        int tx = toneMenuX(x);
        int ty = toneMenuY(y);
        int row = 0;
        for (int tone = 0; tone <= 5; tone++) {
            if (tone == selected) continue;
            if (contains(mouseX, mouseY, tx + 3, ty + 3 + row * TONE_OPTION_SIZE, 20, 20)) return tone;
            row++;
        }
        return null;
    }

    private boolean isToneInteraction(int mouseX, int mouseY, int x, int y) {
        if (contains(mouseX, mouseY, toneButtonX(x), toneButtonY(y), TONE_BUTTON_SIZE, TONE_BUTTON_SIZE)) return true;
        if (!this.toneOpen) return false;
        return contains(mouseX, mouseY, toneMenuX(x), toneMenuY(y), 26, TONE_OPTION_SIZE * 5 + 6);
    }

    private boolean headerAt(int mouseX, int mouseY, int x, int y) {
        if (!this.search.isEmpty()) return false;
        return contains(mouseX, mouseY, gridX(x), headerY(y) - 2, PANEL_WIDTH - CATEGORY_WIDTH - 12, 13);
    }

    private EmojiRegistry.EmojiEntry entryAt(int mouseX, int mouseY, int x, int y) {
        int gridX = gridX(x);
        int gridY = gridY(y);
        if (!contains(mouseX, mouseY, gridX, gridY, columns() * CELL, visibleRows() * CELL)) return null;
        int col = (mouseX - gridX) / CELL;
        int row = (mouseY - gridY) / CELL;
        int index = (row + this.scroll) * columns() + col;
        List<EmojiRegistry.EmojiEntry> entries = this.entries();
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    private List<EmojiRegistry.EmojiEntry> entries() {
        List<EmojiRegistry.EmojiEntry> entries = new ArrayList<>();
        String search = this.search.toLowerCase(Locale.ROOT).replace(" ", "_");
        if (search.isEmpty()) {
            if (!this.categoryOpen) return entries;
            entries.addAll(this.entriesFor(this.selected));
            return entries;
        }
        for (EmojiRegistry.EmojiEntry entry : EmojiRegistry.INSTANCE.getEntries().values()) {
            if (EmojiRegistry.isToneVariant(entry)) continue;
            String shortcode = entry.shortcode().toLowerCase(Locale.ROOT);
            if (shortcode.contains(search)) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private List<PickerCategory> pickerCategories() {
        List<PickerCategory> categories = new ArrayList<>();
        if (this.hasFrequentEntries()) categories.add(new BuiltinCategory(Category.FREQUENT));
        for (String name : EmojiRegistry.INSTANCE.getCustomCategoryEntries().keySet()) {
            categories.add(new CustomCategory(name));
        }
        if (!EmojiRegistry.INSTANCE.getCustomEntries().isEmpty()) {
            boolean allCategorized = EmojiRegistry.INSTANCE.getCustomCategoryEntries().values().stream()
                .mapToInt(List::size).sum() >= EmojiRegistry.INSTANCE.getCustomEntries().size();
            if (!allCategorized) categories.add(new BuiltinCategory(Category.CUSTOM));
        }
        categories.add(new BuiltinCategory(Category.PEOPLE));
        categories.add(new BuiltinCategory(Category.NATURE));
        categories.add(new BuiltinCategory(Category.FOOD));
        categories.add(new BuiltinCategory(Category.ACTIVITY));
        categories.add(new BuiltinCategory(Category.TRAVEL));
        categories.add(new BuiltinCategory(Category.OBJECTS));
        categories.add(new BuiltinCategory(Category.SYMBOLS));
        categories.add(new BuiltinCategory(Category.FLAGS));
        return categories;
    }

    private boolean hasFrequentEntries() {
        for (String name : EmojiConfig.get().getFrequentEmojis()) {
            EmojiRegistry.EmojiEntry entry = EmojiRegistry.INSTANCE.get(name);
            if (entry != null && !EmojiRegistry.isToneVariant(entry)) return true;
        }
        return false;
    }

    private String selectedLabel() {
        return switch (this.selected) {
            case BuiltinCategory b -> b.category().label();
            case CustomCategory c -> c.name();
        };
    }

    private void normalizeCategory() {
        List<PickerCategory> categories = this.pickerCategories();
        if (!categories.contains(this.selected)) {
            this.selected = new BuiltinCategory(Category.PEOPLE);
            this.categoryOpen = true;
            this.scroll = 0;
        }
        this.categoryScroll = Mth.clamp(this.categoryScroll, 0, Math.max(0, categories.size() - visibleCategoryCount()));
    }

    private boolean selectAdjacentCategory(int direction) {
        List<PickerCategory> categories = this.pickerCategories();
        int current = categories.indexOf(this.selected);
        if (current == -1) return false;
        int next = current + direction;
        while (next >= 0 && next < categories.size()) {
            PickerCategory candidate = categories.get(next);
            if (!this.entriesFor(candidate).isEmpty()) {
                this.selected = candidate;
                this.categoryOpen = true;
                this.ensureCategoryVisible(next);
                return true;
            }
            next += direction;
        }
        return false;
    }

    private int maxScrollForCurrentCategory() {
        int rows = (this.entriesFor(this.selected).size() + columns() - 1) / columns();
        return Math.max(0, rows - visibleRows());
    }

    private void ensureCategoryVisible(int index) {
        if (index < this.categoryScroll) {
            this.categoryScroll = index;
        } else if (index >= this.categoryScroll + visibleCategoryCount()) {
            this.categoryScroll = index - visibleCategoryCount() + 1;
        }
        this.categoryScroll = Mth.clamp(this.categoryScroll, 0, Math.max(0, this.pickerCategories().size() - visibleCategoryCount()));
    }

    private List<EmojiRegistry.EmojiEntry> entriesFor(PickerCategory cat) {
        return switch (cat) {
            case BuiltinCategory b -> entriesForBuiltin(b.category());
            case CustomCategory c -> List.copyOf(EmojiRegistry.INSTANCE.getCustomCategoryEntries().getOrDefault(c.name(), List.of()));
        };
    }

    private List<EmojiRegistry.EmojiEntry> entriesForBuiltin(Category category) {
        List<EmojiRegistry.EmojiEntry> entries = new ArrayList<>();
        if (category == Category.FREQUENT) {
            for (String name : EmojiConfig.get().getFrequentEmojis()) {
                EmojiRegistry.EmojiEntry entry = EmojiRegistry.INSTANCE.get(name);
                if (entry != null && !EmojiRegistry.isToneVariant(entry)) entries.add(entry);
            }
        } else if (category == Category.CUSTOM) {
            entries.addAll(EmojiRegistry.INSTANCE.getCustomEntries());
        } else {
            entries.addAll(EmojiRegistry.INSTANCE.getCategoryEntries(category.id));
        }
        return entries;
    }

    private void renderCategoryScrollbar(GuiGraphics graphics, int x, int y, int total) {
        int visible = visibleCategoryCount();
        if (total <= visible) return;
        this.renderScrollbar(graphics, x + CATEGORY_WIDTH - 3, y + 3, PANEL_HEIGHT - NAV_HEIGHT - 6, total, visible, this.categoryScroll, 1);
    }

    private void renderPickerCategoryIcon(GuiGraphics graphics, PickerCategory cat, int x, int y) {
        switch (cat) {
            case BuiltinCategory b -> renderCategoryIcon(graphics, b.category(), x, y);
            case CustomCategory c -> {
                String iconName = EmojiRegistry.INSTANCE.getCategoryIcons().get(c.name());
                EmojiRegistry.EmojiEntry iconEntry = iconName != null && !iconName.isBlank()
                    ? EmojiRegistry.INSTANCE.get(iconName) : null;
                if (iconEntry == null) {
                    List<EmojiRegistry.EmojiEntry> entries = EmojiRegistry.INSTANCE.getCustomCategoryEntries().getOrDefault(c.name(), List.of());
                    iconEntry = entries.isEmpty() ? null : entries.getFirst();
                }
                if (iconEntry != null) {
                    this.renderSprite(graphics, EmojiRegistry.INSTANCE.spriteForTone(iconEntry, 0), x, y, CATEGORY_ICON_SIZE);
                } else {
                    graphics.drawString(this.font, c.name().substring(0, 1).toUpperCase(Locale.ROOT), x + 3, y + 2, MUTED_COLOR, false);
                }
            }
        }
    }

    private void renderEmojiScrollbar(GuiGraphics graphics, int x, int y, int total) {
        int visible = visibleRows() * columns();
        if (total <= visible) return;
        int rows = (total + columns() - 1) / columns();
        this.renderScrollbar(graphics, x + PANEL_WIDTH - 5, gridY(y), visibleRows() * CELL, rows, visibleRows(), this.scroll, 2);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height, int total, int visible, int offset, int trackWidth) {
        int maxOffset = Math.max(1, total - visible);
        int thumbHeight = Math.max(8, height * visible / total);
        int travel = Math.max(1, height - thumbHeight);
        int thumbY = y + travel * offset / maxOffset;
        graphics.fill(x, y, x + trackWidth, y + height, SCROLL_TRACK_COLOR);
        graphics.fill(x - 1, thumbY, x + trackWidth + 1, thumbY + thumbHeight, SCROLL_THUMB_COLOR);
        graphics.fill(x, thumbY + 1, x + trackWidth, thumbY + thumbHeight - 1, BORDER_COLOR);
    }

    private void renderToneEmoji(GuiGraphics graphics, int tone, int x, int y, int size) {
        EmojiRegistry.EmojiEntry clap = EmojiRegistry.INSTANCE.get("clap");
        if (clap != null) {
            this.renderSprite(graphics, EmojiRegistry.INSTANCE.spriteForTone(clap, tone), x, y, size);
        } else {
            renderFace(graphics, x, y, size, toneColor(tone), 0xFF2B2D31);
        }
    }

    private void renderCategoryIcon(GuiGraphics graphics, Category category, int x, int y) {
        EmojiRegistry.EmojiEntry entry = EmojiRegistry.INSTANCE.get(category.iconName);
        if (entry != null) {
            this.renderSprite(graphics, EmojiRegistry.INSTANCE.spriteForTone(entry, 0), x, y, CATEGORY_ICON_SIZE);
            return;
        }
        graphics.drawString(this.font, category.label().substring(0, 1), x + 3, y + 2, MUTED_COLOR, false);
    }

    private void renderSprite(GuiGraphics graphics, EmojiSprite sprite, int x, int y, int size) {
        if (sprite == null) return;
        sprite.blit(graphics, x, y, size);
    }

    private String trimToWidth(String text, int width) {
        if (this.font.width(text) <= width) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end) + suffix) > width) {
            end--;
        }
        return end == 0 ? suffix : text.substring(0, end) + suffix;
    }

    private static void renderFace(GuiGraphics graphics, int x, int y, int size, int fill, int ink) {
        graphics.fill(x + 2, y, x + size - 2, y + size, fill);
        graphics.fill(x, y + 2, x + size, y + size - 2, fill);
        graphics.fill(x + size / 4, y + size / 3, x + size / 4 + 2, y + size / 3 + 2, ink);
        graphics.fill(x + size * 3 / 4 - 1, y + size / 3, x + size * 3 / 4 + 1, y + size / 3 + 2, ink);
        graphics.fill(x + size / 3, y + size * 2 / 3, x + size * 2 / 3, y + size * 2 / 3 + 2, ink);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static int toneColor(int tone) {
        return switch (tone) {
            case 1 -> 0xFFFFD7A8;
            case 2 -> 0xFFE8B27D;
            case 3 -> 0xFFC68642;
            case 4 -> 0xFF8D5524;
            case 5 -> 0xFF5C3A21;
            default -> 0xFFFFD84D;
        };
    }

    private static int panelX(int screenWidth) {
        return Math.max(4, screenWidth - PANEL_WIDTH - 6);
    }

    private static int panelY(int screenHeight) {
        return Math.max(4, screenHeight - PANEL_HEIGHT - 20);
    }

    private static int buttonX(int screenWidth) {
        return screenWidth - 16;
    }

    private static int buttonY(int screenHeight) {
        return screenHeight - 12;
    }

    private static int searchX(int panelX) {
        return panelX + 6;
    }

    private static int searchY(int panelY) {
        return panelY + 4;
    }

    private static int searchWidth(int panelX) {
        return toneButtonX(panelX) - searchX(panelX) - 5;
    }

    private static int toneButtonX(int panelX) {
        return panelX + PANEL_WIDTH - 25;
    }

    private static int toneButtonY(int panelY) {
        return panelY + 3;
    }

    private static int toneMenuX(int panelX) {
        return panelX + PANEL_WIDTH - 30;
    }

    private static int toneMenuY(int panelY) {
        return panelY + NAV_HEIGHT + 2;
    }

    private static int gridX(int panelX) {
        return panelX + CATEGORY_WIDTH + 7;
    }

    private static int gridY(int panelY) {
        return panelY + NAV_HEIGHT + CATEGORY_HEADER_HEIGHT + 4;
    }

    private static int headerY(int panelY) {
        return panelY + NAV_HEIGHT + 5;
    }

    private static int columns() {
        return 8;
    }

    private static int visibleRows() {
        return 4;
    }

    private static int visibleCategoryCount() {
        return Math.max(1, (PANEL_HEIGHT - NAV_HEIGHT - 6) / CATEGORY_STEP);
    }

    private static boolean contains(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private enum Category {
        FREQUENT("frequent", "twemoji.picker.category.frequent", "clock3"),
        CUSTOM("custom", "twemoji.picker.category.custom", "sparkles"),
        PEOPLE("people", "twemoji.picker.category.people", "smiley"),
        NATURE("nature", "twemoji.picker.category.nature", "deciduous_tree"),
        FOOD("food", "twemoji.picker.category.food", "hamburger"),
        ACTIVITY("activities", "twemoji.picker.category.activity", "soccer"),
        TRAVEL("travel", "twemoji.picker.category.travel", "red_car"),
        OBJECTS("objects", "twemoji.picker.category.objects", "bulb"),
        SYMBOLS("symbols", "twemoji.picker.category.symbols", "heart"),
        FLAGS("flags", "twemoji.picker.category.flags", "triangular_flag_on_post");

        private final String id;
        private final String labelKey;
        private final String iconName;

        Category(String id, String labelKey, String iconName) {
            this.id = id;
            this.labelKey = labelKey;
            this.iconName = iconName;
        }

        String label() {
            return Component.translatable(labelKey).getString();
        }
    }
}
