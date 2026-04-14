package model;

import java.io.Serializable;

/**
 * 用户实体，保存账号与基础战绩。
 */
public class User implements Serializable {
    private final String username;
    private final String passwordHash;
    private int gamesPlayed;
    private int highScore;

    /**
     * 构造用户实体。
     * @param username 用户名
     * @param passwordHash 密码哈希值
     */
    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    /**
     * 返回用户名。
     */
    public String username() {
        return username;
    }

    /**
     * 返回密码哈希。
     */
    public String passwordHash() {
        return passwordHash;
    }

    /**
     * 返回已玩局数。
     */
    public int gamesPlayed() {
        return gamesPlayed;
    }

    /**
     * 返回历史最高分。
     */
    public int highScore() {
        return highScore;
    }

    /**
     * 记录一次对局并刷新最高分。
     */
    public void recordGame(int score) {
        gamesPlayed++;
        if (score > highScore) {
            highScore = score;
        }
    }

    /**
     * 从持久化数据恢复战绩字段。
     */
    public void restoreStats(int gamesPlayed, int highScore) {
        this.gamesPlayed = Math.max(0, gamesPlayed);
        this.highScore = Math.max(0, highScore);
    }
}
