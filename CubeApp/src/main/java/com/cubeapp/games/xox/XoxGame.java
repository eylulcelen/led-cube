package com.cubeapp.games.xox;

import com.cubeapp.games.Game;
import com.cubeapp.model.Cube;
import com.cubeapp.model.LedColor;

/**
 * 3D Tic-Tac-Toe on a 4x4x4 grid (played within the 5x5x5 cube).
 * Win by placing 4 in a row in any of 13 directions.
 * Player 1 = X (Blue), Player 2 = O (Red).
 */
public class XoxGame implements Game {

    public static final int GRID = 4;   // 4x4x4 grid
    public static final int OFFSET = 0; // grid starts at cube index 0

    public static final int EMPTY = 0;
    public static final int X     = 1;  // Player 1
    public static final int O     = 2;  // Player 2

    private static final LedColor COLOR_X        = LedColor.BLUE;
    private static final LedColor COLOR_O        = LedColor.RED;
    private static final LedColor COLOR_WIN_CELL = LedColor.WHITE; // winning line highlight

    // [x][y][z] — values: EMPTY, X, O
    private final int[][][] grid = new int[GRID][GRID][GRID];

    private int  currentPlayer; // X or O
    private int  winner;        // EMPTY if no winner yet
    private boolean draw;

    // Winning line cells — highlighted after win
    private int[][] winningCells;

    // 13 direction vectors for 4-in-a-row check
    // Each int[3] is a {dx, dy, dz} unit step
    private static final int[][] DIRECTIONS = {
            // Axes
            {1, 0, 0}, {0, 1, 0}, {0, 0, 1},
            // Face diagonals
            {1, 1, 0}, {1,-1, 0},
            {1, 0, 1}, {1, 0,-1},
            {0, 1, 1}, {0, 1,-1},
            // Space diagonals
            {1, 1, 1}, {1, 1,-1},
            {1,-1, 1}, {1,-1,-1}
    };

    public XoxGame() {
        reset();
    }

    @Override
    public void reset() {
        for (int x = 0; x < GRID; x++)
            for (int y = 0; y < GRID; y++)
                for (int z = 0; z < GRID; z++)
                    grid[x][y][z] = EMPTY;

        currentPlayer = X;
        winner        = EMPTY;
        winningCells  = null;
        draw          = false;
    }

    /**
     * Place current player's mark at (x, y, z).
     * Returns true if the move was valid, false if cell is taken or game is over.
     */
    public boolean makeMove(int x, int y, int z) {
        if (isGameOver())            return false;
        if (!inBounds(x, y, z))     return false;
        if (grid[x][y][z] != EMPTY) return false;

        grid[x][y][z] = currentPlayer;

        // Check win
        int[][] line = findWinningLine(x, y, z, currentPlayer);
        if (line != null) {
            winner       = currentPlayer;
            winningCells = line;
            return true;
        }

        // Check draw
        if (isBoardFull()) {
            draw = true;
            return true;
        }

        // Switch player
        currentPlayer = (currentPlayer == X) ? O : X;
        return true;
    }

    @Override
    public void tick() {
        // XOX is turn-based — no automatic ticking needed.
        // The UI calls makeMove() directly.
    }

    @Override
    public void writeState(Cube cube) {
        cube.clear();

        for (int x = 0; x < GRID; x++) {
            for (int y = 0; y < GRID; y++) {
                for (int z = 0; z < GRID; z++) {
                    int cx = x + OFFSET;
                    int cy = y + OFFSET;
                    int cz = z + OFFSET;

                    if (grid[x][y][z] == X) {
                        cube.set(cx, cy, cz, COLOR_X);
                    } else if (grid[x][y][z] == O) {
                        cube.set(cx, cy, cz, COLOR_O);
                    }
                }
            }
        }

        // Highlight winning line in white
        if (winningCells != null) {
            for (int[] cell : winningCells) {
                cube.set(cell[0] + OFFSET, cell[1] + OFFSET, cell[2] + OFFSET, COLOR_WIN_CELL);
            }
        }
    }

    @Override
    public boolean isGameOver() {
        return winner != EMPTY || draw;
    }

    @Override
    public int getScore() {
        return winner; // X=1, O=2, EMPTY=0 (draw/ongoing)
    }

    public int  getCurrentPlayer() { return currentPlayer; }
    public int  getWinner()        { return winner; }
    public boolean isDraw()        { return draw; }
    public int  getCell(int x, int y, int z) { return grid[x][y][z]; }

    // --- Private helpers ---

    /**
     * Checks all 13 directions from the last placed cell.
     * Returns the 4 winning cell coordinates if found, null otherwise.
     */
    private int[][] findWinningLine(int x, int y, int z, int player) {
        for (int[] dir : DIRECTIONS) {
            int[][] line = checkLine(x, y, z, dir[0], dir[1], dir[2], player);
            if (line != null) return line;
        }
        return null;
    }

    /**
     * From origin (x,y,z), walks both forward and backward along (dx,dy,dz).
     * Collects consecutive cells owned by player.
     * Returns 4-cell array if 4-in-a-row found, null otherwise.
     */
    private int[][] checkLine(int x, int y, int z, int dx, int dy, int dz, int player) {
        // Collect all consecutive owned cells in both directions
        java.util.List<int[]> cells = new java.util.ArrayList<>();
        cells.add(new int[]{x, y, z});

        // Walk forward
        for (int step = 1; step < GRID; step++) {
            int nx = x + dx * step;
            int ny = y + dy * step;
            int nz = z + dz * step;
            if (!inBounds(nx, ny, nz) || grid[nx][ny][nz] != player) break;
            cells.add(new int[]{nx, ny, nz});
        }

        // Walk backward
        for (int step = 1; step < GRID; step++) {
            int nx = x - dx * step;
            int ny = y - dy * step;
            int nz = z - dz * step;
            if (!inBounds(nx, ny, nz) || grid[nx][ny][nz] != player) break;
            cells.add(new int[]{nx, ny, nz});
        }

        if (cells.size() >= 4) {
            return cells.subList(0, 4).toArray(new int[0][]);
        }
        return null;
    }

    private boolean isBoardFull() {
        for (int x = 0; x < GRID; x++)
            for (int y = 0; y < GRID; y++)
                for (int z = 0; z < GRID; z++)
                    if (grid[x][y][z] == EMPTY) return false;
        return true;
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < GRID
                && y >= 0 && y < GRID
                && z >= 0 && z < GRID;
    }
}