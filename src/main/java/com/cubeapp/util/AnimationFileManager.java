package com.cubeapp.util;

import com.cubeapp.model.Animation;
import com.cubeapp.model.Cube;
import com.cubeapp.model.Frame;
import com.cubeapp.model.LedColor;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Saves and loads Animation objects as .json files.
 *
 * File format:
 * {
 *   "name": "MyAnim",
 *   "looping": true,
 *   "frames": [
 *     { "durationMs": 100, "cube": [r,g,b, r,g,b, ...] },
 *     ...
 *   ]
 * }
 */
public class AnimationFileManager {

    private final Gson gson;

    public AnimationFileManager() {
        gson = new GsonBuilder()
                .registerTypeAdapter(LedColor.class, new LedColorAdapter())
                .registerTypeAdapter(Cube.class,     new CubeAdapter())
                .setPrettyPrinting()
                .create();
    }

    // ----------------------------------------------------------------
    // Save
    // ----------------------------------------------------------------

    public void save(Animation animation, Path path) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("name",    animation.getName());
        root.addProperty("looping", animation.isLooping());

        JsonArray framesArr = new JsonArray();
        for (Frame frame : animation.getFrames()) {
            JsonObject frameObj = new JsonObject();
            frameObj.addProperty("durationMs", frame.getDurationMs());
            frameObj.add("cube", gson.toJsonTree(frame.getCube(), Cube.class));
            framesArr.add(frameObj);
        }
        root.add("frames", framesArr);

        String json = gson.toJson(root);
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    // ----------------------------------------------------------------
    // Load
    // ----------------------------------------------------------------

    public Animation load(Path path) throws IOException {
        String    json = Files.readString(path, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        String    name    = root.get("name").getAsString();
        boolean   looping = root.get("looping").getAsBoolean();

        Animation animation = new Animation(name);
        animation.setLooping(looping);

        for (JsonElement el : root.getAsJsonArray("frames")) {
            JsonObject frameObj   = el.getAsJsonObject();
            int        durationMs = frameObj.get("durationMs").getAsInt();
            Cube       cube       = gson.fromJson(frameObj.get("cube"), Cube.class);

            Frame frame = new Frame(durationMs);
            // Copy cube data into frame's cube
            for (int z = 0; z < Cube.SIZE; z++)
                for (int y = 0; y < Cube.SIZE; y++)
                    for (int x = 0; x < Cube.SIZE; x++)
                        frame.getCube().set(x, y, z, cube.get(x, y, z));

            animation.addFrame(frame);
        }

        return animation;
    }

    // ----------------------------------------------------------------
    // Convenience overloads for File/String paths
    // ----------------------------------------------------------------

    public void save(Animation animation, File file) throws IOException {
        save(animation, file.toPath());
    }

    public Animation load(File file) throws IOException {
        return load(file.toPath());
    }
}