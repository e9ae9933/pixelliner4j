package org.aliceincradle.pxlsloader;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class Algorithm {
    public static boolean packRectangles(Rect[] rects, int atlasWidth, int atlasHeight) {
        Rect[] sortedRects = rects.clone();     // shallow copy?
        Arrays.sort(sortedRects, (u, v) -> Long.compare((long) v.h * v.w, (long) u.h * u.w));
        Node root = new Node(0, 0, atlasWidth, atlasHeight);
        for (Rect rect : sortedRects) {
            Node node = root.insert(rect.w, rect.h);
            if (node == null)
                return false;
            rect.x = node.x;
            rect.y = node.y;
        }
        return true;
    }
    
    public static class Rect {
        public int x, y;
        public int w, h;
        
        public Rect(int w, int h) {
            this.w = w;
            this.h = h;
        }
    }
    
    private static class Node {
        int x, y, w, h;
        Node right, down;
        boolean used = false;
        
        Node(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
        
        @Nullable Node insert(int imgW, int imgH) {
            if (used) {
                Node node = right.insert(imgW, imgH);
                if (node != null)
                    return node;
                return down.insert(imgW, imgH);
            } else if (imgW <= w && imgH <= h) {
                used = true;
                down = new Node(x, y + imgH, w, h - imgH);
                right = new Node(x + imgW, y, w - imgW, imgH);
                return this;
            }
            return null;
        }
    }
}
