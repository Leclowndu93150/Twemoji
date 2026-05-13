package com.leclowndu93150.twemoji.client.registry;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;

import java.util.Map;

public final class ShapingTable {

    public static final ShapingTable EMPTY = new ShapingTable(new Node(), IntSets.EMPTY_SET);

    private final Node root;
    private final IntSet rootCodepoints;

    private ShapingTable(Node root, IntSet rootCodepoints) {
        this.root = root;
        this.rootCodepoints = rootCodepoints;
    }

    public static ShapingTable build(Map<String, Integer> rgiToPua) {
        Node root = new Node();
        for (Map.Entry<String, Integer> e : rgiToPua.entrySet()) {
            String seq = e.getKey();
            Node node = root;
            int i = 0;
            while (i < seq.length()) {
                int cp = seq.codePointAt(i);
                node = node.child(cp);
                i += Character.charCount(cp);
            }
            node.terminal = e.getValue();
        }
        IntSet roots = root.children == null ? IntSets.EMPTY_SET : IntSets.unmodifiable(new IntOpenHashSet(root.children.keySet()));
        return new ShapingTable(root, roots);
    }

    public Node root() {
        return root;
    }

    public boolean isEmpty() {
        return root.children == null || root.children.isEmpty();
    }

    public boolean stringHasCandidate(String s) {
        if (rootCodepoints.isEmpty()) return false;
        int len = s.length();
        for (int i = 0; i < len; ) {
            int cp = s.codePointAt(i);
            if (rootCodepoints.contains(cp)) return true;
            i += Character.charCount(cp);
        }
        return false;
    }

    public static final class Node {
        private Int2ObjectMap<Node> children;
        private Integer terminal;

        Node child(int codepoint) {
            if (children == null) children = new Int2ObjectOpenHashMap<>(2);
            Node existing = children.get(codepoint);
            if (existing != null) return existing;
            Node fresh = new Node();
            children.put(codepoint, fresh);
            return fresh;
        }

        Int2ObjectMap<Node> childrenMap() {
            return children;
        }

        public Node next(int codepoint) {
            return children == null ? null : children.get(codepoint);
        }

        public Integer terminal() {
            return terminal;
        }
    }
}
