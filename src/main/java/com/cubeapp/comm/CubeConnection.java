package com.cubeapp.comm;

import com.cubeapp.model.Cube;
import com.cubeapp.protocol.CubeProtocol;
import com.fazecast.jSerialComm.SerialPort;

/**
 * Represents an active connection to the cube.
 * Holds the open SerialPort and uses a CubeProtocol to encode frames.
 */
public class CubeConnection {

    private final SerialPort port;
    private final CubeProtocol protocol;
    private boolean connected;

    public CubeConnection(SerialPort port, CubeProtocol protocol) {
        this.port      = port;
        this.protocol  = protocol;
        this.connected = true;
    }

    /**
     * Encodes the cube state and sends it to the Arduino.
     */
    public void sendFrame(Cube cube) {
        if (!connected) throw new IllegalStateException("Not connected");

        byte[] packet = protocol.encode(cube);
        int written = port.writeBytes(packet, packet.length);

        if (written != packet.length) {
            throw new RuntimeException(
                    "Send incomplete: wrote " + written + "/" + packet.length + " bytes"
            );
        }
    }

    /**
     * Sends a frame and swallows errors — useful for animation loops
     * where a single dropped frame is acceptable.
     */
    public boolean trySendFrame(Cube cube) {
        try {
            sendFrame(cube);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isConnected() { return connected && port.isOpen(); }

    public String getPortName() { return port.getSystemPortName(); }

    public void disconnect() {
        connected = false;
        port.closePort();
    }
}