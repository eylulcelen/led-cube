package com.cubeapp.protocol;

import com.cubeapp.model.Cube;
import com.cubeapp.model.LedColor;

public class RgbProtocol implements CubeProtocol {

    private static final byte START_MARKER   = (byte) 0xAB;
    private static final byte END_MARKER     = (byte) 0xCD;
    private static final byte CMD_DRAW_FRAME = 0x01;

    // 1 start + 1 cmd + (125*3) rgb + 1 checksum + 1 end = 379
    private static final int PACKET_SIZE = 379;

    @Override
    public byte[] encode(Cube cube) {
        byte[] packet = new byte[PACKET_SIZE];
        int i = 0;

        packet[i++] = START_MARKER;
        packet[i++] = CMD_DRAW_FRAME;

        byte checksum = 0;

        for (int z = 0; z < Cube.SIZE; z++) {
            for (int y = 0; y < Cube.SIZE; y++) {
                for (int x = 0; x < Cube.SIZE; x++) {
                    LedColor c = cube.get(x, y, z);
                    byte r = (byte) c.getR();
                    byte g = (byte) c.getG();
                    byte b = (byte) c.getB();

                    packet[i++] = r;
                    packet[i++] = g;
                    packet[i++] = b;

                    checksum ^= r;
                    checksum ^= g;
                    checksum ^= b;
                }
            }
        }

        packet[i++] = checksum;
        packet[i]   = END_MARKER;

        return packet;
    }
}