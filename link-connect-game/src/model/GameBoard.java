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
 * GameBoard 是各种状态的储存，记录了当前状态
 * <p>使用 {@code Serializable} 以支持存档 {@link model.GameSnapshot} 的序列化和反序列化
 * 以及深拷贝 {@link #deepCopy()} 的实现。</p>
 *
 * <p>储存了两个与网格保持同步的辅助索引，
 * 避免每次查询都要迭代整个网格</p>
 * <ul>
 *   <li>{@code emptyPositions} 记录空单元格，供 O(1) 检查</li>
 *   <li>{@code patternLocations} 一个 ID 与图案的 {@code Map} 供 O(1) 单元格查找</li>
 * </ul>
 *
 * <p>所有操作如 ({@link #setPattern}, {@link #clear}) 会自动更新索引
 */
public class GameBoard implements Serializable {

    private final int rows;

    private final int cols;

    private final Constants.Theme theme;

    /**
     * {@code tiles[r][c]} 保存了 {@link Tile} 及其 行数
     * {@code r}, 列数 {@code c}. ID 为 0 的单元格表示空单元格。
     * 
     * <p>虽然{@link tile} 有 {@code position} 属性，但
     * 这里另外使用二维数组记录位置实现简化查询，避免需要重新创建对象</p>
     */
    private final Tile[][] tiles;

    /**
     * 所有空位置的索引
     * 与 {@code tiles} 同步以实现 O(1) {@link #isEmpty}
     * 的检查和 O(n_empty) 大小查询.
     */
    private final Set<Position> emptyPositions = new HashSet<>();

    /**
     * ID 与位置的索引，供 O(1) 查询所有具有特定图案的单元格。
     *
     * <p>移除单元格时会自动清理空集</p>
     */
    private final Map<Integer, Set<Position>> patternLocations = new HashMap<>();

    /**
     * 构建棋盘
     */
    public GameBoard(int rows, int cols) {
        this(rows, cols, Constants.Theme.THEME1);
    }

    public GameBoard(int rows, int cols, Constants.Theme theme) {
        this.rows = rows;
        this.cols = cols;
        this.theme = theme == null ? Constants.Theme.THEME1 : theme;
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

    public Constants.Theme theme() {
        return theme;
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

    public boolean canMatchPatterns(int patternA, int patternB) {
        if (patternA <= 0 || patternB <= 0) {
            return false;
        }
        if (theme == Constants.Theme.THEME1) {
            return patternA == patternB;
        }
        int pairA = (patternA + 1) / 2;
        int pairB = (patternB + 1) / 2;
        boolean sideDifferent = (patternA % 2) != (patternB % 2);
        return pairA == pairB && sideDifferent;
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
        GameBoard copy = new GameBoard(rows, cols, theme);
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
