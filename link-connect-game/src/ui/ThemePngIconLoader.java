package ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * 从 theme1 目录读取 PNG 图标并做尺寸缓存。
 */
public final class ThemePngIconLoader {
    private static final boolean DEBUG_ASSET_PATHS = true;
    private static final Map<String, BufferedImage> ORIGINAL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> FILE_IMAGE_CACHE = new ConcurrentHashMap<>();

    private ThemePngIconLoader() {
    }

    public static BufferedImage loadTileImage(String key, int size) {
        String cacheKey = key + "@" + size;
        BufferedImage cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BufferedImage original = ORIGINAL_CACHE.computeIfAbsent(key, ThemePngIconLoader::loadOriginal);
        if (original == null) {
            BufferedImage fallback = fallbackImage(size, key);
            return fallback;
        }

        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        BufferedImage highQuality = downscaleHighQuality(original, size, size);
        int drawW = highQuality.getWidth();
        int drawH = highQuality.getHeight();
        int x = (size - drawW) / 2;
        int y = (size - drawH) / 2;
        g2.drawImage(highQuality, x, y, null);
        g2.dispose();

        CACHE.put(cacheKey, scaled);
        return scaled;
    }

    public static BufferedImage loadImageFromFile(String relativePath, int width, int height) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        int safeW = Math.max(1, width);
        int safeH = Math.max(1, height);
        String cacheKey = relativePath + "@" + safeW + "x" + safeH;
        BufferedImage cached = FILE_IMAGE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Path path = resolveReadablePath(relativePath);
        if (path == null) {
            if (DEBUG_ASSET_PATHS) {
                System.out.println("[PNG] missing " + relativePath);
            }
            BufferedImage fallback = fallbackImage(Math.min(safeW, safeH), "img");
            FILE_IMAGE_CACHE.put(cacheKey, fallback);
            return fallback;
        }

        try {
            if (DEBUG_ASSET_PATHS) {
                System.out.println("[PNG] loading " + relativePath + " -> " + path.toAbsolutePath());
            }
            BufferedImage source = ImageIO.read(path.toFile());
            if (source == null) {
                return null;
            }
            BufferedImage scaled = downscaleHighQuality(source, safeW, safeH);
            FILE_IMAGE_CACHE.put(cacheKey, scaled);
            return scaled;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BufferedImage loadOriginal(String key) {
        String[] names;
        if ("icon3_2".equals(key)) {
            names = new String[]{"icon3_2.png", "icon3_2资源 9.png"};
        } else {
            names = new String[]{key + ".png"};
        }

        for (String name : names) {
            Path path = resolveReadablePath("icon/theme1/" + name);
            if (path == null) {
                if (DEBUG_ASSET_PATHS) {
                    System.out.println("[PNG] missing icon/theme1/" + name);
                }
                continue;
            }
            try {
                if (DEBUG_ASSET_PATHS) {
                    System.out.println("[PNG] loading icon/theme1/" + name + " -> " + path.toAbsolutePath());
                }
                BufferedImage image = ImageIO.read(path.toFile());
                if (image != null) {
                    return image;
                }
            } catch (Exception ignored) {
                // 尝试下一个候选路径。
            }
        }
        return null;
    }

    private static Path resolveReadablePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }

        Path direct = Path.of(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }

        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path userDir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path classDir = codeSourceDirectory();

        List<Path> roots = new ArrayList<>();
        if (classDir != null) {
            roots.add(classDir);
        }
        roots.add(cwd);
        if (!userDir.equals(cwd)) {
            roots.add(userDir);
        }

        for (Path root : roots) {
            Path projectRoot = detectProjectRoot(root);
            if (projectRoot != null) {
                Path candidate = projectRoot.resolve(relativePath).normalize();
                if (Files.exists(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static Path codeSourceDirectory() {
        try {
            Path location = Path.of(ThemePngIconLoader.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .toAbsolutePath()
                    .normalize();
            if (Files.isRegularFile(location)) {
                return location.getParent();
            }
            return location;
        } catch (URISyntaxException | NullPointerException ignored) {
            return null;
        }
    }

    private static Path detectProjectRoot(Path base) {
        Path cursor = base;
        while (cursor != null) {
            Path srcDir = cursor.resolve("src");
            Path iconDir = cursor.resolve("icon");
            if (Files.isDirectory(srcDir) && Files.isDirectory(iconDir)) {
                return cursor;
            }
            Path nestedProject = cursor.resolve("link-connect-game");
            if (Files.isDirectory(nestedProject.resolve("src")) && Files.isDirectory(nestedProject.resolve("icon"))) {
                return nestedProject;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private static BufferedImage downscaleHighQuality(BufferedImage source, int targetW, int targetH) {
        double ratio = Math.min((double) targetW / Math.max(1, source.getWidth()), (double) targetH / Math.max(1, source.getHeight()));
        int desiredW = Math.max(1, (int) Math.round(source.getWidth() * ratio));
        int desiredH = Math.max(1, (int) Math.round(source.getHeight() * ratio));

        BufferedImage current = source;
        int w = source.getWidth();
        int h = source.getHeight();

        while (w / 2 >= desiredW && h / 2 >= desiredH) {
            w = Math.max(desiredW, w / 2);
            h = Math.max(desiredH, h / 2);
            BufferedImage next = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = next.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(current, 0, 0, w, h, null);
            g2.dispose();
            current = next;
        }

        if (current.getWidth() != desiredW || current.getHeight() != desiredH) {
            BufferedImage finalImg = new BufferedImage(desiredW, desiredH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = finalImg.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(current, 0, 0, desiredW, desiredH, null);
            g2.dispose();
            return finalImg;
        }
        return current;
    }

    private static BufferedImage fallbackImage(int size, String key) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(AuthUiKit.TEXTBOX_BG);
        g2.fillRoundRect(0, 0, size - 1, size - 1, 12, 12);
        g2.setColor(new Color(130, 130, 130));
        g2.drawRoundRect(0, 0, size - 1, size - 1, 12, 12);
        g2.setColor(new Color(45, 45, 45, 120));
        g2.fillRoundRect(size / 4, size / 4, size / 2, size / 2, 10, 10);
        g2.dispose();
        return image;
    }
}