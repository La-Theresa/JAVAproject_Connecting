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
     * 
     * <p>默认为 Theme1，但一般不使用
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

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public Constants.Theme theme() {
        return theme;
    }

    /**
     * 若需要判断的点在边界内且非null返回 {@code true}
     */
    public boolean isValid(Position p) {
        return p != null
                && p.row() >= 0 && p.row() < rows
                && p.col() >= 0 && p.col() < cols;
    }

    public Tile tileAt(Position p) {
        return tiles[p.row()][p.col()];
    }

    /**
     * 设在位置 {@code p} 的图案为 {@code patternId} 并更新
     *  ({@code emptyPositions} 和 {@code patternLocations})
     *
     * <p>将 {@code patternId} 设为 0 等价于使用 {@link #clear}.
     * 若前后图案相同，该函数直接返回
     *
     * @param p         目标位置，须为 valid
     * @param patternId 新的图案id
     */
    public void setPattern(Position p, int patternId) {
        Tile tile = tileAt(p);
        int old = tile.patternId();

        if (old == patternId) {
            return;
        }

        if (old == 0) {
            emptyPositions.remove(p);
        } else {
            removePatternLocation(old, p);
        }

        tile.setPatternId(patternId);

        if (patternId == 0) {
            emptyPositions.add(p);
        } else {
            patternLocations.computeIfAbsent(patternId, k -> new HashSet<>()).add(p);
        }
    }

    public void clear(Position p) {
        setPattern(p, 0);
    }

    /**
     * 使用 {@code emptyPositions} 作 O(1) 查询
     */
    public boolean isEmpty(Position p) {
        return emptyPositions.contains(p);
    }


    /**
     * 在不同主题中判断二者是否是一对
     */
    public boolean canMatchPatterns(int patternA, int patternB) {
        if (theme == Constants.Theme.THEME1) {
            return patternA == patternB;
        }

        // 其余主题下 A 为奇数，B 为偶数，这里利用 JAVA 自动向下取整
        int pairA = (patternA + 1) / 2;
        int pairB = (patternB + 1) / 2;
        boolean sideDifferent = (patternA % 2) != (patternB % 2);
        return pairA == pairB && sideDifferent;
    }

    /**
     * 返回当前图案位置的副本
     *
     * @param patternId 查询的 ID
     * @return 一个不影响原有位置的包含 {@code patternId} 的 {@link Set} ;
     *         若不存在则为空
     */
    public Set<Position> getPatternPositions(int patternId) {
        return new HashSet<>(patternLocations.getOrDefault(patternId, Set.of()));
    }

    /**
     * 返回一个不可更改的 {@code patternId → positions} 映射
     */
    public Set<Map.Entry<Integer, Set<Position>>> patternEntries() {
        return Collections.unmodifiableMap(patternLocations).entrySet();
    }

    /**
     * 返回非空位置列表
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
     * 返回非空数
     */
    public int remainingTiles() {
        return rows * cols - emptyPositions.size();
    }

    /**
     * 返回棋盘深拷贝
     * <p>被 {@link logic.BoardGenerator} 用来检验是否可消
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
     * 以数组形式返回棋盘
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
     * 根据 {@link #exportPatternGrid()} 导出的数组重置棋盘状态。
     */
    public void importPatternGrid(int[][] grid) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                setPattern(new Position(r, c), grid[r][c]);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 私有函数
    // -------------------------------------------------------------------------

    /**
     *在{@code patternLocations} 取出有 {@code patternId} 的位置 {@code p}
     * 若取出后为空，则删除
     */
    private void removePatternLocation(int patternId, Position p) {
        Set<Position> positions = patternLocations.get(patternId);
        if (positions == null) {
            return;
        }
        positions.remove(p);
        if (positions.isEmpty()) {
            patternLocations.remove(patternId);
        }
    }
}
