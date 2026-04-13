package ui;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.util.List;

/**
 * 排行榜显示面板。
 */
public class LeaderboardPanel extends JPanel {
    private final JTextArea area = new JTextArea();

    public LeaderboardPanel(GameFrame frame) {
        setLayout(new BorderLayout());
        area.setEditable(false);
        add(area, BorderLayout.CENTER);

        JButton back = new JButton("Back");
        back.addActionListener(e -> frame.showMenu());
        add(back, BorderLayout.SOUTH);
    }

    /**
     * 刷新排行榜文本。
     */
    public void updateLines(List<String> lines) {
        area.setText(String.join("\n", lines));
    }
}
