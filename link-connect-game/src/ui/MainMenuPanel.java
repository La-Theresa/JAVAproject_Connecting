package ui;

import controller.GameController;
import model.Constants;

import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * 主菜单面板。
 */
public class MainMenuPanel extends JPanel {
    private static final Color BACKGROUND_TOP = AuthUiKit.APP_BG;
    private static final Color BACKGROUND_BOTTOM = AuthUiKit.APP_BG;
    private static final Color TEXT_MAIN = new Color(30, 30, 30);
    private static final Color TEXT_SUB = new Color(70, 70, 70);
    private static final Color ACCENT = AuthUiKit.BUTTON_BG;
    private static final Color ACCENT_ALT = new Color(251, 249, 245);
    private static final Color SURFACE = AuthUiKit.TEXTBOX_BG;
    private static final Color SURFACE_BORDER = new Color(216, 214, 207);

    private final GameController controller;
    private final RoundedChipButton authButton;
    private final RoundedChipButton leaderboardButton;
    private final JLabel greetingLabel;

    /**
     * 构造主菜单面板。
     * @param controller 游戏控制器
     */
    public MainMenuPanel(GameController controller) {
        this.controller = controller;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(18, 22, 22, 22));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel rightCluster = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightCluster.setOpaque(false);

        leaderboardButton = new RoundedChipButton("Leaderboard", ACCENT, AuthUiKit.BUTTON_TEXT, ACCENT_ALT, 150, 34);
        leaderboardButton.addActionListener(e -> controller.openLeaderboard());
        rightCluster.add(leaderboardButton);

        authButton = new RoundedChipButton("", AuthUiKit.BUTTON_BG, AuthUiKit.BUTTON_TEXT, ACCENT_ALT, 100, 34);
        authButton.addActionListener(e -> {
            if (controller.isLoggedIn()) {
                StyledDialogs.showMessage(this, "Profile", "Signed in as " + controller.getCurrentUserName(), false);
            } else {
                controller.openLogin();
            }
        });
        rightCluster.add(authButton);
        topBar.add(rightCluster, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(22, 0, 8, 0));

        JPanel hero = new JPanel(new BorderLayout());
        hero.setOpaque(false);
        hero.setBorder(new EmptyBorder(0, 16, 0, 16));

        JPanel heroCard = new JPanel(new BorderLayout(0, 18)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 32, 32));
                g2.setColor(SURFACE_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 32, 32));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        heroCard.setOpaque(false);
        heroCard.setBorder(new EmptyBorder(28, 32, 28, 32));
        heroCard.setPreferredSize(new Dimension(0, 390));

        JPanel heroContent = new JPanel(new BorderLayout(0, 18));
        heroContent.setOpaque(false);

        JLabel iconLabel = new JLabel(new MenuPngIcon("icon/icon_main.png", 154, 154), SwingConstants.CENTER);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconLabel.setBorder(new EmptyBorder(4, 0, 12, 0));
        heroContent.add(iconLabel, BorderLayout.NORTH);

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Choose how to get started", SwingConstants.CENTER);
        AuthUiKit.applyLocalizedLabelFont(title, true, Font.BOLD, 56);
        title.setForeground(TEXT_MAIN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        greetingLabel = new JLabel("Jump straight into a clean, modern match.", SwingConstants.CENTER);
        AuthUiKit.applyLocalizedLabelFont(greetingLabel, false, Font.PLAIN, 16);
        greetingLabel.setForeground(TEXT_SUB);
        greetingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textBlock.add(Box.createVerticalStrut(4));
        textBlock.add(title);
        textBlock.add(Box.createVerticalStrut(8));
        textBlock.add(greetingLabel);
        textBlock.add(Box.createVerticalStrut(4));
        heroContent.add(textBlock, BorderLayout.CENTER);
        heroCard.add(heroContent, BorderLayout.CENTER);
        hero.add(heroCard, BorderLayout.CENTER);
        center.add(hero, BorderLayout.NORTH);

        JPanel actionArea = new JPanel(new BorderLayout(0, 14));
        actionArea.setOpaque(false);
        actionArea.setBorder(new EmptyBorder(0, 18, 0, 18));

        JPanel primaryRow = new JPanel(new GridLayout(1, 2, 16, 0));
        primaryRow.setOpaque(false);
        primaryRow.add(createStartCard("Easy", "4 × 4 board · 60 sec · choose theme", ACCENT, e -> controller.startGame(Constants.Difficulty.EASY)));
        primaryRow.add(createStartCard("Hard", "10 × 10 board · 120 sec · choose theme", ACCENT, e -> controller.startGame(Constants.Difficulty.HARD)));
        actionArea.add(primaryRow, BorderLayout.NORTH);

        JPanel secondaryRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        secondaryRow.setOpaque(false);
        secondaryRow.add(createLinkButton("Log in", e -> controller.openLogin()));
        secondaryRow.add(createLinkButton("Register", e -> controller.openRegister()));
        actionArea.add(secondaryRow, BorderLayout.SOUTH);
        center.add(actionArea, BorderLayout.CENTER);

        JPanel bottomArtworkWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomArtworkWrap.setOpaque(false);
        bottomArtworkWrap.add(new BottomArtworkPanel());
        center.add(bottomArtworkWrap, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
        refreshState();
    }

    /**
     * 刷新界面状态，根据登录状态显示不同内容。
     */
    public void refreshState() {
        if (controller.isLoggedIn()) {
            authButton.setText(controller.getCurrentUserInitial());
            authButton.setToolTipText("Signed in as " + controller.getCurrentUserName());
            authButton.setAvatarMode(true);
            authButton.setPreferredSize(new Dimension(56, 36));
            greetingLabel.setText("Welcome back, " + controller.getCurrentUserName() + ".");
        } else {
            authButton.setText("Log in");
            authButton.setToolTipText("Log in to save your progress");
            authButton.setAvatarMode(false);
            authButton.setPreferredSize(new Dimension(100, 34));
            greetingLabel.setText("Jump straight into a clean, modern match.");
        }
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();
        g2.setPaint(new java.awt.GradientPaint(0, 0, BACKGROUND_TOP, 0, height, BACKGROUND_BOTTOM));
        g2.fillRect(0, 0, width, height);
        g2.setColor(new Color(245, 243, 236, 90));
        g2.fillOval(width - 260, -120, 360, 360);
        g2.fillOval(-140, height - 220, 280, 280);
        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * 创建文本链接按钮。
     * @param text 按钮文本
     * @param listener 动作监听器
     * @return 样式化的按钮
     */
    private RoundedChipButton createLinkButton(String text, ActionListener listener) {
        RoundedChipButton button = new RoundedChipButton(text, AuthUiKit.BUTTON_BG, AuthUiKit.BUTTON_TEXT, ACCENT_ALT, 100, 34);
        button.setBorderColor(SURFACE_BORDER);
        button.addActionListener(listener);
        return button;
    }

    /**
     * 创建开始游戏卡片按钮。
     * @param title 难度标题
     * @param subtitle 难度描述
     * @param baseColor 按钮基础颜色
     * @param listener 动作监听器
     * @return 样式化的开始按钮
     */
    private RoundedStartButton createStartCard(String title, String subtitle, Color baseColor, ActionListener listener) {
        RoundedStartButton button = new RoundedStartButton(title, subtitle, baseColor);
        button.addActionListener(listener);
        return button;
    }

    /**
     * 底部装饰图，自适应当前尺寸重绘。
     */
    private static class BottomArtworkPanel extends JPanel {
        private BufferedImage cachedImage;
        private int cachedWidth = -1;
        private int cachedHeight = -1;

        BottomArtworkPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(120, 116));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            if (cachedImage == null || cachedWidth != width || cachedHeight != height) {
                cachedImage = ThemePngIconLoader.loadImageFromFile("icon/icon_main_bottom.png", width, height);
                cachedWidth = width;
                cachedHeight = height;
            }
            if (cachedImage == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.drawImage(cachedImage, 0, 0, width, height, null);
            g2.dispose();
        }
    }

    private static class MenuPngIcon implements javax.swing.Icon {
        private final String relativePath;
        private final int width;
        private final int height;
        private BufferedImage image;

        MenuPngIcon(String relativePath, int width, int height) {
            this.relativePath = relativePath;
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (image == null) {
                image = ThemePngIconLoader.loadImageFromFile(relativePath, width, height);
            }
            if (image != null) {
                g.drawImage(image, x, y, null);
            }
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }

    /**
     * 圆角扁平按钮，支持头像模式。
     */
    private static class RoundedChipButton extends JButton {
        private final Color backgroundColor;
        private final Color hoverColor;
        private Color borderColor = new Color(0, 0, 0, 0);
        private boolean avatarMode;

        /**
         * 构造圆角按钮。
         * @param text 按钮文本
         * @param backgroundColor 背景色
         * @param foregroundColor 前景色
         * @param hoverColor 悬停色
         */
        RoundedChipButton(String text, Color backgroundColor, Color foregroundColor, Color hoverColor, int width, int height) {
            super(text);
            this.backgroundColor = backgroundColor;
            this.hoverColor = hoverColor;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setForeground(foregroundColor);
            AuthUiKit.applyLocalizedButtonFont(this, true, Font.BOLD, 16);
            setMargin(new java.awt.Insets(10, 16, 10, 16));
            setPreferredSize(new Dimension(width, height));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
        }

        /**
         * 设置边框颜色。
         * @param borderColor 边框颜色
         */
        void setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
        }

        /**
         * 设置头像模式（圆形显示）。
         * @param avatarMode 是否为头像模式
         */
        void setAvatarMode(boolean avatarMode) {
            this.avatarMode = avatarMode;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? hoverColor : backgroundColor);
            int arc = avatarMode ? 12 : 14;
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, arc, arc));
            if (borderColor.getAlpha() > 0) {
                g2.setColor(borderColor);
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, arc, arc));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * 圆角渐变开始按钮。
     */
    private static class RoundedStartButton extends JButton {
        private final String title;
        private final String subtitle;
        private final Color baseColor;

        /**
         * 构造开始游戏按钮。
         * @param title 标题
         * @param subtitle 副标题
         * @param baseColor 基础颜色
         */
        RoundedStartButton(String title, String subtitle, Color baseColor) {
            super("");
            this.title = title;
            this.subtitle = subtitle;
            this.baseColor = baseColor;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setForeground(AuthUiKit.BUTTON_TEXT);
            AuthUiKit.applyLocalizedButtonFont(this, true, Font.BOLD, 22);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setMargin(new java.awt.Insets(18, 20, 18, 20));
            setPreferredSize(new Dimension(204, 92));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? baseColor.brighter() : baseColor);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 28, 28));
            g2.setColor(AuthUiKit.BUTTON_TEXT);
            Font titleFont = AuthUiKit.localizedSerifFont(title, Font.BOLD, 22);
            Font subFont = AuthUiKit.localizedSansFont(subtitle, Font.PLAIN, 13);
            g2.setFont(titleFont);
            java.awt.FontMetrics titleFm = g2.getFontMetrics(titleFont);
            g2.setFont(subFont);
            java.awt.FontMetrics subFm = g2.getFontMetrics(subFont);

            int totalHeight = titleFm.getHeight() + 4 + subFm.getHeight();
            int startY = (getHeight() - totalHeight) / 2;

            g2.setFont(titleFont);
            int titleX = (getWidth() - titleFm.stringWidth(title)) / 2;
            int titleY = startY + titleFm.getAscent();
            g2.drawString(title, titleX, titleY);

            g2.setFont(subFont);
            int subX = (getWidth() - subFm.stringWidth(subtitle)) / 2;
            int subY = titleY + 4 + subFm.getAscent();
            g2.drawString(subtitle, subX, subY);
            g2.dispose();
        }
    }
}