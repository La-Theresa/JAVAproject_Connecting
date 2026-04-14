package data;

import model.Constants;
import model.GameSnapshot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * 存档管理器，按用户名隔离保存与读取。
 */
public class SaveManager {
    private static final DateTimeFormatter NAME_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS", Locale.ROOT);
    private static final DateTimeFormatter DISPLAY_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    public static class SaveSlot {
        private final String fileName;
        private final String displayName;

        /**
         * 构造存档槽。
         * @param fileName 文件名
         * @param displayName 显示名称
         */
        public SaveSlot(String fileName, String displayName) {
            this.fileName = fileName;
            this.displayName = displayName;
        }

        public String fileName() {
            return fileName;
        }

        public String displayName() {
            return displayName;
        }
    }

    /**
     * 将快照写入一个新的存档槽。
     */
    public SaveSlot save(GameSnapshot snapshot) throws IOException {
        Path saveFile = newSavePath(snapshot.username());
        Files.createDirectories(saveFile.getParent());
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(saveFile))) {
            out.writeObject(snapshot);
        }
        String display = formatDisplayName(saveFile.getFileName().toString(), snapshot, Files.getLastModifiedTime(saveFile));
        return new SaveSlot(saveFile.getFileName().toString(), display);
    }

    /**
     * 列出用户全部存档，按时间倒序。
     */
    public List<SaveSlot> listSaves(String username) throws IOException {
        Path dir = Path.of(Constants.SAVE_DIR);
        if (!Files.exists(dir)) {
            return List.of();
        }
        List<Path> candidates = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(username + "_slot_"))
                    .forEach(candidates::add);
        }

        candidates.sort(Comparator.comparing((Path path) -> {
            try {
                return Files.getLastModifiedTime(path);
            } catch (IOException e) {
                return FileTime.fromMillis(0);
            }
        }).reversed());

        List<SaveSlot> slots = new ArrayList<>();
        for (Path path : candidates) {
            String fileName = path.getFileName().toString();
            try {
                GameSnapshot snapshot = load(username, fileName);
                slots.add(new SaveSlot(fileName, formatDisplayName(fileName, snapshot, Files.getLastModifiedTime(path))));
            } catch (IOException | ClassNotFoundException ignored) {
                // 跳过损坏存档，不影响其他可用存档。
            }
        }
        return slots;
    }

    /**
     * 读取指定用户的指定存档槽，若损坏抛出异常。
     */
    public GameSnapshot load(String username, String slotFileName) throws IOException, ClassNotFoundException {
        Path saveFile = savePath(username, slotFileName);
        if (!Files.exists(saveFile)) {
            return null;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(saveFile))) {
            Object data = in.readObject();
            if (!(data instanceof GameSnapshot)) {
                throw new IOException("Invalid save file");
            }
            GameSnapshot snapshot = (GameSnapshot) data;
            if (!username.equals(snapshot.username())) {
                throw new IOException("Invalid save file");
            }
            return snapshot;
        }
    }

    /**
     * 生成新存档文件的完整路径。
     * @param username 用户名
     * @return 存档文件路径
     */
    private Path newSavePath(String username) {
        String ts = NAME_TS_FORMAT.format(Instant.now().atZone(ZoneId.systemDefault()));
        return Path.of(Constants.SAVE_DIR, username + "_slot_" + ts + ".dat");
    }

    /**
     * 解析存档槽文件名并返回完整路径。
     * @param username 用户名
     * @param slotFileName 存档槽文件名
     * @return 存档文件路径
     * @throws IOException 文件名格式错误
     */
    private Path savePath(String username, String slotFileName) throws IOException {
        if (slotFileName == null || slotFileName.isBlank()) {
            throw new IOException("Invalid save slot");
        }
        if (!slotFileName.startsWith(username + "_slot_") || !slotFileName.endsWith(".dat") || slotFileName.contains("/") || slotFileName.contains("\\")) {
            throw new IOException("Invalid save slot");
        }
        return Path.of(Constants.SAVE_DIR, slotFileName);
    }

    private String formatDisplayName(String fileName, GameSnapshot snapshot, FileTime modifiedTime) {
        String timeText = DISPLAY_TS_FORMAT.format(modifiedTime.toInstant().atZone(ZoneId.systemDefault()));
        return String.format(
                Locale.ROOT,
                "%s | %s | score=%d | time=%ds",
                timeText,
                snapshot.difficulty().name(),
                snapshot.score(),
                snapshot.timeLeft()
        );
    }
}
