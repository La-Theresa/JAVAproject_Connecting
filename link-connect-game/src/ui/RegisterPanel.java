package ui;

import controller.GameController;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * 注册面板。
 */
public class RegisterPanel extends JPanel {
    public RegisterPanel(GameController controller) {
        setLayout(new BorderLayout());

        JPanel root = AuthUiKit.createRoot();
        add(root, BorderLayout.CENTER);

        JPanel card = AuthUiKit.createCard(566, 640);
        root.add(card);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new javax.swing.border.EmptyBorder(0, 28, 24, 28));

        card.add(AuthUiKit.createBanner("LinkIt", "Create your account and save progress."), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);

        content.add(Box.createVerticalStrut(28));

        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new java.awt.Font(AuthUiKit.BODY_FONT_FAMILY, java.awt.Font.PLAIN, 14));
        userLabel.setHorizontalAlignment(SwingConstants.CENTER);
        userLabel.setAlignmentX(CENTER_ALIGNMENT);
        content.add(userLabel);
        content.add(Box.createVerticalStrut(8));

        JTextField username = AuthUiKit.createTextField();
        username.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        content.add(username);

        content.add(Box.createVerticalStrut(18));

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new java.awt.Font(AuthUiKit.BODY_FONT_FAMILY, java.awt.Font.PLAIN, 14));
        passLabel.setHorizontalAlignment(SwingConstants.CENTER);
        passLabel.setAlignmentX(CENTER_ALIGNMENT);
        content.add(passLabel);
        content.add(Box.createVerticalStrut(8));

        JPasswordField password = AuthUiKit.createPasswordField();
        password.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        content.add(password);

        content.add(Box.createVerticalStrut(16));
        JLabel messageLabel = AuthUiKit.createErrorLabel();
        messageLabel.setForeground(new java.awt.Color(42, 157, 143));
        messageLabel.setAlignmentX(CENTER_ALIGNMENT);
        content.add(messageLabel);
        content.add(Box.createVerticalStrut(10));

        JButton register = AuthUiKit.createPrimaryButton("Create account");
        register.setAlignmentX(CENTER_ALIGNMENT);
        register.addActionListener(e -> {
            boolean ok = controller.register(username.getText().trim(), new String(password.getPassword()));
            if (ok) {
                messageLabel.setForeground(new java.awt.Color(42, 157, 143));
                messageLabel.setText("Register success. You can log in now.");
            } else {
                messageLabel.setForeground(new java.awt.Color(231, 111, 81));
                messageLabel.setText("Register failed. Username may already exist.");
            }
        });
        content.add(register);

        content.add(Box.createVerticalStrut(12));

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 6, 0, 6);

        JButton login = AuthUiKit.createTextButton("Log in");
        login.addActionListener(e -> controller.openLogin());
        actions.add(login, c);

        JButton back = AuthUiKit.createTextButton("Back to menu");
        back.addActionListener(e -> controller.goToMenu());
        actions.add(back, c);
        content.add(actions);

        content.add(Box.createVerticalGlue());
    }
}
