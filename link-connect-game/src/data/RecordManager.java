package data;

import model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 排行榜服务，按分数降序排序。
 */
public class RecordManager {
    private final UserManager userManager;

    public RecordManager(UserManager userManager) {
        this.userManager = userManager;
    }

    /**
     * 返回Top N排行榜文本列表。
     */
    public List<String> topN(int n) {
        List<User> users = userManager.allUsers();
        users.sort(Comparator.comparingInt(User::highScore).reversed().thenComparing(User::username));
        List<String> lines = new ArrayList<>();
        int limit = Math.min(n, users.size());
        for (int i = 0; i < limit; i++) {
            User u = users.get(i);
            lines.add((i + 1) + ". " + u.username() + " - " + u.highScore());
        }
        if (lines.isEmpty()) {
            lines.add("No records yet");
        }
        return lines;
    }
}
