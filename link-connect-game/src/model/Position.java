package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * 棋盘位置。
 */
public final class Position implements Serializable {
    private final int row;
    private final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }

    public boolean isSameRow(Position other) {
        return this.row == other.row;
    }

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