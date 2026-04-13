package ui;

import model.Constants;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * 主菜单面板。
 */
public class MainMenuPanel extends JPanel {
    public MainMenuPanel(GameFrame frame) {
        setLayout(new GridLayout(0, 1, 10, 10));
        JLabel title = new JLabel("Link Connect", JLabel.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        add(title);

        JButton easyGuest = new JButton("Guest - Easy");
        easyGuest.addActionListener(e -> frame.startGameAsGuest(Constants.Difficulty.EASY));
        add(easyGuest);

        JButton hardGuest = new JButton("Guest - Hard");
        hardGuest.addActionListener(e -> frame.startGameAsGuest(Constants.Difficulty.HARD));
        add(hardGuest);

        JButton login = new JButton("Login");
        login.addActionListener(e -> frame.showLogin());
        add(login);

        JButton register = new JButton("Register");
        register.addActionListener(e -> frame.showRegister());
        add(register);

        JButton leaderboard = new JButton("Leaderboard");
        leaderboard.addActionListener(e -> frame.showLeaderboard());
        add(leaderboard);
    }
}
