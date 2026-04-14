package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GameBoard is the authoritative container for the tile grid state.
 *
 * <p>It maintains two auxiliary indexes that are kept in sync with the grid
 * at all times to avoid scanning the entire grid on every query:
 * <ul>
 *   <li>{@code emptyPositions} — the set of cells that currently hold no tile
 *       (patternId == 0), used for O(1) emptiness checks.</li>
 *   <li>{@code patternLocations} — a map from each active pattern ID to the
 *       set of board positions carrying that pattern, used for O(1) group
 *       lookups without iterating the full grid.</li>
 * </ul>
 *
 * <p>All mutating operations ({@link #setPattern}, {@link #clear}) update both
 * the raw tile array and the two indexes atomically.
 */
public class GameBoard implements Serializable {

    /** Number of rows on the board (fixed at construction). */
    private final int rows;

    /** Number of columns on the board (fixed at construction). */
    private final int cols;

    /**
     * The raw tile grid. {@code tiles[r][c]} holds the {@link Tile} at row
     * {@code r}, column {@code c}. A tile with patternId 0 is considered empty.
     */
    private final Tile[][] tiles;

    /**
     * Index of all positions that are currently empty (patternId == 0).
     * Maintained in sync with {@code tiles} to allow O(1) {@link #isEmpty}
     * checks and O(n_empty) size queries.
     */
    private final Set<Position> emptyPositions = new HashSet<>();

    /**
     * Index mapping each active pattern ID to the set of positions on the
     * board that carry that pattern. Entries are added/removed whenever
     * {@link #setPattern} is called.
     *
     * <p>Invariant: a pattern ID key is present if and only if at least one
     * position on the board has that pattern. Empty sets are removed eagerly.
     */
    private final Map<Integer, Set<Position>> patternLocations = new HashMap<>();

    /**
     * Constructs a board of the given dimensions with all cells empty.
     *
     * @param rows number of rows (must be > 0)
     * @param cols number of columns (must be > 0)
     */
    public GameBoard(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.tiles = new Tile[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tiles[r][c] = new Tile(new Position(r, c), 0);
                emptyPositions.add(new Position(r, c));
            }
        }
    }

    /**
     * Returns the number of rows on this board.
     *
     * @return row count (≥ 1)
     */
    public int rows() {
        return rows;
    }

    /**
     * Returns the number of columns on this board.
     *
     * @return column count (≥ 1)
     */
    public int cols() {
        return cols;
    }

    /**
     * Returns {@code true} if the given position is within the board's bounds.
     *
     * @param p the position to check; a {@code null} argument returns false
     * @return {@code true} if {@code p} is non-null and within [0, rows) × [0, cols)
     */
    public boolean isValid(Position p) {
        return p != null
                && p.row() >= 0 && p.row() < rows
                && p.col() >= 0 && p.col() < cols;
    }

    /**
     * Returns the {@link Tile} object at the given position.
     * The position must be valid (see {@link #isValid}).
     *
     * @param p a valid board position
     * @return the tile at {@code p} (never {@code null})
     */
    public Tile tileAt(Position p) {
        return tiles[p.row()][p.col()];
    }

    /**
     * Sets the pattern at position {@code p} to {@code patternId} and updates
     * both auxiliary indexes ({@code emptyPositions} and {@code patternLocations})
     * atomically.
     *
     * <p>Setting {@code patternId} to 0 is equivalent to calling {@link #clear}.
     * If the position already carries the given pattern, the call is a no-op.
     *
     * @param p         the target board position (must be valid)
     * @param patternId the new pattern identifier; 0 means empty
     */
    public void setPattern(Position p, int patternId) {
        Tile tile = tileAt(p);
        int old = tile.patternId();

        // Skip if the pattern is unchanged — no index work needed.
        if (old == patternId) {
            return;
        }

        // Remove the position from whichever index it currently belongs to.
        if (old == 0) {
            emptyPositions.remove(p);
        } else {
            removePatternLocation(old, p);
        }

        // Apply the new pattern to the tile.
        tile.setPatternId(patternId);

        // Register the position with its new index bucket.
        if (patternId == 0) {
            emptyPositions.add(p);
        } else {
            patternLocations.computeIfAbsent(patternId, k -> new HashSet<>()).add(p);
        }
    }

    /**
     * Clears the tile at position {@code p} (sets its pattern to 0).
     * Equivalent to {@code setPattern(p, 0)}.
     *
     * @param p the position to clear (must be valid)
     */
    public void clear(Position p) {
        setPattern(p, 0);
    }

    /**
     * Returns {@code true} if the cell at {@code p} is empty (patternId == 0).
     * Uses the {@code emptyPositions} index for an O(1) lookup.
     *
     * @param p the board position to check (must be valid)
     * @return {@code true} if the position is empty
     */
    public boolean isEmpty(Position p) {
        return emptyPositions.contains(p);
    }

    /**
     * Returns a snapshot copy of the positions currently holding the given
     * pattern. The returned set is a defensive copy — mutations do not affect
     * the internal index.
     *
     * @param patternId the pattern to look up (must be > 0)
     * @return an independent {@link Set} of positions carrying {@code patternId};
     *         may be empty if no tile with that pattern exists
     */
    public Set<Position> getPatternPositions(int patternId) {
        return new HashSet<>(patternLocations.getOrDefault(patternId, Set.of()));
    }

    /**
     * Returns a live view of the internal pattern-to-positions index as an
     * entry set. Callers that only need to iterate over pattern groups (e.g.
     * {@link logic.DeadEndDetector}) should prefer this over
     * {@link #nonEmptyPositions()} to avoid re-grouping by pattern.
     *
     * <p><b>Warning:</b> the returned entry set is backed by the internal map.
     * Do not mutate it or the associated sets; take a snapshot if mutation
     * is needed.
     *
     * @return an unmodifiable view of the {@code patternId → positions} entries
     */
    public Set<Map.Entry<Integer, Set<Position>>> patternEntries() {
        return Collections.unmodifiableMap(patternLocations).entrySet();
    }

    /**
     * Returns a list of all positions that currently hold a tile (patternId ≠ 0).
     *
     * <p>Implemented by flattening the values of the {@code patternLocations}
     * index, so it runs in O(n_tiles) rather than O(rows × cols).
     *
     * @return a new {@link List} of non-empty positions in unspecified order
     */
    public List<Position> nonEmptyPositions() {
        // Aggregate all positions from the patternLocations index.
        // This avoids iterating every cell in the grid just to filter empties.
        List<Position> result = new ArrayList<>(rows * cols - emptyPositions.size());
        for (Set<Position> group : patternLocations.values()) {
            result.addAll(group);
        }
        return result;
    }

    /**
     * Returns the number of non-empty tiles remaining on the board.
     *
     * @return count of tiles with patternId ≠ 0 (in range [0, rows × cols])
     */
    public int remainingTiles() {
        return rows * cols - emptyPositions.size();
    }

    /**
     * Creates a complete deep copy of this board, including all tile patterns
     * and both auxiliary indexes. Used by {@link logic.BoardGenerator} to
     * simulate board clearability without modifying the original.
     *
     * @return a new {@link GameBoard} with identical state
     */
    public GameBoard deepCopy() {
        GameBoard copy = new GameBoard(rows, cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Position p = new Position(r, c);
                copy.setPattern(p, tileAt(p).patternId());
            }
        }
        return copy;
    }

    /**
     * Exports the current board state as a 2D array of pattern IDs, suitable
     * for serialisation into a {@link model.GameSnapshot}.
     * A value of 0 indicates an empty cell.
     *
     * @return a new {@code int[rows][cols]} grid of pattern IDs
     */
    public int[][] exportPatternGrid() {
        int[][] grid = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = tiles[r][c].patternId();
            }
        }
        return grid;
    }

    /**
     * Restores the board state from a 2D pattern-ID array produced by
     * {@link #exportPatternGrid()}. The supplied grid dimensions must match
     * {@code rows × cols}.
     *
     * @param grid a {@code int[rows][cols]} array of pattern IDs to import
     */
    public void importPatternGrid(int[][] grid) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                setPattern(new Position(r, c), grid[r][c]);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Removes position {@code p} from the set associated with {@code patternId}
     * in the {@code patternLocations} index. If the resulting set is empty, the
     * map entry is removed to keep the invariant that every key has ≥ 1 position.
     *
     * @param patternId the pattern whose index entry should be updated
     * @param p         the position to remove from that entry
     */
    private void removePatternLocation(int patternId, Position p) {
        Set<Position> positions = patternLocations.get(patternId);
        if (positions == null) {
            return; // defensive: should not happen under normal usage
        }
        positions.remove(p);
        if (positions.isEmpty()) {
            // Eagerly remove the empty set to maintain the non-empty invariant.
            patternLocations.remove(patternId);
        }
    }
}
