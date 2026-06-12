package com.cubeapp.model;

import java.util.Objects;

/**
 * Immutable RGB color for a single LED.
 * Values are 0–255.
 */
public final class LedColor {

    public static final LedColor OFF   = new LedColor(0,   0,   0);
    public static final LedColor RED   = new LedColor(255, 0,   0);
    public static final LedColor GREEN = new LedColor(0,   255, 0);
    public static final LedColor BLUE  = new LedColor(0,   0,   255);
    public static final LedColor WHITE = new LedColor(255, 255, 255);

    private final int r, g, b;

    public LedColor(int r, int g, int b) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
    }

    public int getR() { return r; }
    public int getG() { return g; }
    public int getB() { return b; }

    public boolean isOff() {
        return r == 0 && g == 0 && b == 0;
    }

    //returns a new color dimmed by factor 0.0–1.0
    public LedColor dim(double factor) {
        return new LedColor((int)(r * factor), (int)(g * factor), (int)(b * factor));
    }

    //for values to stay in length of 0 to 255 when it gets values like -10 or 300
    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof LedColor c))
            return false;
        return r == c.r && g == c.g && b == c.b;
    }

    @Override public int hashCode() {
        return Objects.hash(r, g, b);
    }

    @Override public String toString() {
        return "LedColor(%d,%d,%d)".formatted(r, g, b);
    }
}