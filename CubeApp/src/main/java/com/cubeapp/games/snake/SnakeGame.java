package com.cubeapp.games.snake;

import com.cubeapp.games.Game;
import com.cubeapp.model.Cube;
import com.cubeapp.model.LedColor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public class SnakeGame implements Game {

    // Colors
    private static final LedColor COLOR_HEAD  = LedColor.WHITE;
    private static final LedColor COLOR_BODY  = LedColor.GREEN;
    private static final LedColor COLOR_FOOD  = LedColor.RED;

    // Snake body: front = head, back = tail
    private final Deque<int[]> body = new ArrayDeque<>();

    private Direction currentDir;
    private Direction pendingDir;   // buffered input — applied on next tick

    private int[] food;             // [x, y, z]

    private boolean gameOver;
    private int score;

    private final Random random = new Random();

    public SnakeGame() {
        reset();
    }

    @Override
    public void reset() {
        body.clear();
        gameOver = false;
        score    = 0;

        // Start in the middle, 3 segments long, moving in X_POS
        body.addFirst(new int[]{2, 2, 2}); // head
        body.addLast (new int[]{1, 2, 2}); // body
        body.addLast (new int[]{0, 2, 2}); // tail

        currentDir = Direction.X_POS;
        pendingDir = Direction.X_POS;

        spawnFood();
    }

    @Override
    public void tick() {
        if (gameOver) return;

        // Apply buffered direction (ignore if opposite)
        if (!pendingDir.isOpposite(currentDir)) {
            currentDir = pendingDir;
        }

        // Compute new head position
        int[] head    = body.peekFirst();
        int   newX    = head[0] + currentDir.dx;
        int   newY    = head[1] + currentDir.dy;
        int   newZ    = head[2] + currentDir.dz;

        // Wall collision
        if (!inBounds(newX, newY, newZ)) {
            gameOver = true;
            return;
        }

        // Self collision
        if (occupies(newX, newY, newZ)) {
            gameOver = true;
            return;
        }

        // Move: add new head
        body.addFirst(new int[]{newX, newY, newZ});

        // Food eaten?
        if (newX == food[0] && newY == food[1] && newZ == food[2]) {
            score++;
            spawnFood(); // don't remove tail — snake grows
        } else {
            body.removeLast(); // remove tail — snake moves
        }
    }

    /**
     * Call this from the UI when the player presses a key.
     * Direction change is buffered and applied on next tick.
     */
    public void setDirection(Direction dir) {
        pendingDir = dir;
    }

    @Override
    public void writeState(Cube cube) {
        cube.clear();

        // Draw body
        boolean isHead = true;
        for (int[] seg : body) {
            cube.set(seg[0], seg[1], seg[2], isHead ? COLOR_HEAD : COLOR_BODY);
            isHead = false;
        }

        // Draw food
        cube.set(food[0], food[1], food[2], COLOR_FOOD);
    }

    @Override
    public boolean isGameOver() { return gameOver; }

    @Override
    public int getScore() { return score; }

    // --- Private helpers ---

    private void spawnFood() {
        int x, y, z;
        do {
            x = random.nextInt(Cube.SIZE);
            y = random.nextInt(Cube.SIZE);
            z = random.nextInt(Cube.SIZE);
        } while (occupies(x, y, z));
        food = new int[]{x, y, z};
    }

    private boolean occupies(int x, int y, int z) {
        for (int[] seg : body) {
            if (seg[0] == x && seg[1] == y && seg[2] == z) return true;
        }
        return false;
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < Cube.SIZE
                && y >= 0 && y < Cube.SIZE
                && z >= 0 && z < Cube.SIZE;
    }
}