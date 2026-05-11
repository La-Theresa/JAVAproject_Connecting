package model;

import java.io.Serializable;

/**
 * 存档快照
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

    public String username() {
        return username;
    }

    public Constants.Difficulty difficulty() {
        return difficulty;
    }

    public Constants.Theme theme() {
        return theme;
    }

    public int[][] boardGrid() {
        return boardGrid;
    }

    public int score() {
        return score;
    }

    public int timeLeft() {
        return timeLeft;
    }

    public int comboCount() {
        return comboCount;
    }

    public String operationMessage() {
        return operationMessage;
    }
}
