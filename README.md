# How to Use the App

## First Launch

Open the project in IntelliJ IDEA and run it using either:

```bash
mvn javafx:run
```

or by right-clicking `Main.java` and selecting **Run**.

When the application starts, a dark-themed window appears with a navigation sidebar on the left and a cube renderer in the center.

---

## Connecting to the Cube

Before connecting, pair the HC-05 Bluetooth module with your computer through Windows Bluetooth settings.

**Default HC-05 PIN:** `1234`

After pairing, Windows creates two new COM ports.

1. Open the application.
2. Click the **Port** dropdown menu.
3. Look for a port containing **Bluetooth** or **HC-05** in its name.
4. If two ports appear, try the higher-numbered one first.
5. Click **Connect**.

When the connection succeeds:

* The status label turns green.
* The selected port name is displayed.
* The **Send to Cube** button becomes enabled.

If the automatically detected port is incorrect, disconnect, select another COM port from the dropdown list, and try again.

---

# Animation Editor

Select **Animation Editor** from the sidebar.

The editor contains:

* A 5×5 paint grid
* A toolbar at the top
* A tool panel on the right
* A frame strip at the bottom

## Editing LEDs

The paint grid displays one Z-layer at a time.

Use the **Z Layer** spinner to switch between layers:

* 0 = Bottom layer
* 4 = Top layer

Controls:

* **Left Click:** Paint with selected color
* **Right Click:** Erase LED

The cube preview updates in real time as you paint.

## Working with Frames

### Add a Frame

Click the green **+** button.

The new frame is created as a copy of the currently selected frame.

### Delete a Frame

Click the red **−** button.

### Reorder Frames

Use:

* **◀** Move Left
* **▶** Move Right

Each frame displays:

* Frame number
* Duration

Click a frame to select and edit it.

## Frame Duration

Use the duration spinner in the right panel to specify how long a frame should remain visible during playback and on the physical cube.

## Playback

### Play

Click **Play** to preview the animation.

The editor plays all frames using their configured durations.

### Stop

Click **Stop** to return to edit mode.

## Saving and Loading

### Save

Click **Save** to export the animation as a JSON file.

### Load

Click **Load** to import a previously saved animation.

The JSON file stores every LED color for every frame, ensuring no information is lost.

## Sending Frames to the Cube

When connected:

1. Select a frame.
2. Click **Send to Cube**.

The currently visible frame is transmitted immediately to the physical cube.

**Note:** Continuous animation streaming is not currently automatic. To display an animation on the cube, frames must be sent manually. Automatic streaming is planned as a future improvement.

---

# Snake

Select **Snake** from the sidebar.

The game starts immediately.

### Colors

* White LED → Snake Head
* Green LEDs → Snake Body
* Red LED → Food

### Controls

#### Keyboard

| Key | Action         |
| --- | -------------- |
| W   | Forward        |
| S   | Backward       |
| A   | Left           |
| D   | Right          |
| Q   | Move Up (Z+)   |
| E   | Move Down (Z−) |

Because the cube is three-dimensional, the snake can move in six directions.

### Rules

The snake dies if it:

* Hits a wall
* Hits its own body

The score increases whenever food is eaten.

### Buttons

* **Restart** → Start a new game
* **Pause** → Pause or resume gameplay

When connected, the snake state is also transmitted to the physical cube, allowing gameplay directly on the LED cube.

---

# XOX (3D Tic-Tac-Toe)

Select **XOX** from the sidebar.

### Players

* Player 1 → Blue
* Player 2 → Red

### Board Size

4 × 4 × 4

### Playing

1. Choose coordinates using the:

   * X spinner
   * Y spinner
   * Z spinner
2. Review the selected layer in the preview grid.
3. The selected cell is highlighted with a white border.
4. Click **Place Mark**.

### Winning

Place four marks in a straight line.

Valid winning lines include:

* Horizontal rows
* Vertical columns
* Z-axis pillars
* Face diagonals
* Space diagonals

There are **76 possible winning combinations** in a 4×4×4 cube.

When a player wins:

* The four winning cells illuminate white.

### New Game

Click **New Game** to reset the board.

---

# Hour Display

Select **Hour Display** from the sidebar.

The cube displays the current hour as two digits on the **x = 0** face.

### Layout

* Tens digit → Left side (y = 0–2)
* Units digit → Right side (y = 3–4)

Digits are drawn vertically from:

* z = 4 (top)
* z = 0 (bottom)

### Features

* Gentle pulse animation
* Dim blink every few seconds
* Automatic update at the beginning of each hour

Click **Hour Display** again to stop the feature.

---

# Sensor Features

When the Arduino is connected and transmitting sensor data, the sensor panel displays four live values.

## LDR

Raw light sensor reading.

Range:

```
0 – 1023
```

* High value → Bright environment
* Low value → Dark environment

## Temperature

Current temperature measured by the DHT11 sensor.

Unit:

```
°C
```

## Humidity

Current relative humidity.

Unit:

```
%
```

## Brightness

Calculated brightness percentage applied to outgoing cube frames.

---

## Adaptive Brightness

Enable the **Adaptive Brightness** checkbox.

Behavior:

* Bright room → 100% brightness
* Dark room → Approximately 15% brightness

Brightness adjustments affect only data sent to the cube.

The editor preview always remains at full brightness.

---

## Temperature Tint

Enable the **Temperature Tint** checkbox.

Color shifts are applied according to temperature:

* Cold → Blue tint
* Warm → Yellow tint
* Hot → Red tint

Like Adaptive Brightness, this filter affects only transmitted data and does not modify the on-screen preview.

Both filters can be enabled or disabled at any time, including during animations and gameplay.

---

# Data Flow Summary

The following sequence describes how a frame travels from the editor to the physical LED cube.

1. The user clicks a cell in the paint grid.
2. `AnimationEditorController.onCellClicked()` updates the LED color inside the current frame's `Cube`.
3. `CubeRenderer.render()` redraws the cube on the JavaFX canvas.
4. `MainController.setActiveCube()` stores the updated cube as the active frame.
5. The user clicks **Send to Cube**.
6. `MainController.onSendClicked()` executes `applyFilters()`.
7. `TemperatureColorFilter` and `BrightnessFilter` generate a filtered cube.
8. `CubeConnection.trySendFrame()` calls `RgbProtocol.encode()`.
9. The cube is serialized into a 379-byte packet.
10. jSerialComm writes the packet to the selected Bluetooth COM port.
11. Windows transmits the data through Bluetooth to the HC-05 module.
12. The Arduino detects the start byte (`0xAB`) and executes `receiveFrame()`.
13. The Arduino reads 379 bytes, verifies the checksum, and validates the end marker.
14. If valid, the 375 RGB bytes are unpacked into the `leds[125][3]` array.
15. `drawCube()` updates the physical LED cube with the new colors.

This process repeats every time a new frame is transmitted.
