package com.cubeapp.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * Live sensor readings — updated by SensorDataReader,
 * observed by UI and filters.
 * Uses JavaFX properties so UI labels can bind directly.
 */
public class SensorData {

    // LDR raw value 0–1023 (Arduino analogRead)
    private final DoubleProperty ldrValue = new SimpleDoubleProperty(512);

    // Temperature in Celsius
    private final DoubleProperty temperature = new SimpleDoubleProperty(20.0);

    // Humidity percent
    private final DoubleProperty humidity = new SimpleDoubleProperty(50.0);

    // Derived brightness 0.0–1.0 from LDR
    private final DoubleProperty brightness = new SimpleDoubleProperty(1.0);

    // --- LDR ---
    public double getLdrValue()                  { return ldrValue.get(); }
    public void   setLdrValue(double v)          {
        ldrValue.set(v);
        // Map LDR 0-1023 → brightness 0.15-1.0
        // Low LDR = dark room = dim cube
        double b = 0.15 + (v / 1023.0) * 0.85;
        brightness.set(Math.max(0.15, Math.min(1.0, b)));
    }
    public DoubleProperty ldrValueProperty()     { return ldrValue; }

    // --- Temperature ---
    public double getTemperature()               { return temperature.get(); }
    public void   setTemperature(double v)       { temperature.set(v); }
    public DoubleProperty temperatureProperty()  { return temperature; }

    // --- Humidity ---
    public double getHumidity()                  { return humidity.get(); }
    public void   setHumidity(double v)          { humidity.set(v); }
    public DoubleProperty humidityProperty()     { return humidity; }

    // --- Brightness ---
    public double getBrightness()                { return brightness.get(); }
    public DoubleProperty brightnessProperty()   { return brightness; }
}