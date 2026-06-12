package com.cubeapp.ui.editor;

import com.cubeapp.model.Animation;
import com.cubeapp.model.Frame;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

/**
 * Manages the bottom frame strip.
 * Notifies AnimationEditorController when selected frame changes.
 */
public class FrameTimelineController {

    @FXML private HBox frameStrip;

    private Animation        animation;
    private int              selectedIndex = 0;
    private Consumer<Integer> onFrameSelected; // callback → editor

    // Called by AnimationEditorController after FXML include resolves
    public void init(Animation animation, Consumer<Integer> onFrameSelected) {
        this.animation       = animation;
        this.onFrameSelected = onFrameSelected;
        refresh();
    }

    // ----------------------------------------------------------------
    // Public API used by AnimationEditorController
    // ----------------------------------------------------------------

    public int getSelectedIndex() { return selectedIndex; }

    public void setAnimation(Animation animation) {
        this.animation = animation;
        selectedIndex  = 0;
        refresh();
    }

    /** Rebuild the strip from scratch */
    public void refresh() {
        frameStrip.getChildren().clear();
        for (int i = 0; i < animation.getFrameCount(); i++) {
            final int idx = i;
            Button btn = new Button("F" + (i + 1)
                    + "\n" + animation.getFrame(i).getDurationMs() + "ms");
            btn.setPrefWidth(64);
            btn.setPrefHeight(56);
            btn.setStyle(baseStyle(i == selectedIndex));
            btn.setOnAction(e -> selectFrame(idx));
            frameStrip.getChildren().add(btn);
        }
    }

    // ----------------------------------------------------------------
    // FXML handlers
    // ----------------------------------------------------------------

    @FXML
    private void onAddFrame() {
        Frame newFrame = new Frame(200);
        // Copy current frame as starting point
        if (animation.getFrameCount() > 0) {
            Frame current = animation.getFrame(selectedIndex);
            for (int z = 0; z < 5; z++)
                for (int y = 0; y < 5; y++)
                    for (int x = 0; x < 5; x++)
                        newFrame.getCube().set(x, y, z, current.getCube().get(x, y, z));
        }
        animation.addFrame(selectedIndex + 1, newFrame);
        selectFrame(selectedIndex + 1);
        refresh();
    }

    @FXML
    private void onRemoveFrame() {
        if (animation.getFrameCount() <= 1) return; // keep at least 1 frame
        animation.removeFrame(selectedIndex);
        selectedIndex = Math.min(selectedIndex, animation.getFrameCount() - 1);
        refresh();
        onFrameSelected.accept(selectedIndex);
    }

    @FXML
    private void onMoveLeft() {
        if (selectedIndex <= 0) return;
        animation.moveFrame(selectedIndex, selectedIndex - 1);
        selectFrame(selectedIndex - 1);
    }

    @FXML
    private void onMoveRight() {
        if (selectedIndex >= animation.getFrameCount() - 1) return;
        animation.moveFrame(selectedIndex, selectedIndex + 1);
        selectFrame(selectedIndex + 1);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private void selectFrame(int idx) {
        selectedIndex = idx;
        refresh();
        onFrameSelected.accept(selectedIndex);
    }

    private String baseStyle(boolean selected) {
        String bg = selected ? "#2c3e6a" : "#1a1a2e";
        return "-fx-background-color: " + bg + "; "
                + "-fx-text-fill: white; "
                + "-fx-font-size: 10px; "
                + "-fx-border-color: " + (selected ? "#5dade2" : "#333") + "; "
                + "-fx-border-width: 1.5;";
    }
}