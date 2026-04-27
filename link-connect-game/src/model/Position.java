package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * 棋盘位置值对象。
 */
public final class Position implements Serializable {
    private final int row;
    private final int col;

    /**
     * 构造棋盘位置。
     * @param row 行坐标
     * @param col 列坐标
     */
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * 返回行坐标。
     */
    public int row() {
        return row;
    }

    /**
     * 返回列坐标。
     */
    public int col() {
        return col;
    }

    /**
     * 判断是否同行。
     */
    public boolean isSameRow(Position other) {
        return this.row == other.row;
    }

    /**
     * 判断是否同列。
     */
    public boolean isSameCol(Position other) {
        return this.col == other.col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position)) {
            return false;
        }
        Position position = (Position) o;
        return row == position.row && col == position.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}