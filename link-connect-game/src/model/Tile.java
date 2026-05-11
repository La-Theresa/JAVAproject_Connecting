package model;

import java.io.Serializable;

/**
 * 棋盘中的图案方块。
 */
public class Tile implements Serializable {
    private final Position position;
    private int patternId;

    /**
     * 构造图案方块。
     * @param position 方块所在位置（基本不使用，避免创建新对象）
     * @param patternId 图案编号，0表示空
     */
    public Tile(Position position, int patternId) {
        this.position = position;
        this.patternId = patternId;
    }

    public Position position() {
        return position;
    }

    public int patternId() {
        return patternId;
    }

    public boolean isEmpty() {
        return patternId == 0;
    }

    public boolean hasSamePattern(Tile other) {
        return other != null && patternId != 0 && patternId == other.patternId;
    }

    public void clear() {
        patternId = 0;
    }

    public void setPatternId(int patternId) {
        this.patternId = patternId;
    }
}
