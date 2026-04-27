package app;

import controller.GameController;
import ui.AuthUiKit;
import ui.GameFrame;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.io.IOException;
import java.util.Enumeration;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * 程序入口。
 */
public class Main {
    private static ServerSocket singleInstanceLock;

    /**
     * 启动Swing界面。
     */
    public static void main(String[] args){ 
        if (!acquireSingleInstance()) {
            System.err.println("Application is already running.");
            return;
        }
        applyGlobalUiFonts();
        SwingUtilities.invokeLater(() -> {
            GameController controller = new GameController();
            GameFrame frame = new GameFrame(controller);
            frame.setVisible(true);
        });
    }

    private static boolean acquireSingleInstance() {
        try {
            singleInstanceLock = new ServerSocket(47321, 1, InetAddress.getLoopbackAddress());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (singleInstanceLock != null && !singleInstanceLock.isClosed()) {
                        singleInstanceLock.close();
                    }
                } catch (IOException ignored) {
                }
            }));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void applyGlobalUiFonts() {
        FontUIResource bodyUiFont = new FontUIResource(new Font(AuthUiKit.BODY_FONT_FAMILY, Font.PLAIN, 14));
        UIDefaults defaults = UIManager.getDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, bodyUiFont);
            }
        }
    }
}
