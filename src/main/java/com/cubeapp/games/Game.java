package com.cubeapp.games;

import com.cubeapp.model.Cube;

/**
 * Common contract for all cube games.
 * The UI calls tick() on a timer, reads getState() to render,
 * and checks isGameOver() to know when to stop.
 */
public interface Game {

    /** Advance game by one step */
    void tick();

    /** Write current game state into the given cube (for rendering) */
    void writeState(Cube cube);

    /** Reset to initial state */
    void reset();

    boolean isGameOver();

    int getScore();
}