package com.cubeapp.ui.games;

import com.cubeapp.games.xox.XoxGame;
import com.cubeapp.model.Cube;
import com.cubeapp.model.LedColor;
import com.cubeapp.ui.CubeRenderer;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.function.Consumer;

public class XoxController {

    @FXML private BorderPane       root;
    @FXML private Label            turnLabel;
    @FXML private Label            statusLabel;
    @FXML private Spinner<Integer> xSpinner;
    @FXML private Spinner<Integer> ySpinner;
    @FXML private Spinner<Integer> zSpinner;
    @FXML private Spinner<Integer> previewZSpinner;
    @FXML private GridPane         layerGrid;

    private CubeRenderer   renderer;
    private Consumer<Cube> onCubeUpdated;
    private Cube           cube;
    private XoxGame        game;

    private static final int CELL_SIZE = 48;

    public void init(CubeRenderer renderer, Consumer<Cube> onCubeUpdated) {
        this.renderer      = renderer;
        this.onCubeUpdated = onCubeUpdated;
        this.cube          = new Cube();
        this.game          = new XoxGame();
        setupSpinners();
        refreshLayerPreview();
        renderAndNotify();
    }

    @FXML
    private void onPlaceMark() {
        if (game.isGameOver()) return;
        int x = xSpinner.getValue();
        int y = ySpinner.getValue();
        int z = zSpinner.getValue();

        if (!game.makeMove(x, y, z)) {
            statusLabel.setText("⚠ Cell already taken!");
            return;
        }

        statusLabel.setText("");
        game.writeState(cube);
        renderAndNotify();
        refreshLayerPreview();
        updateTurnLabel();
    }

    @FXML
    private void onNewGame() {
        game.reset();
        game.writeState(cube);
        renderAndNotify();
        refreshLayerPreview();
        statusLabel.setText("");
        turnLabel.setText("Player 1's turn (Blue)");
        turnLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 13px;");
    }

    @FXML
    private void onPreviewZChanged() {
        refreshLayerPreview();
    }

    private void updateTurnLabel() {
        if (game.isGameOver()) {
            if (game.isDraw()) {
                statusLabel.setText("🤝 Draw!");
                turnLabel.setText("Game Over");
                turnLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");
            } else {
                boolean p1wins = game.getWinner() == XoxGame.X;
                statusLabel.setText("🏆 " + (p1wins ? "Player 1 (Blue)" : "Player 2 (Red)") + " wins!");
                turnLabel.setText("Game Over");
                turnLabel.setStyle("-fx-text-fill: " + (p1wins ? "#3498db" : "#e74c3c") + "; -fx-font-size: 13px;");
            }
            return;
        }
        if (game.getCurrentPlayer() == XoxGame.X) {
            turnLabel.setText("Player 1's turn (Blue)");
            turnLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 13px;");
        } else {
            turnLabel.setText("Player 2's turn (Red)");
            turnLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        }
    }

    private void refreshLayerPreview() {
        layerGrid.getChildren().clear();
        int previewZ = previewZSpinner.getValue();
        int selX = xSpinner.getValue();
        int selY = ySpinner.getValue();
        int selZ = zSpinner.getValue();

        for (int y = 3; y >= 0; y--) {
            for (int x = 0; x < 4; x++) {
                int cell = game.getCell(x, y, previewZ);

                Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
                rect.setArcWidth(6);
                rect.setArcHeight(6);
                rect.setFill(cell == XoxGame.X ? Color.web("#3498db")
                        : cell == XoxGame.O ? Color.web("#e74c3c")
                        : Color.web("#1e1e3a"));

                boolean isSelected = x == selX && y == selY && previewZ == selZ;
                rect.setStroke(isSelected ? Color.WHITE : Color.web("#333"));
                rect.setStrokeWidth(isSelected ? 2 : 1);

                Label coord = new Label(x + "," + y);
                coord.setStyle("-fx-text-fill: #555; -fx-font-size: 9px;");

                StackPane cell2 = new StackPane(rect, coord);
                layerGrid.add(cell2, x, 3 - y);
            }
        }
    }

    private void setupSpinners() {
        xSpinner.valueProperty().addListener((o, a, b) -> refreshLayerPreview());
        ySpinner.valueProperty().addListener((o, a, b) -> refreshLayerPreview());
        zSpinner.valueProperty().addListener((o, a, b) -> refreshLayerPreview());
    }

    private void renderAndNotify() {
        renderer.render(cube);
        onCubeUpdated.accept(cube);
    }

    public Node getView() { return root; }
}