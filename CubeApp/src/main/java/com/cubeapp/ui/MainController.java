package com.cubeapp.ui;

import com.cubeapp.comm.BluetoothManager;
import com.cubeapp.comm.CubeConnection;
import com.cubeapp.model.Cube;
import com.cubeapp.protocol.RgbProtocol;
import com.cubeapp.ui.editor.AnimationEditorController;
import com.cubeapp.ui.games.SnakeController;
import com.cubeapp.ui.games.XoxController;
import com.fazecast.jSerialComm.SerialPort;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML private Canvas           cubeCanvas;
    @FXML private ComboBox<String> portCombo;
    @FXML private Button           connectBtn;
    @FXML private Label            statusLabel;
    @FXML private Button           sendBtn;

    // The actual root BorderPane — injected via initialize(), not @FXML
    private BorderPane mainRoot;

    private CubeRenderer   renderer;
    private Cube           activeCube;
    private CubeConnection connection;
    private BluetoothManager btManager;

    private AnimationEditorController animEditor;
    private SnakeController snakeCtrl;
    private XoxController xoxCtrl;

    @FXML
    public void initialize() {
        renderer   = new CubeRenderer(cubeCanvas);
        activeCube = new Cube();
        btManager  = new BluetoothManager();
        renderer.render(activeCube);
        populatePorts();

        // Grab root after scene is ready using listener
        cubeCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                mainRoot = (BorderPane) newScene.getRoot();
            }
        });
    }

    // ----------------------------------------------------------------
    // Connection
    // ----------------------------------------------------------------

    private void populatePorts() {
        portCombo.getItems().clear();
        for (SerialPort p : btManager.getAvailablePorts()) {
            portCombo.getItems().add(
                    p.getSystemPortName() + " — " + p.getDescriptivePortName());
        }
        List<SerialPort> candidates = btManager.findHC05Candidates();
        if (!candidates.isEmpty()) {
            SerialPort c = candidates.get(0);
            portCombo.setValue(
                    c.getSystemPortName() + " — " + c.getDescriptivePortName());
        }
        if (portCombo.getItems().isEmpty())
            portCombo.setPromptText("No ports found");
    }

    @FXML
    private void onConnectClicked() {
        if (connection != null && connection.isConnected()) {
            connection.disconnect();
            connection = null;
            connectBtn.setText("Connect");
            setStatus("● Disconnected", false);
            sendBtn.setDisable(true);
            return;
        }

        String selected = portCombo.getValue();
        if (selected == null) { showAlert("No port selected", "Please select a COM port first."); return; }

        String portName = selected.split(" — ")[0].trim();
        SerialPort target = btManager.getAvailablePorts().stream()
                .filter(p -> p.getSystemPortName().equals(portName))
                .findFirst().orElse(null);

        if (target == null) { showAlert("Port not found", "Could not find: " + portName); return; }

        try {
            connection = new CubeConnection(btManager.openPort(target), new RgbProtocol());
            connectBtn.setText("Disconnect");
            setStatus("● Connected — " + portName, true);
            sendBtn.setDisable(false);
        } catch (Exception e) {
            showAlert("Connection failed", e.getMessage());
        }
    }

    @FXML
    private void onSendClicked() {
        if (connection == null || !connection.isConnected()) return;
        if (!connection.trySendFrame(activeCube))
            setStatus("● Send failed", false);
    }

    // ----------------------------------------------------------------
    // Mode switching
    // ----------------------------------------------------------------

    @FXML
    private void onAnimationEditorClicked() {
        try {
            if (animEditor == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/cubeapp/editor/animation_editor.fxml"));
                loader.load();
                animEditor = loader.getController();
                animEditor.init(renderer, this::setActiveCube);
            }
            mainRoot.setCenter(animEditor.getView());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSnakeClicked() {
        try {
            if (snakeCtrl == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/cubeapp/games/snake_view.fxml"));
                loader.load();
                snakeCtrl = loader.getController();
                snakeCtrl.init(renderer, this::setActiveCube);
            }
            mainRoot.setCenter(snakeCtrl.getView());
            snakeCtrl.startGame();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onXoxClicked() {
        try {
            if (xoxCtrl == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/cubeapp/games/xox_view.fxml"));
                loader.load();
                xoxCtrl = loader.getController();
                xoxCtrl.init(renderer, this::setActiveCube);
            }
            mainRoot.setCenter(xoxCtrl.getView());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    public void setActiveCube(Cube cube) { this.activeCube = cube; }

    private void setStatus(String text, boolean good) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: " + (good ? "#2ecc71" : "#e74c3c") + "; -fx-font-size: 11px;");
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}