package ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * 注册面板。
 */
public class RegisterPanel extends JPanel {
    public RegisterPanel(GameFrame frame) {
        setLayout(new GridLayout(0, 2, 8, 8));
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();

        add(new JLabel("Username"));
        add(username);
        add(new JLabel("Password"));
        add(password);

        JButton register = new JButton("Register");
        register.addActionListener(e -> {
            boolean ok = frame.register(username.getText().trim(), new String(password.getPassword()));
            JOptionPane.showMessageDialog(frame, ok ? "Register success" : "Register failed");
        });
        add(register);

        JButton back = new JButton("Back");
        back.addActionListener(e -> frame.showMenu());
        add(back);
    }
}
