package com.cubeapp.games.snake;

/**
 * 3D movement directions for snake.
 * A 5x5x5 cube gives 6 possible directions.
 */
public enum Direction {
    X_POS( 1,  0,  0),
    X_NEG(-1,  0,  0),
    Y_POS( 0,  1,  0),
    Y_NEG( 0, -1,  0),
    Z_POS( 0,  0,  1),
    Z_NEG( 0,  0, -1);

    public final int dx, dy, dz;

    Direction(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    /** Returns true if this direction is the exact opposite of other.
     *  Snake can't reverse into itself. */
    public boolean isOpposite(Direction other) {
        return this.dx == -other.dx
                && this.dy == -other.dy
                && this.dz == -other.dz;
    }
}