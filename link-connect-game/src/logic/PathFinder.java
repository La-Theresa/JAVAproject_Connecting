package logic;

import model.GameBoard;
import model.Path;
import model.Position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * PathFinder implements the core pathfinding logic for the Link Connect game.
 *
 * <p>It determines whether two tiles can be connected by a path with at most 2
 * direction changes (turns), and returns the connecting path as a sequence of
 * key waypoints if one exists.
 *
 * <p>The algorithm extends the logical board by a 1-cell border of empty space
 * on all sides, allowing paths to route around the outside of the grid.
 * A 0-1 BFS (deque-based) is used: straight moves cost 0 extra; turning costs 1.
 * States that exceed 2 turns are pruned immediately.
 *
 * <p>Performance notes:
 * <ul>
 *   <li>The BFS parent table is a flat {@code int[][][]} array instead of a
 *       {@code HashMap} to avoid per-entry boxing and hash overhead.</li>
 *   <li>{@code isWalkable} is inlined to avoid {@code new Position()} allocation
 *       on every inner-loop iteration.</li>
 *   <li>The state identity is {@code (row, col, dir)} only; {@code turns} is
 *       tracked as a separate value, not part of the key.</li>
 * </ul>
 */
public class PathFinder {

    /** Row deltas for the 4 movement directions: up, down, left, right. */
    private static final int[] DR = {-1, 1, 0, 0};

    /** Column deltas for the 4 movement directions: up, down, left, right. */
    private static final int[] DC = {0, 0, -1, 1};

    /**
     * Sentinel value stored in the parent table to indicate "no parent"
     * (i.e. this is the search root). Chosen as a value that cannot be a
     * valid encoded state since direction is stored in bits 0–1 and is always
     * 0–3, so a value of -1 is unambiguous.
     */
    private static final int NO_PARENT = -1;

    /**
     * Finds a valid path connecting {@code start} to {@code end} on the board,
     * respecting the at-most-2-turns constraint, or returns {@code null} if no
     * such path exists.
     *
     * <p>Pre-conditions checked:
     * <ul>
     *   <li>Start and end are not the same position.</li>
     *   <li>Both positions are within board bounds.</li>
     *   <li>Neither position is empty (patternId == 0).</li>
     *   <li>Both tiles carry the same pattern.</li>
     * </ul>
     *
     * @param board the current game board
     * @param start the first tile position (0-indexed row/col)
     * @param end   the second tile position (0-indexed row/col)
     * @return a {@link Path} of key waypoints if connectable, or {@code null}
     */
    public Path findPath(GameBoard board, Position start, Position end) {
        // Reject trivially invalid inputs before allocating anything.
        if (start.equals(end)) {
            return null;
        }
        if (!board.isValid(start) || !board.isValid(end)) {
            return null;
        }
        if (board.isEmpty(start) || board.isEmpty(end)) {
            return null;
        }
        if (!board.tileAt(start).hasSamePattern(board.tileAt(end))) {
            return null;
        }

        // Extend the board by 1 cell on every side so paths can skirt the edges.
        int rows = board.rows() + 2; // shifted coordinate space height
        int cols = board.cols() + 2; // shifted coordinate space width

        // Shift start/end into the extended coordinate space (add 1 to each axis).
        int sr = start.row() + 1;
        int sc = start.col() + 1;
        int tr = end.row() + 1;
        int tc = end.col() + 1;

        /*
         * bestTurns[r][c][d] = fewest turns to reach cell (r,c) arriving from
         * direction d. Initialised to MAX_VALUE (unvisited). Only the direction
         * matters for state identity — turns is the cost, not part of the key.
         */
        int[][][] bestTurns = new int[rows][cols][4];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                for (int d = 0; d < 4; d++) {
                    bestTurns[r][c][d] = Integer.MAX_VALUE;
                }
            }
        }

        /*
         * parent[r][c][d] stores the encoded predecessor state that produced the
         * best arrival at (r,c,d). Encoding: (parentRow * cols + parentCol) * 4 + parentDir.
         * NO_PARENT (-1) means this is the root.
         *
         * Using a flat array instead of HashMap<State,State> eliminates all boxing
         * overhead and hash computations in the hot BFS inner loop.
         */
        int[][][] parent = new int[rows][cols][4];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                for (int d = 0; d < 4; d++) {
                    parent[r][c][d] = NO_PARENT;
                }
            }
        }

        // 0-1 BFS deque: straight moves go to the front (cost 0), turns to the back (cost 1).
        Deque<int[]> queue = new ArrayDeque<>();

        // Seed all 4 initial directions from the start cell with 0 turns.
        for (int d = 0; d < 4; d++) {
            bestTurns[sr][sc][d] = 0;
            // Encoded state: (row, col, dir) stored as a single int[3].
            queue.offer(new int[]{sr, sc, d});
        }

        while (!queue.isEmpty()) {
            int[] cur = queue.pollFirst();
            int cr = cur[0];
            int cc = cur[1];
            int cd = cur[2];
            int ct = bestTurns[cr][cc][cd]; // turns accumulated to reach this state

            // Try all 4 movement directions from the current cell.
            for (int nd = 0; nd < 4; nd++) {
                int nr = cr + DR[nd];
                int nc = cc + DC[nd];

                // A turn costs 1; keeping the same direction costs 0.
                int nt = ct + (cd == nd ? 0 : 1);

                // Prune: game rule allows at most 2 turns.
                if (nt > 2) {
                    continue;
                }

                // Inline walkability check to avoid allocating a Position object.
                // Out-of-bounds → not walkable.
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) {
                    continue;
                }
                // The target cell is always walkable (it is a matching tile, not empty).
                boolean isTarget = (nr == tr && nc == tc);
                if (!isTarget) {
                    // Border cells (row 0, col 0, last row, last col) are the
                    // virtual outer ring — always empty and always walkable.
                    boolean isBorder = (nr == 0 || nc == 0 || nr == rows - 1 || nc == cols - 1);
                    if (!isBorder) {
                        // Interior cell: walkable only if it is empty on the original board.
                        if (!board.isEmpty(new Position(nr - 1, nc - 1))) {
                            continue;
                        }
                    }
                }

                // Prune: only update if this path is strictly better than any known arrival.
                if (nt >= bestTurns[nr][nc][nd]) {
                    continue;
                }

                // Record the best known turns and parent for path reconstruction.
                bestTurns[nr][nc][nd] = nt;
                parent[nr][nc][nd] = encode(cr, cc, cd, cols); // store predecessor

                if (isTarget) {
                    // Reached the target — reconstruct and return the path immediately.
                    return rebuildPath(parent, nr, nc, nd, sr, sc, rows, cols);
                }

                // 0-1 BFS: straight moves (same direction) go to the front of the deque.
                if (cd == nd) {
                    queue.offerFirst(new int[]{nr, nc, nd});
                } else {
                    queue.offerLast(new int[]{nr, nc, nd});
                }
            }
        }
        return null; // No path found within the 2-turn limit.
    }

    /**
     * Checks whether two positions on the board can be connected.
     * Delegates to {@link #findPath} for the full search.
     *
     * @param board the current game board
     * @param start the first tile position
     * @param end   the second tile position
     * @return {@code true} if a valid connecting path exists
     */
    public boolean canConnect(GameBoard board, Position start, Position end) {
        return findPath(board, start, end) != null;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Encodes a BFS state {@code (row, col, dir)} as a single integer for
     * compact storage in the parent table.
     *
     * <p>Encoding: {@code (row * cols + col) * 4 + dir}
     *
     * @param row  row coordinate in shifted space
     * @param col  column coordinate in shifted space
     * @param dir  movement direction (0=up, 1=down, 2=left, 3=right)
     * @param cols number of columns in the shifted board
     * @return the encoded integer representing this state
     */
    private int encode(int row, int col, int dir, int cols) {
        return (row * cols + col) * 4 + dir;
    }

    /**
     * Decodes a packed state integer back into {@code [row, col, dir]}.
     *
     * @param encoded the packed state integer produced by {@link #encode}
     * @param cols    number of columns in the shifted board
     * @return an int array {@code [row, col, dir]}
     */
    private int[] decode(int encoded, int cols) {
        int dir = encoded % 4;
        int cell = encoded / 4;
        int col = cell % cols;
        int row = cell / cols;
        return new int[]{row, col, dir};
    }

    /**
     * Reconstructs the connecting path by walking backwards through the parent
     * table from the target cell to the source cell, then collapses collinear
     * steps into waypoints (so the returned path contains only direction-change
     * corners, plus the two endpoints).
     *
     * @param parent   flat parent table: {@code parent[r][c][d]} = encoded predecessor
     * @param endR     row of the target cell (shifted space)
     * @param endC     col of the target cell (shifted space)
     * @param endD     arrival direction at the target cell
     * @param startR   row of the source cell (shifted space) — used to detect the root
     * @param startC   col of the source cell (shifted space)
     * @param rows     total rows in shifted space (unused directly, kept for clarity)
     * @param cols     total cols in shifted space (needed for decode)
     * @return {@link Path} with original-coordinate waypoints (unshifted)
     */
    private Path rebuildPath(int[][][] parent,
                             int endR, int endC, int endD,
                             int startR, int startC,
                             int rows, int cols) {
        // Walk parent pointers from target back to source, collecting shifted coords.
        List<int[]> reversed = new ArrayList<>();
        int cr = endR, cc = endC, cd = endD;
        reversed.add(new int[]{cr, cc});

        while (!(cr == startR && cc == startC)) {
            int enc = parent[cr][cc][cd];
            if (enc == NO_PARENT) {
                break; // reached the root (start cell)
            }
            int[] prev = decode(enc, cols);
            cr = prev[0];
            cc = prev[1];
            cd = prev[2];
            reversed.add(new int[]{cr, cc});
        }

        /*
         * Convert to original (unshifted) coordinates and collapse collinear
         * segments so that only direction-change waypoints are kept.
         * The resulting path has 2–4 points: start, 0–2 corners, end.
         */
        List<Position> waypoints = new ArrayList<>();
        int[] prevDir = null; // direction vector of the previous segment

        for (int i = reversed.size() - 1; i >= 0; i--) {
            int[] pt = reversed.get(i);
            // Unshift: subtract 1 from each axis to return to board coordinates.
            Position unshifted = new Position(pt[0] - 1, pt[1] - 1);

            if (waypoints.isEmpty()) {
                waypoints.add(unshifted);
                continue;
            }

            Position last = waypoints.get(waypoints.size() - 1);
            // Compute the unit direction vector from 'last' to 'unshifted'.
            int[] curDir = new int[]{
                Integer.compare(unshifted.row() - last.row(), 0),
                Integer.compare(unshifted.col() - last.col(), 0)
            };

            if (prevDir == null || curDir[0] != prevDir[0] || curDir[1] != prevDir[1]) {
                // Direction changed — add a new waypoint at the corner.
                waypoints.add(unshifted);
            } else {
                // Same direction — extend the current segment (replace last waypoint).
                waypoints.set(waypoints.size() - 1, unshifted);
            }
            prevDir = curDir;
        }

        return new Path(waypoints);
    }
}
