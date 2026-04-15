package model;

import java.io.Serializable;

/**
 * 存档快照对象。
 */
public class GameSnapshot implements Serializable {
    private final String username;
    private final Constants.Difficulty difficulty;
    private final Constants.Theme theme;
    private final int[][] boardGrid;
    private final int score;
    private final int timeLeft;
    private final int comboCount;
    private final String operationMessage;

    /**
     * 构造存档快照。
     * @param username 用户名
     * @param difficulty 游戏难度
     * @param boardGrid 棋盘图案网格
     * @param score 当前分数
     * @param timeLeft 剩余时间（秒）
     * @param comboCount 连击数
     * @param operationMessage 最近操作信息
     */
    public GameSnapshot(String username,
                        Constants.Difficulty difficulty,
                        Constants.Theme theme,
                        int[][] boardGrid,
                        int score,
                        int timeLeft,
                        int comboCount,
                        String operationMessage) {
        this.username = username;
        this.difficulty = difficulty;
        this.theme = theme;
        this.boardGrid = boardGrid;
        this.score = score;
        this.timeLeft = timeLeft;
        this.comboCount = comboCount;
        this.operationMessage = operationMessage;
    }

    /**
     * 返回用户名。
     */
    public String username() {
        return username;
    }

    /**
     * 返回难度。
     */
    public Constants.Difficulty difficulty() {
        return difficulty;
    }

    public Constants.Theme theme() {
        return theme;
    }

    /**
     * 返回棋盘图案网格。
     */
    public int[][] boardGrid() {
        return boardGrid;
    }

    /**
     * 返回分数。
     */
    public int score() {
        return score;
    }

    /**
     * 返回剩余时间。
     */
    public int timeLeft() {
        return timeLeft;
    }

    /**
     * 返回连击数。
     */
    public int comboCount() {
        return comboCount;
    }

    /**
     * 返回最近操作文本。
     */
    public String operationMessage() {
        return operationMessage;
    }
}
