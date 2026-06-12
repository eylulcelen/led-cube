package com.cubeapp.util;

import com.cubeapp.model.Cube;
import com.cubeapp.model.LedColor;
import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Teaches Gson how to read/write Cube as a flat array of 375 RGB values.
 * Format: [ r,g,b, r,g,b, ... ] — 375 ints, order: x→y→z
 */
public class CubeAdapter
        implements JsonSerializer<Cube>, JsonDeserializer<Cube> {

    @Override
    public JsonElement serialize(Cube src, Type type, JsonSerializationContext ctx) {
        JsonArray arr = new JsonArray();
        for (int z = 0; z < Cube.SIZE; z++)
            for (int y = 0; y < Cube.SIZE; y++)
                for (int x = 0; x < Cube.SIZE; x++) {
                    LedColor c = src.get(x, y, z);
                    arr.add(c.getR());
                    arr.add(c.getG());
                    arr.add(c.getB());
                }
        return arr;
    }

    @Override
    public Cube deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
            throws JsonParseException {
        JsonArray arr  = json.getAsJsonArray();
        Cube      cube = new Cube();
        int       i    = 0;
        for (int z = 0; z < Cube.SIZE; z++)
            for (int y = 0; y < Cube.SIZE; y++)
                for (int x = 0; x < Cube.SIZE; x++) {
                    int r = arr.get(i++).getAsInt();
                    int g = arr.get(i++).getAsInt();
                    int b = arr.get(i++).getAsInt();
                    cube.set(x, y, z, new LedColor(r, g, b));
                }
        return cube;
    }
}