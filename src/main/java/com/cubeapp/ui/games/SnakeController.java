package com.cubeapp.ui.games;

import com.cubeapp.games.snake.Direction;
import com.cubeapp.games.snake.SnakeGame;
import com.cubeapp.model.Cube;
import com.cubeapp.ui.CubeRenderer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import java.util.function.Consumer;

public class SnakeController {

    @FXML private BorderPane root;
    @FXML private Label      scoreLabel;
    @FXML private Label      statusLabel;

    private CubeRenderer renderer;
    private Consumer<Cube> onCubeUpdated;
    private SnakeGame      game;
    private Timeline       timeline;
    private Cube           cube;
    private boolean        paused = false;

    private static final int TICK_MS = 400;

    public void init(CubeRenderer renderer, Consumer<Cube> onCubeUpdated) {
        this.renderer      = renderer;
        this.onCubeUpdated = onCubeUpdated;
        this.cube          = new Cube();
        this.game          = new SnakeGame();
        setupTimeline();

        // Set keyboard on scene — available after init() is called from MainController
        // which happens after FXML is loaded and root is injected
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> {
                    switch (e.getCode()) {
                        case W -> game.setDirection(Direction.Y_POS);
                        case S -> game.setDirection(Direction.Y_NEG);
                        case A -> game.setDirection(Direction.X_NEG);
                        case D -> game.setDirection(Direction.X_POS);
                        case Q -> game.setDirection(Direction.Z_POS);
                        case E -> game.setDirection(Direction.Z_NEG);
                        default -> {}
                    }
                });
            }
        });
    }

    public void startGame() {
        game.reset();
        paused = false;
        statusLabel.setText("");
        scoreLabel.setText("0");
        timeline.playFromStart();
        root.requestFocus();
    }

    @FXML private void onRestart() { startGame(); }

    @FXML
    private void onPause() {
        if (game.isGameOver()) return;
        if (paused) {
            timeline.play();
            statusLabel.setText("");
        } else {
            timeline.pause();
            statusLabel.setText("⏸ Paused");
        }
        paused = !paused;
    }

    @FXML private void onXPos() { game.setDirection(Direction.X_POS); }
    @FXML private void onXNeg() { game.setDirection(Direction.X_NEG); }
    @FXML private void onYPos() { game.setDirection(Direction.Y_POS); }
    @FXML private void onYNeg() { game.setDirection(Direction.Y_NEG); }
    @FXML private void onZPos() { game.setDirection(Direction.Z_POS); }
    @FXML private void onZNeg() { game.setDirection(Direction.Z_NEG); }

    private void setupTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.millis(TICK_MS), e -> tick()));
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
    }

    private void tick() {
        game.tick();
        game.writeState(cube);
        renderer.render(cube);
        onCubeUpdated.accept(cube);
        scoreLabel.setText(String.valueOf(game.getScore()));
        if (game.isGameOver()) {
            timeline.stop();
            statusLabel.setText("💀 Game Over! Score: " + game.getScore());
        }
    }

    public Node getView() { return root; }
}