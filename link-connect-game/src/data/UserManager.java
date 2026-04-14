package data;

import model.Constants;
import model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户注册登录管理，数据持久化到本地文本文件。
 */
public class UserManager {
    private final Map<String, User> users = new HashMap<>();
    private final Path userFilePath = Path.of(Constants.USER_FILE);

    public UserManager() {
        load();
    }

    /**
     * 注册用户，成功返回true，重复用户名返回false。
     */
    public boolean register(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        if (users.containsKey(username)) {
            return false;
        }
        users.put(username, new User(username, hash(rawPassword)));
        save();
        return true;
    }

    /**
     * 用户登录，成功返回用户对象，失败返回null。
     */
    public User login(String username, String rawPassword) {
        User user = users.get(username);
        if (user == null) {
            return null;
        }
        if (!user.passwordHash().equals(hash(rawPassword))) {
            return null;
        }
        return user;
    }

    /**
     * 更新战绩并落盘。
     */
    public void recordGame(User user, int score) {
        if (user == null) {
            return;
        }
        user.recordGame(score);
        save();
    }

    /**
     * 返回当前所有用户副本。
     */
    public List<User> allUsers() {
        return new ArrayList<>(users.values());
    }

    /**
     * 从持久化文件加载用户数据。
     */
    private void load() {
        try {
            Files.createDirectories(userFilePath.getParent());
            if (!Files.exists(userFilePath)) {
                Files.createFile(userFilePath);
                return;
            }
            List<String> lines = Files.readAllLines(userFilePath, StandardCharsets.UTF_8);
            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts.length != 4) {
                    continue;
                }
                User user = new User(parts[0], parts[1]);
                int gamesPlayed = parseIntOrZero(parts[2]);
                int highScore = parseIntOrZero(parts[3]);
                user.restoreStats(gamesPlayed, highScore);
                users.put(parts[0], user);
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * 将用户数据保存到持久化文件。
     */
    private void save() {
        List<String> lines = new ArrayList<>();
        for (User user : users.values()) {
            lines.add(user.username() + ":" + user.passwordHash() + ":" + user.gamesPlayed() + ":" + user.highScore());
        }
        try {
            Files.createDirectories(userFilePath.getParent());
            Files.write(userFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    /**
     * 使用SHA-256生成密码哈希。
     * @param input 明文密码
     * @return 十六进制哈希字符串
     */
    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 安全解析整数，失败返回0。
     * @param value 字符串值
     * @return 解析结果或0
     */
    private int parseIntOrZero(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
