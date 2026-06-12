package com.cubeapp.protocol;

import com.cubeapp.model.Cube;

/**
 * Converts a Cube state into a byte array ready to send to Arduino.
 */
public interface CubeProtocol {
    byte[] encode(Cube cube);
}