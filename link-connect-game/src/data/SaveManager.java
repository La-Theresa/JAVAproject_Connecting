package data;

import model.Constants;
import model.GameSnapshot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 存档管理器，按用户名隔离保存与读取。
 */
public class SaveManager {

    /**
     * 将快照写入用户默认存档槽。
     */
    public void save(GameSnapshot snapshot) throws IOException {
        Path saveFile = savePath(snapshot.username());
        Files.createDirectories(saveFile.getParent());
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(saveFile))) {
            out.writeObject(snapshot);
        }
    }

    /**
     * 读取指定用户存档，若损坏抛出异常。
     */
    public GameSnapshot load(String username) throws IOException, ClassNotFoundException {
        Path saveFile = savePath(username);
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

    private Path savePath(String username) {
        return Path.of(Constants.SAVE_DIR, username + "_slot1.dat");
    }
}
