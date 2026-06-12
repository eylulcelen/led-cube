package com.cubeapp.util;

import com.cubeapp.model.Cube;
import com.cubeapp.model.LedColor;

/**
 * Tints lit LEDs based on temperature.
 *
 * < 10°C  → icy blue tint
 * 10–20°C → cool white (no tint)
 * 20–30°C → warm yellow tint
 * > 30°C  → hot red tint
 *
 * Returns a NEW cube.
 */

public class TemperatureColorFilter {

    public static Cube apply(Cube source, double tempCelsius) {
        Cube result = new Cube(source);

        double r = 1.0, g = 1.0, b = 1.0; // tint multipliers

        if (tempCelsius < 10) {
            // Icy blue — reduce red and green
            double t = Math.max(0, (10 - tempCelsius) / 10.0); // 0→1 as temp drops
            r = 1.0 - t * 0.6;
            g = 1.0 - t * 0.3;
            b = 1.0;
        } else if (tempCelsius <= 20) {
            // Neutral — no tint
            r = 1.0; g = 1.0; b = 1.0;
        } else if (tempCelsius <= 30) {
            // Warm yellow — reduce blue
            double t = (tempCelsius - 20) / 10.0; // 0→1
            r = 1.0;
            g = 1.0;
            b = 1.0 - t * 0.6;
        } else {
            // Hot red — reduce green and blue
            double t = Math.min(1.0, (tempCelsius - 30) / 10.0);
            r = 1.0;
            g = 1.0 - t * 0.7;
            b = 1.0 - t * 0.8;
        }

        for (int x = 0; x < Cube.SIZE; x++)
            for (int y = 0; y < Cube.SIZE; y++)
                for (int z = 0; z < Cube.SIZE; z++) {
                    LedColor c = result.get(x, y, z);
                    if (!c.isOff()) {
                        result.set(x, y, z, new LedColor(
                                (int)(c.getR() * r),
                                (int)(c.getG() * g),
                                (int)(c.getB() * b)
                        ));
                    }
                }

        return result;
    }
}