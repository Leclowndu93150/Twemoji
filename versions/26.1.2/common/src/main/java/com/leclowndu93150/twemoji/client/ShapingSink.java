package com.leclowndu93150.twemoji.client;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;

public final class ShapingSink implements FormattedCharSink {

    private static final int MAX_BUF = 16;

    private final FormattedCharSink delegate;
    private final ShapingTable table;
    private final int[] bufPositions = new int[MAX_BUF];
    private final Style[] bufStyles = new Style[MAX_BUF];
    private final int[] bufCodepoints = new int[MAX_BUF];
    private int bufLen;
    private ShapingTable.Node currentNode;
    private int matchLen;
    private int matchPua;
    private boolean stopped;

    public ShapingSink(FormattedCharSink delegate, ShapingTable table) {
        this.delegate = delegate;
        this.table = table;
        this.currentNode = table.root();
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
        boolean ok = delegate.accept(position, style, codepoint);
        if (!ok) stopped = true;
        return ok;
    }

    private boolean flushBuffer() {
        if (bufLen == 0) return true;
        int emitted = 0;
        if (matchLen > 0) {
            if (!delegate.accept(bufPositions[0], bufStyles[0], matchPua)) {
                stopped = true;
                return false;
            }
            emitted = matchLen;
        }
        for (int i = emitted; i < bufLen; i++) {
            if (!delegate.accept(bufPositions[i], bufStyles[i], bufCodepoints[i])) {
                stopped = true;
                return false;
            }
        }
        bufLen = 0;
        matchLen = 0;
        currentNode = table.root();
        return true;
    }

    public boolean finish() {
        return flushBuffer();
    }
}
