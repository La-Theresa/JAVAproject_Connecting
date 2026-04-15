package ui;

import controller.GameController;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

/**
 * 排行榜显示面板。
 */
public class LeaderboardPanel extends JPanel {
    private final JTextArea area = new JTextArea();

    public LeaderboardPanel(GameController controller) {
        setLayout(new BorderLayout());
        setBackground(AuthUiKit.APP_BG);
        area.setEditable(false);
        area.setBackground(AuthUiKit.TEXTBOX_BG);
        area.setForeground(new Color(30, 30, 30));
        area.setFont(AuthUiKit.localizedSansFont("", Font.PLAIN, 15));
        area.setBorder(new EmptyBorder(12, 16, 12, 16));
        add(area, BorderLayout.CENTER);

        JButton back = AuthUiKit.createPrimaryButton("Back");
        back.addActionListener(e -> controller.goToMenu());
        add(back, BorderLayout.SOUTH);
    }

    /**
     * 刷新排行榜文本。
     */
    public void updateLines(List<String> lines) {
        String content = String.join("\n", lines);
        area.setFont(AuthUiKit.localizedSansFont(content, Font.PLAIN, 15));
        area.setText(content);
    }
}
