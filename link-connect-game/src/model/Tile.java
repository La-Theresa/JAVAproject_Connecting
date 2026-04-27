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

    /**
     * 返回方块所在位置。
     */
    public Position position() {
        return position;
    }

    /**
     * 返回图案编号，0表示空。
     */
    public int patternId() {
        return patternId;
    }

    /**
     * 判断当前方块是否为空。
     */
    public boolean isEmpty() {
        return patternId == 0;
    }

    /**
     * 判断两方块是否图案相同且非空。
     */
    public boolean hasSamePattern(Tile other) {
        return other != null && patternId != 0 && patternId == other.patternId;
    }

    /**
     * 消除当前方块。
     */
    public void clear() {
        patternId = 0;
    }

    /**
     * 设置图案编号。
     */
    public void setPatternId(int patternId) {
        this.patternId = patternId;
    }
}
