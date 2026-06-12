package com.cubeapp.ui.editor;

import com.cubeapp.model.Animation;
import com.cubeapp.model.Cube;
import com.cubeapp.model.Frame;
import com.cubeapp.model.LedColor;
import com.cubeapp.ui.CubeRenderer;
import com.cubeapp.util.AnimationFileManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class AnimationEditorController {

    // FXML injected
    @FXML private BorderPane      root;
    @FXML private TextField       nameField;
    @FXML private ColorPicker     colorPicker;
    @FXML private Spinner<Integer> layerSpinner;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private Button          playBtn;
    @FXML private CheckBox        loopCheck;
    @FXML private GridPane        paintGrid;

    @FXML private FrameTimelineController timelineController;

    // App wiring
    private CubeRenderer    renderer;
    private Consumer<Cube>  onCubeUpdated;

    // Editor state
    private Animation           animation;
    private AnimationFileManager fileManager;
    private Timeline            playbackTimeline;
    private boolean             playing = false;
    private int                 playbackFrame = 0;

    private static final int GRID_CELL = 52; // px per paint cell

    // ----------------------------------------------------------------
    // Init
    // ----------------------------------------------------------------

    public void init(CubeRenderer renderer, Consumer<Cube> onCubeUpdated) {
        this.renderer      = renderer;
        this.onCubeUpdated = onCubeUpdated;
        this.fileManager   = new AnimationFileManager();

        // Start with a blank 1-frame animation
        animation = new Animation("New Animation");
        animation.addFrame(new Frame(200));
        nameField.setText(animation.getName());

        // Wire timeline controller
        timelineController.init(animation, this::onFrameSelected);

        // Layer spinner change → rebuild paint grid
        layerSpinner.valueProperty().addListener((o, a, b) -> rebuildPaintGrid());

        rebuildPaintGrid();
        renderCurrentFrame();
    }

    // ----------------------------------------------------------------
    // Paint grid
    // ----------------------------------------------------------------

    private void rebuildPaintGrid() {
        paintGrid.getChildren().clear();
        int z = layerSpinner.getValue();
        Frame frame = currentFrame();

        for (int y = 4; y >= 0; y--) {     // y=4 at top visually
            for (int x = 0; x < 5; x++) {
                LedColor led = frame.getCube().get(x, y, z);
                Rectangle rect = new Rectangle(GRID_CELL, GRID_CELL);
                rect.setArcWidth(6);
                rect.setArcHeight(6);
                rect.setFill(toFxColor(led));
                rect.setStroke(Color.web("#333"));
                rect.setStrokeWidth(1);

                final int fx = x, fy = y;
                rect.setOnMouseClicked(e -> onCellClicked(fx, fy, rect));

                // Right-click to erase
                rect.setOnContextMenuRequested(e -> onCellErased(fx, fy, rect));

                paintGrid.add(rect, x, 4 - y);
            }
        }
    }

    private void onCellClicked(int x, int y, Rectangle rect) {
        if (playing) return;
        int z = layerSpinner.getValue();
        LedColor color = fromFxColor(colorPicker.getValue());
        currentFrame().getCube().set(x, y, z, color);
        rect.setFill(colorPicker.getValue());
        renderCurrentFrame();
    }

    private void onCellErased(int x, int y, Rectangle rect) {
        if (playing) return;
        int z = layerSpinner.getValue();
        currentFrame().getCube().set(x, y, z, LedColor.OFF);
        rect.setFill(Color.web("#111"));
        renderCurrentFrame();
    }

    // ----------------------------------------------------------------
    // Frame selection (called by timeline)
    // ----------------------------------------------------------------

    private void onFrameSelected(int index) {
        durationSpinner.getValueFactory().setValue(currentFrame().getDurationMs());
        rebuildPaintGrid();
        renderCurrentFrame();
    }

    // ----------------------------------------------------------------
    // FXML handlers
    // ----------------------------------------------------------------

    @FXML
    private void onDurationChanged() {
        currentFrame().setDurationMs(durationSpinner.getValue());
        timelineController.refresh(); // update label in strip
    }

    @FXML
    private void onClearFrame() {
        currentFrame().getCube().clear();
        rebuildPaintGrid();
        renderCurrentFrame();
    }

    @FXML
    private void onDuplicateFrame() {
        Frame copy = new Frame(currentFrame());
        int idx = timelineController.getSelectedIndex();
        animation.addFrame(idx + 1, copy);
        timelineController.refresh();
    }

    @FXML
    private void onPlayStop() {
        if (playing) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }

    @FXML
    private void onSave() {
        animation.setName(nameField.getText());
        animation.setLooping(loopCheck.isSelected());

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Animation");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Animation", "*.json"));
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file == null) return;

        try {
            fileManager.save(animation, file);
        } catch (IOException e) {
            showAlert("Save failed", e.getMessage());
        }
    }

    @FXML
    private void onLoad() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Animation");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Animation", "*.json"));
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) return;

        try {
            animation = fileManager.load(file);
            nameField.setText(animation.getName());
            loopCheck.setSelected(animation.isLooping());
            timelineController.setAnimation(animation);
            rebuildPaintGrid();
            renderCurrentFrame();
        } catch (IOException e) {
            showAlert("Load failed", e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Playback
    // ----------------------------------------------------------------

    private void startPlayback() {
        if (animation.getFrameCount() == 0) return;
        playing       = true;
        playbackFrame = 0;
        playBtn.setText("⏹ Stop");

        scheduleNextFrame();
    }

    private void scheduleNextFrame() {
        Frame frame = animation.getFrame(playbackFrame);
        playbackTimeline = new Timeline(
                new KeyFrame(Duration.millis(frame.getDurationMs()), e -> {
                    renderFrame(frame);
                    playbackFrame++;

                    if (playbackFrame >= animation.getFrameCount()) {
                        if (loopCheck.isSelected()) {
                            playbackFrame = 0;
                            scheduleNextFrame();
                        } else {
                            stopPlayback();
                        }
                    } else {
                        scheduleNextFrame();
                    }
                })
        );
        playbackTimeline.play();
    }

    private void stopPlayback() {
        playing = false;
        playBtn.setText("▶ Play");
        if (playbackTimeline != null) playbackTimeline.stop();
        renderCurrentFrame();
    }

    // ----------------------------------------------------------------
    // Rendering
    // ----------------------------------------------------------------

    private void renderCurrentFrame() {
        renderFrame(currentFrame());
    }

    private void renderFrame(Frame frame) {
        renderer.render(frame.getCube());
        onCubeUpdated.accept(frame.getCube());
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private Frame currentFrame() {
        return animation.getFrame(
                timelineController.getSelectedIndex());
    }

    private Color toFxColor(LedColor c) {
        if (c.isOff()) return Color.web("#111");
        return Color.rgb(c.getR(), c.getG(), c.getB());
    }

    private LedColor fromFxColor(Color c) {
        return new LedColor(
                (int)(c.getRed()   * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue()  * 255)
        );
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public Node getView() { return root; }
}