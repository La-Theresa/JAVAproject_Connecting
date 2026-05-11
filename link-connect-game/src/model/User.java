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

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public int gamesPlayed() {
        return gamesPlayed;
    }

    public int highScore() {
        return highScore;
    }

    public void recordGame(int score) {
        gamesPlayed++;
        if (score > highScore) {
            highScore = score;
        }
    }

    public void restoreStats(int gamesPlayed, int highScore) {
        this.gamesPlayed = gamesPlayed;
        this.highScore = highScore;
    }
}
