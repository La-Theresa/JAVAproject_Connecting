package app;

import ui.GameFrame;

import javax.swing.SwingUtilities;

/**
 * 程序入口。
 */
public class Main {
    /**
     * 启动Swing界面。
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}
