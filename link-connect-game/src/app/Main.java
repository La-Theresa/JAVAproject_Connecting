package app;

import controller.GameController;
import ui.GameFrame;

import javax.swing.SwingUtilities;
import java.io.IOException;
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
}
