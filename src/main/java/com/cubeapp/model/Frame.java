package com.cubeapp.model;

/**
 * One animation frame: a full cube snapshot + how long to display it.
 */
public class Frame {

    private Cube cube;
    private int durationMs; // how long to show this frame

    public Frame(int durationMs) {
        this.cube = new Cube();
        this.durationMs = durationMs;
    }

    /** Deep-copy constructor */
    public Frame(Frame other) {
        this.cube = new Cube(other.cube);
        this.durationMs = other.durationMs;
    }

    public Cube getCube()          { return cube; }
    public int  getDurationMs()    { return durationMs; }
    public void setDurationMs(int ms) { this.durationMs = ms; }
}