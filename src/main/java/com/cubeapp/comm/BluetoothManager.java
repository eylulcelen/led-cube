package com.cubeapp.comm;

import com.fazecast.jSerialComm.SerialPort;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scans system COM ports and identifies HC-05 candidates.
 */
public class BluetoothManager {

    /**
     * Returns all available COM ports on the system.
     */
    public List<SerialPort> getAvailablePorts() {
        return Arrays.asList(SerialPort.getCommPorts());
    }

    /**
     * Returns ports that are likely the HC-05.
     * HC-05 paired via Bluetooth shows up as "Dev-B" or contains "HC-05" on most systems.
     * User can override and pick manually from getAvailablePorts().
     */
    public List<SerialPort> findHC05Candidates() {
        return getAvailablePorts().stream()
                .filter(p -> {
                    String desc = p.getDescriptivePortName().toLowerCase();
                    String name = p.getSystemPortName().toLowerCase();
                    return desc.contains("hc-05")
                            || desc.contains("hc05")
                            || desc.contains("dev-b")
                            || desc.contains("bluetooth")
                            || name.contains("rfcomm");
                })
                .collect(Collectors.toList());
    }

    /**
     * Opens a port at 9600 baud (must match Arduino firmware).
     * Returns the open port, or throws if it fails.
     */
    public SerialPort openPort(SerialPort port) {
        port.setComPortParameters(
                9600,           // baud — must match bt.begin(9600) in Arduino
                8,              // data bits
                SerialPort.ONE_STOP_BIT,
                SerialPort.NO_PARITY
        );
        port.setComPortTimeouts(
                SerialPort.TIMEOUT_WRITE_BLOCKING,
                0,    // read timeout  (we don't read from Java side)
                2000  // write timeout (ms)
        );

        if (!port.openPort()) {
            throw new RuntimeException("Failed to open port: " + port.getSystemPortName());
        }

        return port;
    }

    /**
     * Closes the port safely.
     */
    public void closePort(SerialPort port) {
        if (port != null && port.isOpen()) {
            port.closePort();
        }
    }
}