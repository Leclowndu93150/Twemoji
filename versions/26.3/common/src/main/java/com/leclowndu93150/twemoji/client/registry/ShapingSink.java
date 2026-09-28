package com.leclowndu93150.twemoji.client.registry;

import com.leclowndu93150.twemoji.client.render.TwemojiSheets;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;

public final class ShapingSink implements FormattedCharSink {

    private static final int MAX_BUF = 16;
    private static final ThreadLocal<ShapingSink> POOL = ThreadLocal.withInitial(ShapingSink::new);

    private FormattedCharSink delegate;
    private ShapingTable table;
    private final int[] bufPositions = new int[MAX_BUF];
    private final Style[] bufStyles = new Style[MAX_BUF];
    private final int[] bufCodepoints = new int[MAX_BUF];
    private int bufLen;
    private ShapingTable.Node currentNode;
    private int matchLen;
    private int matchPua;
    private boolean stopped;
    private boolean inUse;
    private Style cachedPlainStyle;
    private Style cachedEmojiStyle;

    private ShapingSink() {}

    public static ShapingSink acquire(FormattedCharSink delegate, ShapingTable table) {
        ShapingSink sink = POOL.get();
        if (sink.inUse) {
            ShapingSink fresh = new ShapingSink();
            fresh.reset(delegate, table);
            fresh.inUse = true;
            return fresh;
        }
        sink.reset(delegate, table);
        sink.inUse = true;
        return sink;
    }

    private void reset(FormattedCharSink delegate, ShapingTable table) {
        this.delegate = delegate;
        this.table = table;
        this.currentNode = table.root();
        this.bufLen = 0;
        this.matchLen = 0;
        this.matchPua = 0;
        this.stopped = false;
        this.cachedPlainStyle = null;
        this.cachedEmojiStyle = null;
        for (int i = 0; i < MAX_BUF; i++) this.bufStyles[i] = null;
    }

    public void release() {
        this.delegate = null;
        this.table = null;
        this.currentNode = null;
        this.cachedPlainStyle = null;
        this.cachedEmojiStyle = null;
        for (int i = 0; i < MAX_BUF; i++) this.bufStyles[i] = null;
        this.inUse = false;
    }

    @Override
    public boolean accept(int position, Style style, int codepoint) {
        if (stopped) return false;
        ShapingTable.Node next = currentNode.next(codepoint);
        if (next == null && bufLen > 0) {
            if (!flushBuffer()) return false;
            next = table.root().next(codepoint);
        }
        if (next != null) {
            if (bufLen >= MAX_BUF) {
                if (!flushBuffer()) return false;
                next = table.root().next(codepoint);
                if (next == null) return passthrough(position, style, codepoint);
            }
            bufPositions[bufLen] = position;
            bufStyles[bufLen] = style;
            bufCodepoints[bufLen] = codepoint;
            bufLen++;
            currentNode = next;
            if (next.terminal() != null) {
                matchLen = bufLen;
                matchPua = next.terminal();
            }
            return true;
        }
        return passthrough(position, style, codepoint);
    }

    private boolean passthrough(int position, Style style, int codepoint) {
        currentNode = table.root();
        boolean ok = delegate.accept(position, styleFor(style, codepoint), codepoint);
        if (!ok) stopped = true;
        return ok;
    }

    private boolean flushBuffer() {
        if (bufLen == 0) return true;
        int emitted = 0;
        if (matchLen > 0) {
            int end = bufPositions[matchLen - 1] + Character.charCount(bufCodepoints[matchLen - 1]);
            int position = Math.max(0, end - Character.charCount(matchPua));
            if (!delegate.accept(position, emojiStyle(bufStyles[0]), matchPua)) {
                stopped = true;
                return false;
            }
            emitted = matchLen;
        }
        for (int i = emitted; i < bufLen; i++) {
            if (!delegate.accept(bufPositions[i], styleFor(bufStyles[i], bufCodepoints[i]), bufCodepoints[i])) {
                stopped = true;
                return false;
            }
        }
        bufLen = 0;
        matchLen = 0;
        currentNode = table.root();
        return true;
    }

    private Style styleFor(Style style, int codepoint) {
        return EmojiRegistry.INSTANCE.rendersInEmojiFont(codepoint) ? emojiStyle(style) : style;
    }

    private Style emojiStyle(Style style) {
        if (style != cachedPlainStyle) {
            cachedPlainStyle = style;
            cachedEmojiStyle = TwemojiSheets.withEmojiFont(style);
        }
        return cachedEmojiStyle;
    }

    public boolean finish() {
        return flushBuffer();
    }
}
