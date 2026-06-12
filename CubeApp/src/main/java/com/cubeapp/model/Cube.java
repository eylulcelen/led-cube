package com.cubeapp.model;

/**
 * Mutable 5x5x5 RGB cube state.
 * Axis: x=left/right, y=front/back, z=bottom/top.
 */

public class Cube {

    public static final int SIZE = 5;

    // [x][y][z]
    private final LedColor[][][] leds;

    public Cube() {
        leds = new LedColor[SIZE][SIZE][SIZE];
        fill(LedColor.OFF);
    }

    /** Deep-copy constructor */
    public Cube(Cube other) {
        leds = new LedColor[SIZE][SIZE][SIZE];
        for (int x = 0; x < SIZE; x++)
            // LedColor is immutable
            for (int y = 0; y < SIZE; y++)
                System.arraycopy(other.leds[x][y], 0, leds[x][y], 0, SIZE);
    }

    public LedColor get(int x, int y, int z) {
        return leds[x][y][z];
    }

    public void set(int x, int y, int z, LedColor color) {
        leds[x][y][z] = color;
    }

    public void fill(LedColor color) {
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                for (int z = 0; z < SIZE; z++)
                    leds[x][y][z] = color;
    }

    public void clear() { fill(LedColor.OFF); }

    /** Fill an entire Z layer (horizontal plane) */
    public void fillLayer(int z, LedColor color) {
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < SIZE; y++)
                leds[x][y][z] = color;
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
    }
}