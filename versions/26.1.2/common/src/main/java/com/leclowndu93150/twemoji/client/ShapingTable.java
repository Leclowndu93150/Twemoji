package com.leclowndu93150.twemoji.client;

import java.util.HashMap;
import java.util.Map;

public final class ShapingTable {

    public static final ShapingTable EMPTY = new ShapingTable(new Node());

    private final Node root;

    private ShapingTable(Node root) {
        this.root = root;
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
        return new ShapingTable(root);
    }

    public Node root() {
        return root;
    }

    public boolean isEmpty() {
        return root.children == null || root.children.isEmpty();
    }

    public static final class Node {
        private Map<Integer, Node> children;
        private Integer terminal;

        Node child(int codepoint) {
            if (children == null) children = new HashMap<>(2);
            return children.computeIfAbsent(codepoint, k -> new Node());
        }

        public Node next(int codepoint) {
            return children == null ? null : children.get(codepoint);
        }

        public Integer terminal() {
            return terminal;
        }
    }
}
