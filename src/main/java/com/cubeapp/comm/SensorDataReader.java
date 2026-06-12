package com.cubeapp.comm;

import com.cubeapp.model.SensorData;
import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Runs on a background thread.
 * Reads "LDR:450,TEMP:23.5,HUM:60.0\n" lines from Arduino
 * and updates SensorData on the JavaFX thread.
 */
public class SensorDataReader {

    private final SerialPort port;
    private final SensorData sensorData;
    private Thread           readerThread;
    private volatile boolean running = false;

    public SensorDataReader(SerialPort port, SensorData sensorData) {
        this.port       = port;
        this.sensorData = sensorData;
    }

    public void start() {
        running      = true;
        readerThread = new Thread(this::readLoop, "sensor-reader");
        readerThread.setDaemon(true); // dies when app closes
        readerThread.start();
    }

    public void stop() {
        running = false;
        if (readerThread != null) readerThread.interrupt();
    }

    private void readLoop() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(port.getInputStream()))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                parseLine(line.trim());
            }
        } catch (Exception e) {
            if (running) System.err.println("SensorDataReader error: " + e.getMessage());
        }
    }

    /**
     * Parses: "LDR:450,TEMP:23.5,HUM:60.0"
     * Fields are optional — handles partial lines gracefully.
     */
    private void parseLine(String line) {
        if (line.isEmpty()) return;

        double ldr  = sensorData.getLdrValue();
        double temp = sensorData.getTemperature();
        double hum  = sensorData.getHumidity();

        try {
            for (String part : line.split(",")) {
                String[] kv = part.split(":");
                if (kv.length != 2) continue;
                double val = Double.parseDouble(kv[1].trim());
                switch (kv[0].trim().toUpperCase()) {
                    case "LDR"  -> ldr  = val;
                    case "TEMP" -> temp = val;
                    case "HUM"  -> hum  = val;
                }
            }
        } catch (NumberFormatException e) {
            return; // skip malformed line
        }

        final double fLdr = ldr, fTemp = temp, fHum = hum;
        Platform.runLater(() -> {
            sensorData.setLdrValue(fLdr);
            sensorData.setTemperature(fTemp);
            sensorData.setHumidity(fHum);
        });
    }
}