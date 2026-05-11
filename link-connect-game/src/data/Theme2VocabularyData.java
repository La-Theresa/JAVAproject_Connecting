package data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <em> 此处正则表达式与文件查询参考 DeepSeek </em>
 * 
 * 
 * <li>从本地 .tex 格式的词库文件中读取词汇条目</li>
 * <p>将词汇条目映射为连连看棋盘上的图案 ID（奇数代表英文，偶数代表对应的中文）。</p>
 */
public final class Theme2VocabularyData {
    private static final Pattern ENTRY_PATTERN = Pattern.compile(
        "\\\\vocabentry\\{(\\d+)\\}\\{([^}]*)\\}\\{[^}]*\\}\\{([^}]*)\\}"
    );

    private static final List<VocabEntry> ENTRIES = loadEntries();
    private static final Map<Integer, VocabEntry> BY_ID = buildIdMap(ENTRIES);


    public static boolean isAvailable() {
        return !ENTRIES.isEmpty();
    }

    /**
     * 随机获取指定数量的词汇条目 ID
     */
    public static List<Integer> randomEntryIds(int count) {
        if (count <= 0 || ENTRIES.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>(ENTRIES.size());
        for (VocabEntry entry : ENTRIES) {
            ids.add(entry.id());
        }
        Collections.shuffle(ids, ThreadLocalRandom.current());
        if (count >= ids.size()) {
            return ids;
        }
        return new ArrayList<>(ids.subList(0, count));
    }

    /**
     * 根据图案 ID 获取对应的英文词汇（奇数 ID）或中文词汇（偶数 ID）。如果 ID 无效或不存在，返回 null。
     * @param patternId
     * @return
     */
    public static String englishForPattern(int patternId) {
        int pairId = pairId(patternId);
        VocabEntry entry = BY_ID.get(pairId);
        return entry == null ? null : entry.english();
    }

    public static String chineseForPattern(int patternId) {
        int pairId = pairId(patternId);
        VocabEntry entry = BY_ID.get(pairId);
        return entry == null ? null : entry.chinese();
    }

    public static int pairId(int patternId) {
        if (patternId <= 0) {
            return -1;
        }
        return (patternId + 1) / 2;
    }

    public static boolean isEnglishPattern(int patternId) {
        return patternId > 0 && (patternId % 2 == 1);
    }

    public static int englishPatternId(int pairId) {
        return pairId * 2 - 1;
    }

    public static int chinesePatternId(int pairId) {
        return pairId * 2;
    }

    private static List<VocabEntry> loadEntries() {
        Path tex = resolveVocabularyTex();
        if (tex == null) {
            return List.of();
        }

        String content;
        try {
            content = Files.readString(tex, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return List.of();
        }

        // 利用正则捕获组抓取词条
        Matcher matcher = ENTRY_PATTERN.matcher(content);
        List<VocabEntry> result = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();
        while (matcher.find()) {
            int id = parseInt(matcher.group(1));
            if (id <= 0 || seenIds.contains(id)) {
                continue;
            }
            String english = sanitizeEnglish(matcher.group(2));
            String chinese = sanitizeChinese(matcher.group(3));
            if (english.isBlank() || chinese.isBlank()) {
                continue;
            }
            result.add(new VocabEntry(id, english, chinese));
            seenIds.add(id);
        }
        result.sort(Comparator.comparingInt(VocabEntry::id));
        return result;
    }

    private static Map<Integer, VocabEntry> buildIdMap(List<VocabEntry> entries) {
        Map<Integer, VocabEntry> map = new HashMap<>();
        for (VocabEntry entry : entries) {
            map.put(entry.id(), entry);
        }
        return map;
    }

    private static Path resolveVocabularyTex() {
        String relative = "icon/theme2/vocabulary.tex";
        Path direct = Path.of(relative);
        if (Files.exists(direct)) {
            return direct;
        }

        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path userDir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        List<Path> roots = new ArrayList<>();
        roots.add(cwd);
        if (!userDir.equals(cwd)) {
            roots.add(userDir);
        }

        for (Path root : roots) {
            Path projectRoot = detectProjectRoot(root);
            if (projectRoot == null) {
                continue;
            }
            Path candidate = projectRoot.resolve(relative).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Path detectProjectRoot(Path base) {
        Path cursor = base;
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve("src")) && Files.isDirectory(cursor.resolve("icon"))) {
                return cursor;
            }
            Path nested = cursor.resolve("link-connect-game");
            if (Files.isDirectory(nested.resolve("src")) && Files.isDirectory(nested.resolve("icon"))) {
                return nested;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * 正则清理英文字符串，避免过长
     */
    private static String sanitizeEnglish(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\\\", "").trim();
    }

    /**
     * 正则清理中文字符串，避免过长
     * <ul>
     * <li>去除首尾空白</li>
     * <li>移除开头的英文字母、点和空格</li>
     * <li>将全角分号替换为中文逗号</li>
     * <li>只保留第一个逗号前的内容</li>
     * <li>只保留第一个斜杠前的内容</li>
     * </ul>
     */
    private static String sanitizeChinese(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        value = value.replaceAll("^[a-zA-Z.\\s]+", "").trim();
        value = value.replace('；', '，');
        int comma = value.indexOf('，');
        if (comma > 0) {
            value = value.substring(0, comma).trim();
        }
        int slash = value.indexOf('/');
        if (slash > 0) {
            value = value.substring(0, slash).trim();
        }
        return value;
    }

    /**
     * 词条。
     */
    public record VocabEntry(int id, String english, String chinese) {
    }
}
