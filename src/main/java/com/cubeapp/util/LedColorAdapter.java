package com.cubeapp.util;

import com.cubeapp.model.LedColor;
import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Teaches Gson how to read/write LedColor as {r, g, b}.
 */
public class LedColorAdapter
        implements JsonSerializer<LedColor>, JsonDeserializer<LedColor> {

    @Override
    public JsonElement serialize(LedColor src, Type type, JsonSerializationContext ctx) {
        JsonObject obj = new JsonObject();
        obj.addProperty("r", src.getR());
        obj.addProperty("g", src.getG());
        obj.addProperty("b", src.getB());
        return obj;
    }

    @Override
    public LedColor deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        return new LedColor(
                obj.get("r").getAsInt(),
                obj.get("g").getAsInt(),
                obj.get("b").getAsInt()
        );
    }
}