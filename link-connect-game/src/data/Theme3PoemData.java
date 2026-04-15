package data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 读取 theme3 诗句资源并提供上下句配对数据。
 */
public final class Theme3PoemData {
    private static final Pattern ENTRY_PATTERN = Pattern.compile("\\{([^}]*)\\}\\{([^}]*)\\}");

    private static final List<PoemEntry> ENTRIES = loadEntries();
    private static final Map<Integer, PoemEntry> BY_ID = buildIdMap(ENTRIES);

    private Theme3PoemData() {
    }

    public static boolean isAvailable() {
        return !ENTRIES.isEmpty();
    }

    public static List<Integer> randomEntryIds(int count) {
        if (count <= 0 || ENTRIES.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>(ENTRIES.size());
        for (PoemEntry entry : ENTRIES) {
            ids.add(entry.id());
        }
        Collections.shuffle(ids, ThreadLocalRandom.current());
        if (count >= ids.size()) {
            return ids;
        }
        return new ArrayList<>(ids.subList(0, count));
    }

    public static String firstLineForPattern(int patternId) {
        int pairId = pairId(patternId);
        PoemEntry entry = BY_ID.get(pairId);
        return entry == null ? null : entry.firstLine();
    }

    public static String secondLineForPattern(int patternId) {
        int pairId = pairId(patternId);
        PoemEntry entry = BY_ID.get(pairId);
        return entry == null ? null : entry.secondLine();
    }

    public static int pairId(int patternId) {
        if (patternId <= 0) {
            return -1;
        }
        return (patternId + 1) / 2;
    }

    public static boolean isFirstLinePattern(int patternId) {
        return patternId > 0 && (patternId % 2 == 1);
    }

    public static int firstPatternId(int pairId) {
        return pairId * 2 - 1;
    }

    public static int secondPatternId(int pairId) {
        return pairId * 2;
    }

    private static List<PoemEntry> loadEntries() {
        Path md = resolvePoemsMd();
        if (md == null) {
            return List.of();
        }

        String content;
        try {
            content = Files.readString(md, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return List.of();
        }

        Matcher matcher = ENTRY_PATTERN.matcher(content);
        List<PoemEntry> result = new ArrayList<>();
        Set<String> seenPairs = new LinkedHashSet<>();
        int index = 1;
        while (matcher.find()) {
            String first = sanitize(matcher.group(1));
            String second = sanitize(matcher.group(2));
            if (first.isBlank() || second.isBlank()) {
                continue;
            }
            String key = first + "|" + second;
            if (!seenPairs.add(key)) {
                continue;
            }
            result.add(new PoemEntry(index++, first, second));
        }
        return result;
    }

    private static Map<Integer, PoemEntry> buildIdMap(List<PoemEntry> entries) {
        Map<Integer, PoemEntry> map = new HashMap<>();
        for (PoemEntry entry : entries) {
            map.put(entry.id(), entry);
        }
        return map;
    }

    private static Path resolvePoemsMd() {
        String relative = "icon/theme3/poems.md";
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

    private static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\s+", "").trim();
    }

    /**
     * 诗句条目。
     */
    public record PoemEntry(int id, String firstLine, String secondLine) {
    }
}