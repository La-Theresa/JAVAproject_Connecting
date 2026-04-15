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
 * 登录面板。
 */
public class LoginPanel extends JPanel {
    /**
     * 构造登录面板。
     * @param controller 游戏控制器
     */
    public LoginPanel(GameController controller) {
        setLayout(new BorderLayout());

        JPanel root = AuthUiKit.createRoot();
        add(root, BorderLayout.CENTER);

        JPanel card = AuthUiKit.createCard(566, 640);
        root.add(card);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new javax.swing.border.EmptyBorder(0, 28, 24, 28));

        card.add(AuthUiKit.createBanner("LinkIt", "Welcome back, sign in to continue."), BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);

        content.add(Box.createVerticalStrut(28));

        JLabel userLabel = new JLabel("Username");
        AuthUiKit.applyLocalizedLabelFont(userLabel, false, java.awt.Font.PLAIN, 14);
        userLabel.setHorizontalAlignment(SwingConstants.CENTER);
        userLabel.setAlignmentX(CENTER_ALIGNMENT);
        content.add(userLabel);
        content.add(Box.createVerticalStrut(8));

        JTextField username = AuthUiKit.createTextField();
        username.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        content.add(username);

        content.add(Box.createVerticalStrut(18));

        JLabel passLabel = new JLabel("Password");
        AuthUiKit.applyLocalizedLabelFont(passLabel, false, java.awt.Font.PLAIN, 14);
        passLabel.setHorizontalAlignment(SwingConstants.CENTER);
        passLabel.setAlignmentX(CENTER_ALIGNMENT);
        content.add(passLabel);
        content.add(Box.createVerticalStrut(8));

        JPasswordField password = AuthUiKit.createPasswordField();
        password.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        content.add(password);

        content.add(Box.createVerticalStrut(16));
        JLabel errorLabel = AuthUiKit.createErrorLabel();
        errorLabel.setAlignmentX(CENTER_ALIGNMENT);
        content.add(errorLabel);
        content.add(Box.createVerticalStrut(10));

        JButton login = AuthUiKit.createPrimaryButton("Log in");
        login.setAlignmentX(CENTER_ALIGNMENT);
        login.addActionListener(e -> {
            boolean ok = controller.login(username.getText().trim(), new String(password.getPassword()));
            if (!ok) {
                errorLabel.setText("Login failed. Please check username or password.");
            } else {
                errorLabel.setText(" ");
            }
        });
        content.add(login);

        content.add(Box.createVerticalStrut(12));

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 6, 0, 6);

        JButton register = AuthUiKit.createTextButton("Register");
        register.addActionListener(e -> controller.openRegister());
        actions.add(register, c);

        JButton back = AuthUiKit.createTextButton("Back to menu");
        back.addActionListener(e -> controller.goToMenu());
        actions.add(back, c);
        content.add(actions);

        content.add(Box.createVerticalGlue());
    }
}
