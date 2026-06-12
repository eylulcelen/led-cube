package com.cubeapp.ui;

import com.cubeapp.model.Cube;
import com.cubeapp.model.LedColor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws a Cube onto a JavaFX Canvas using isometric projection.
 * Call render(cube) any time the cube state changes.
 */

public class CubeRenderer {

    // --- Tunable constants ---
    private static final double CELL_W  = 40; // width of one cell (x-axis spread)
    private static final double CELL_H  = 24; // height of one cell (z-axis rise)
    private static final double GAP     = 10;  // gap between LEDs

    private final Canvas canvas;

    // Isometric origin — where (0,0,0) maps to on screen
    private final double originX;
    private final double originY;

    public CubeRenderer(Canvas canvas) {
        this.canvas  = canvas;
        // Center the cube horizontally; push it down a bit vertically
        this.originX = canvas.getWidth()  / 2.0;
        this.originY = canvas.getHeight() * 0.72;
    }

    /** Call this whenever cube state changes */
    public void render(Cube cube) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw back-to-front, bottom-to-top so front LEDs paint over back ones
        for (int z = 0; z < Cube.SIZE; z++)
            for (int y = Cube.SIZE - 1; y >= 0; y--)
                for (int x = 0; x < Cube.SIZE; x++)
                    drawLed(gc, x, y, z, cube.get(x, y, z));
    }

    private void drawLed(GraphicsContext gc, int x, int y, int z, LedColor color) {
        double cx = originX + (x - y) * (CELL_W / 2.0 + GAP);
        double cy = originY - z * (CELL_H + GAP) + (x + y) * (CELL_H / 2.0 + GAP / 2.0);

        double hw = CELL_W / 2.0; // half width
        double hh = CELL_H / 2.0; // half height

        if (color.isOff()) {
            drawFaces(gc, cx, cy, hw, hh,
                    Color.rgb(20, 20, 30),   // top  — dark
                    Color.rgb(12, 12, 20),   // left — darker
                    Color.rgb(15, 15, 25));  // right
            return;
        }

        Color base  = toFxColor(color);
        Color top   = base;
        Color left  = base.darker();
        Color right = base.darker().darker();

        drawFaces(gc, cx, cy, hw, hh, top, left, right);
    }

    private void drawFaces(GraphicsContext gc,
                           double cx, double cy,
                           double hw, double hh,
                           Color top, Color left, Color right) {
        // --- Top face (diamond) ---
        gc.setFill(top);
        gc.fillPolygon(
                new double[]{ cx,      cx + hw, cx,      cx - hw },
                new double[]{ cy - hh, cy,      cy + hh, cy      },
                4
        );

        // --- Left face ---
        gc.setFill(left);
        gc.fillPolygon(
                new double[]{ cx - hw, cx,      cx,      cx - hw },
                new double[]{ cy,      cy + hh, cy + hh * 2, cy + hh },
                4
        );

        // --- Right face ---
        gc.setFill(right);
        gc.fillPolygon(
                new double[]{ cx,      cx + hw, cx + hw, cx      },
                new double[]{ cy + hh, cy,      cy + hh, cy + hh * 2 },
                4
        );

        // --- Subtle outline ---
        gc.setStroke(Color.rgb(0, 0, 0, 0.25));
        gc.setLineWidth(0.5);
        gc.strokePolygon(
                new double[]{ cx,      cx + hw, cx,      cx - hw },
                new double[]{ cy - hh, cy,      cy + hh, cy      },
                4
        );
    }

    private Color toFxColor(LedColor c) {
        return Color.rgb(c.getR(), c.getG(), c.getB());
    }
}