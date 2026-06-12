package com.cubeapp.util;

import com.cubeapp.model.Animation;
import com.cubeapp.model.Cube;
import com.cubeapp.model.Frame;
import com.cubeapp.model.LedColor;

import java.time.LocalTime;

/**
 * Generates a 3-frame Animation showing the current hour
 * as two digits on the cube face (x=0 plane).
 *
 * Layout on the x=0 face (y=0-4, z=0-4):
 *   Tens digit: y=0-2, z=0-4  (3 wide, 5 tall)
 *   Gap:        y=3
 *   Units digit: y=4... wait — 5 cols total, 3+1+3 = 7, too wide.
 *
 * So we use TWO z-layers instead:
 *   Hour tens:  displayed on z=4 layer (top), scrolling down
 *   Hour units: displayed on z=3 layer
 *   Then a "colon" blink on z=2
 *
 * Actually simpler: show tens digit on left half (y 0-2),
 * units digit on right half (y 2-4) — 3 cols each, sharing y=2.
 * Draw on x=0 face, rows map to z (z=4=top).
 */
public class HourAnimationGenerator {

    // Color for the digits — changes with temperature
    private static LedColor digitColor = LedColor.WHITE;

    public static void setDigitColor(LedColor color) {
        digitColor = color;
    }

    /**
     * Generates a looping animation for the current hour.
     * Frame 1: both digits lit
     * Frame 2: both digits lit (same — gives steady display)
     * Frame 3: blank (subtle blink)
     */
    public static Animation generate() {
        int hour  = LocalTime.now().getHour(); // 0-23
        int tens  = hour / 10;
        int units = hour % 10;

        Animation anim = new Animation("Hour: " + hour);
        anim.setLooping(true);

        // Frame 1 & 2 — lit (1000ms each)
        anim.addFrame(buildDigitFrame(tens, units, 1000));
        anim.addFrame(buildDigitFrame(tens, units, 1000));

        // Frame 3 — dim blink (300ms)
        anim.addFrame(buildDimFrame(tens, units, 300));

        return anim;
    }

    private static Frame buildDigitFrame(int tens, int units, int durationMs) {
        Frame frame = new Frame(durationMs);
        drawDigit(frame.getCube(), tens,  0); // left: y cols 0-2
        drawDigit(frame.getCube(), units, 3); // right: y cols 2-4 (shifted +2... wait +3)
        return frame;
    }

    private static Frame buildDimFrame(int tens, int units, int durationMs) {
        Frame frame = buildDigitFrame(tens, units, durationMs);
        LedColor dimColor = digitColor.dim(0.2);
        Cube cube = frame.getCube();
        for (int x = 0; x < Cube.SIZE; x++)
            for (int y = 0; y < Cube.SIZE; y++)
                for (int z = 0; z < Cube.SIZE; z++)
                    if (!cube.get(x, y, z).isOff())
                        cube.set(x, y, z, dimColor);
        return frame;
    }

    /**
     * Draws a digit onto the cube at y-offset yStart.
     * Uses x=0 face, z=4 (top) down to z=0.
     * Digit is 3 wide (y) × 5 tall (z).
     */
    private static void drawDigit(Cube cube, int digit, int yStart) {
        boolean[][] bitmap = DigitFont.get(digit);
        for (int row = 0; row < 5; row++) {
            int z = 4 - row; // row 0 → z=4 (top)
            for (int col = 0; col < 3; col++) {
                int y = yStart + col;
                if (y < Cube.SIZE && bitmap[row][col]) {
                    // Draw on x=0 and x=1 for thickness
                    cube.set(0, y, z, digitColor);
                    cube.set(1, y, z, digitColor.dim(0.5));
                }
            }
        }
    }
}