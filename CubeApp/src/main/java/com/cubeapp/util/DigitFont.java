package com.cubeapp.util;

/**
 * 3×5 pixel font for digits 0–9.
 * Each digit is boolean[5][3] — [row][col], row 0 = top.
 */
public class DigitFont {

    private static final boolean[][][] DIGITS = {
            // 0
            { {true,  true,  true},
                    {true,  false, true},
                    {true,  false, true},
                    {true,  false, true},
                    {true,  true,  true} },
            // 1
            { {false, true,  false},
                    {false, true,  false},
                    {false, true,  false},
                    {false, true,  false},
                    {false, true,  false} },
            // 2
            { {true,  true,  true},
                    {false, false, true},
                    {true,  true,  true},
                    {true,  false, false},
                    {true,  true,  true} },
            // 3
            { {true,  true,  true},
                    {false, false, true},
                    {true,  true,  true},
                    {false, false, true},
                    {true,  true,  true} },
            // 4
            { {true,  false, true},
                    {true,  false, true},
                    {true,  true,  true},
                    {false, false, true},
                    {false, false, true} },
            // 5
            { {true,  true,  true},
                    {true,  false, false},
                    {true,  true,  true},
                    {false, false, true},
                    {true,  true,  true} },
            // 6
            { {true,  true,  true},
                    {true,  false, false},
                    {true,  true,  true},
                    {true,  false, true},
                    {true,  true,  true} },
            // 7
            { {true,  true,  true},
                    {false, false, true},
                    {false, false, true},
                    {false, false, true},
                    {false, false, true} },
            // 8
            { {true,  true,  true},
                    {true,  false, true},
                    {true,  true,  true},
                    {true,  false, true},
                    {true,  true,  true} },
            // 9
            { {true,  true,  true},
                    {true,  false, true},
                    {true,  true,  true},
                    {false, false, true},
                    {true,  true,  true} }
    };

    /**
     * Returns the 3×5 bitmap for a digit 0–9.
     * [row][col] where row 0 = top, col 0 = left.
     */
    public static boolean[][] get(int digit) {
        return DIGITS[digit % 10];
    }
}