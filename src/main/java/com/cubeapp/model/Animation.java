package com.cubeapp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ordered list of frames with metadata.
 */

public class Animation {

    private String name;
    private boolean looping;
    private final List<Frame> frames = new ArrayList<>();

    public Animation(String name) {
        this.name    = name;
        this.looping = true;
    }

    public String getName(){ return name; }
    public void setName(String name) { this.name = name; }
    public boolean isLooping()  { return looping; }
    public void setLooping(boolean looping) { this.looping = looping; }

    public List<Frame> getFrames() { return Collections.unmodifiableList(frames); }

    public void addFrame(Frame frame) { frames.add(frame); }
    public void addFrame(int index, Frame frame) { frames.add(index, frame); }
    public void removeFrame(int index) { frames.remove(index); }
    public void moveFrame(int from, int to) { frames.add(to, frames.remove(from)); }
    public Frame getFrame(int index) { return frames.get(index); }
    public int getFrameCount() { return frames.size(); }

    public int getTotalDurationMs() {
        return frames.stream().mapToInt(Frame::getDurationMs).sum();
    }
}