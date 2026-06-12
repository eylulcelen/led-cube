package com.cubeapp.util;

import com.cubeapp.model.Cube;
import com.cubeapp.model.LedColor;

/**
 * Dims every LED in a cube by a brightness factor 0.0–1.0.
 * Returns a NEW cube — never modifies the original.
 */
public class BrightnessFilter {

    /**
     * @param source     original cube
     * @param brightness 0.0 = off, 1.0 = full
     * @return new dimmed cube
     */
    public static Cube apply(Cube source, double brightness) {
        brightness = Math.max(0.0, Math.min(1.0, brightness));
        Cube result = new Cube(source); // deep copy

        for (int x = 0; x < Cube.SIZE; x++)
            for (int y = 0; y < Cube.SIZE; y++)
                for (int z = 0; z < Cube.SIZE; z++) {
                    LedColor c = result.get(x, y, z);
                    if (!c.isOff())
                        result.set(x, y, z, c.dim(brightness));
                }

        return result;
    }
}