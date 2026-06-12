package com.cubeapp.ui;

import com.cubeapp.comm.BluetoothManager;
import com.cubeapp.comm.CubeConnection;
import com.cubeapp.comm.SensorDataReader;
import com.cubeapp.model.Cube;
import com.cubeapp.model.LedColor;
import com.cubeapp.model.SensorData;
import com.cubeapp.protocol.RgbProtocol;
import com.cubeapp.ui.editor.AnimationEditorController;
import com.cubeapp.ui.games.SnakeController;
import com.cubeapp.ui.games.XoxController;
import com.cubeapp.util.BrightnessFilter;
import com.cubeapp.util.HourAnimationGenerator;
import com.cubeapp.util.TemperatureColorFilter;
import com.fazecast.jSerialComm.SerialPort;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

public class MainController {

    // --- FXML ---
    @FXML private Canvas           cubeCanvas;
    @FXML private ComboBox<String> portCombo;
    @FXML private Button           connectBtn;
    @FXML private Label            statusLabel;
    @FXML private Button           sendBtn;
    @FXML private Label            ldrLabel;
    @FXML private Label            tempLabel;
    @FXML private Label            humLabel;
    @FXML private Label            brightnessLabel;
    @FXML private CheckBox         adaptiveBrightnessCheck;
    @FXML private CheckBox         tempColorCheck;

    // --- App state ---
    private BorderPane   mainRoot;
    private CubeRenderer renderer;
    private Cube         activeCube;
    private CubeConnection   connection;
    private BluetoothManager btManager;
    private SensorData       sensorData;
    private SensorDataReader sensorReader;

    // Mode controllers
    private AnimationEditorController animEditor;
    private SnakeController snakeCtrl;
    private XoxController xoxCtrl;

    // Hour display
    private Timeline hourTimeline;
    private boolean  hourModeActive = false;
    private int      hourFrameIndex = 0;
    private com.cubeapp.model.Animation hourAnimation;

    @FXML
    public void initialize() {
        renderer   = new CubeRenderer(cubeCanvas);
        activeCube = new Cube();
        btManager  = new BluetoothManager();
        sensorData = new SensorData();

        renderer.render(activeCube);
        populatePorts();
        bindSensorLabels();

        // Grab mainRoot once scene is attached
        cubeCanvas.sceneProperty().addListener((obs, o, newScene) -> {
            if (newScene != null)
                mainRoot = (BorderPane) newScene.getRoot();
        });
    }

    // ----------------------------------------------------------------
    // Sensor label bindings
    // ----------------------------------------------------------------

    private void bindSensorLabels() {
        sensorData.ldrValueProperty().addListener((obs, o, n) ->
                ldrLabel.setText(String.valueOf(n.intValue())));

        sensorData.temperatureProperty().addListener((obs, o, n) ->
                tempLabel.setText(String.format("%.1f°C", n.doubleValue())));

        sensorData.humidityProperty().addListener((obs, o, n) ->
                humLabel.setText(String.format("%.0f%%", n.doubleValue())));

        sensorData.brightnessProperty().addListener((obs, o, n) ->
                brightnessLabel.setText(String.format("%.0f%%",
                        n.doubleValue() * 100)));
    }

    // ----------------------------------------------------------------
    // Connection
    // ----------------------------------------------------------------

    private void populatePorts() {
        portCombo.getItems().clear();
        for (SerialPort p : btManager.getAvailablePorts())
            portCombo.getItems().add(
                    p.getSystemPortName() + " — " + p.getDescriptivePortName());

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
            stopSensorReader();
            connection.disconnect();
            connection = null;
            connectBtn.setText("Connect");
            setStatus("● Disconnected", false);
            sendBtn.setDisable(true);
            return;
        }

        String selected = portCombo.getValue();
        if (selected == null) {
            showAlert("No port selected", "Please select a COM port.");
            return;
        }

        String portName = selected.split(" — ")[0].trim();
        SerialPort target = btManager.getAvailablePorts().stream()
                .filter(p -> p.getSystemPortName().equals(portName))
                .findFirst().orElse(null);

        if (target == null) {
            showAlert("Port not found", "Could not find: " + portName);
            return;
        }

        try {
            SerialPort open = btManager.openPort(target);
            connection   = new CubeConnection(open, new RgbProtocol());
            connectBtn.setText("Disconnect");
            setStatus("● Connected — " + portName, true);
            sendBtn.setDisable(false);
            startSensorReader(open);
        } catch (Exception e) {
            showAlert("Connection failed", e.getMessage());
        }
    }

    private void startSensorReader(SerialPort port) {
        sensorReader = new SensorDataReader(port, sensorData);
        sensorReader.start();
    }

    private void stopSensorReader() {
        if (sensorReader != null) {
            sensorReader.stop();
            sensorReader = null;
        }
    }

    // ----------------------------------------------------------------
    // Send — applies filters before sending
    // ----------------------------------------------------------------

    @FXML
    private void onSendClicked() {
        if (connection == null || !connection.isConnected()) return;
        Cube toSend = applyFilters(activeCube);
        if (!connection.trySendFrame(toSend))
            setStatus("● Send failed", false);
    }

    /**
     * Applies enabled filters to a cube copy before sending.
     * Never modifies the original.
     */
    private Cube applyFilters(Cube source) {
        Cube result = source;

        if (tempColorCheck.isSelected())
            result = TemperatureColorFilter.apply(result,
                    sensorData.getTemperature());

        if (adaptiveBrightnessCheck.isSelected())
            result = BrightnessFilter.apply(result,
                    sensorData.getBrightness());

        return result;
    }

    // ----------------------------------------------------------------
    // Hour Display mode
    // ----------------------------------------------------------------

    @FXML
    private void onHourDisplayClicked() {
        if (hourModeActive) {
            stopHourMode();
            return;
        }
        startHourMode();
    }

    private void startHourMode() {
        hourModeActive = true;
        hourFrameIndex = 0;
        hourAnimation  = HourAnimationGenerator.generate();

        // Refresh animation every full hour
        hourTimeline = new Timeline(
                new KeyFrame(Duration.millis(getNextFrameDuration()), e -> hourTick())
        );
        hourTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        hourTimeline.play();

        // Show in center
        if (mainRoot != null)
            mainRoot.setCenter(null); // clear any mode view, canvas shows through
    }

    private void stopHourMode() {
        hourModeActive = false;
        if (hourTimeline != null) hourTimeline.stop();
    }

    private void hourTick() {
        if (hourAnimation == null || hourAnimation.getFrameCount() == 0) return;

        // Regenerate every hour (frame 0 of a new cycle)
        if (hourFrameIndex == 0)
            hourAnimation = HourAnimationGenerator.generate();

        com.cubeapp.model.Frame frame =
                hourAnimation.getFrame(hourFrameIndex);

        activeCube = frame.getCube();
        Cube toRender = applyFilters(activeCube);
        renderer.render(toRender);

        if (connection != null && connection.isConnected())
            connection.trySendFrame(applyFilters(activeCube));

        hourFrameIndex =
                (hourFrameIndex + 1) % hourAnimation.getFrameCount();

        // Reschedule with next frame's duration
        hourTimeline.stop();
        hourTimeline = new Timeline(
                new KeyFrame(Duration.millis(getNextFrameDuration()), ev -> hourTick())
        );
        hourTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        hourTimeline.play();
    }

    private double getNextFrameDuration() {
        if (hourAnimation == null || hourAnimation.getFrameCount() == 0)
            return 1000;
        return hourAnimation.getFrame(hourFrameIndex).getDurationMs();
    }

    // ----------------------------------------------------------------
    // Mode switching
    // ----------------------------------------------------------------

    @FXML
    private void onAnimationEditorClicked() {
        stopHourMode();
        try {
            if (animEditor == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/cubeapp/editor/animation_editor.fxml"));
                loader.load();
                animEditor = loader.getController();
                animEditor.init(renderer, this::setActiveCube);
            }
            mainRoot.setCenter(animEditor.getView());
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void onSnakeClicked() {
        stopHourMode();
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
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void onXoxClicked() {
        stopHourMode();
        try {
            if (xoxCtrl == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/cubeapp/games/xox_view.fxml"));
                loader.load();
                xoxCtrl = loader.getController();
                xoxCtrl.init(renderer, this::setActiveCube);
            }
            mainRoot.setCenter(xoxCtrl.getView());
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    public void setActiveCube(Cube cube) { this.activeCube = cube; }

    private void setStatus(String text, boolean good) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-text-fill: "
                + (good ? "#2ecc71" : "#e74c3c")
                + "; -fx-font-size: 11px;");
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}