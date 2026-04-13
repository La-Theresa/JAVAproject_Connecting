package ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * 登录面板。
 */
public class LoginPanel extends JPanel {
    public LoginPanel(GameFrame frame) {
        setLayout(new GridLayout(0, 2, 8, 8));
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();

        add(new JLabel("Username"));
        add(username);
        add(new JLabel("Password"));
        add(password);

        JButton login = new JButton("Login");
        login.addActionListener(e -> {
            boolean ok = frame.login(username.getText().trim(), new String(password.getPassword()));
            if (!ok) {
                JOptionPane.showMessageDialog(frame, "Login failed");
            }
        });
        add(login);

        JButton back = new JButton("Back");
        back.addActionListener(e -> frame.showMenu());
        add(back);
    }
}
