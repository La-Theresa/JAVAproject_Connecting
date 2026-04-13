package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 棋盘状态容器，维护方块网格及用于性能优化的索引缓存。
 */
public class GameBoard implements Serializable {
    private final int rows;
    private final int cols;
    private final Tile[][] tiles;
    private final Set<Position> emptyPositions = new HashSet<>();
    private final Map<Integer, Set<Position>> patternLocations = new HashMap<>();

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
     * 返回行数。
     */
    public int rows() {
        return rows;
    }

    /**
     * 返回列数。
     */
    public int cols() {
        return cols;
    }

    /**
     * 判断位置是否在棋盘内。
     */
    public boolean isValid(Position p) {
        return p != null && p.row() >= 0 && p.row() < rows && p.col() >= 0 && p.col() < cols;
    }

    /**
     * 获取指定位置方块。
     */
    public Tile tileAt(Position p) {
        return tiles[p.row()][p.col()];
    }

    /**
     * 设置指定位置图案并刷新缓存。
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

    /**
     * 消除指定位置方块。
     */
    public void clear(Position p) {
        setPattern(p, 0);
    }

    /**
     * 判断位置是否为空。
     */
    public boolean isEmpty(Position p) {
        return emptyPositions.contains(p);
    }

    /**
     * 获取某个图案的所有位置副本。
     */
    public Set<Position> getPatternPositions(int patternId) {
        return new HashSet<>(patternLocations.getOrDefault(patternId, Set.of()));
    }

    /**
     * 返回所有非空位置列表。
     */
    public List<Position> nonEmptyPositions() {
        List<Position> res = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Position p = new Position(r, c);
                if (!isEmpty(p)) {
                    res.add(p);
                }
            }
        }
        return res;
    }

    /**
     * 统计剩余非空方块数。
     */
    public int remainingTiles() {
        return rows * cols - emptyPositions.size();
    }

    /**
     * 深拷贝棋盘，用于可解性模拟。
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
     * 导出为二维图案数组，用于存档。
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
     * 从二维图案数组恢复棋盘。
     */
    public void importPatternGrid(int[][] grid) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                setPattern(new Position(r, c), grid[r][c]);
            }
        }
    }

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
