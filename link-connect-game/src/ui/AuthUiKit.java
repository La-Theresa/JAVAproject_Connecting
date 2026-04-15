package ui;

import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 登录/注册界面共用样式组件。
 */
public final class AuthUiKit {
    // 定义初始颜色和字体常量
    public static final Color APP_BG = new Color(254, 254, 253);
    public static final Color TEXTBOX_BG = new Color(250, 249, 245);
    public static final Color BUTTON_BG = new Color(240, 238, 230);
    public static final Color BUTTON_TEXT = new Color(0, 0, 0);

    public static final String TITLE_FONT_FAMILY = resolveFontFamily("EB Garamond", "Garamond", "Serif");
    public static final String BODY_FONT_FAMILY = resolveFontFamily("Arial", "SansSerif");
        public static final String CHINESE_SERIF_FONT_FAMILY = resolveFontFamily(
            "Source Han Serif SC SemiBold",
            "Source Han Serif CN SemiBold",
            "Source Han Serif SC",
            "Source Han Serif CN",
            "Noto Serif CJK SC",
            "STSong",
            "Serif"
        );
        public static final String CHINESE_SANS_FONT_FAMILY = resolveFontFamily(
            "Source Han Sans SC SemiBold",
            "Source Han Sans CN SemiBold",
            "Source Han Sans SC",
            "Source Han Sans CN",
            "Noto Sans CJK SC",
            "Microsoft YaHei UI",
            "Microsoft YaHei",
            "PingFang SC",
            "SansSerif"
        );

    public static final Font TITLE_FONT = new Font(TITLE_FONT_FAMILY, Font.BOLD, 36);
    public static final Font BUTTON_FONT = new Font(TITLE_FONT_FAMILY, Font.BOLD, 20);
    public static final Font BODY_FONT = new Font(BODY_FONT_FAMILY, Font.PLAIN, 14);

    private AuthUiKit() {
    }

    public static JPanel createRoot() {
        return new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, APP_BG, 0, getHeight(), APP_BG));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(245, 243, 236, 90));
                g2.fillOval(-120, getHeight() - 220, 320, 320);
                g2.fillOval(getWidth() - 240, -120, 340, 340);
                g2.dispose();
                super.paintComponent(g);
            }
        };
    }

    public static JPanel createCard(int width, int height) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(211, 201, 188, 20));
                g2.fill(new RoundRectangle2D.Double(4, 8, getWidth() - 8, getHeight() - 8, 28, 28));
                g2.setColor(TEXTBOX_BG);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 8, getHeight() - 12, 24, 24));
                g2.setColor(new Color(224, 224, 224));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 9, getHeight() - 13, 24, 24));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new java.awt.BorderLayout());
        panel.setPreferredSize(new Dimension(width, height));
        panel.setMinimumSize(new Dimension(width, height));
        return panel;
    }

    public static JPanel createBanner(String title, String subtitle) {
        JPanel banner = new JPanel(new java.awt.BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(20, 20, 20), getWidth(), getHeight(), new Color(36, 36, 36)));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight() + 18, 24, 24));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        applyLocalizedLabelFont(titleLabel, true, Font.BOLD, 36);

        JLabel subLabel = new JLabel(subtitle, SwingConstants.CENTER);
        subLabel.setForeground(new Color(240, 240, 240));
        applyLocalizedLabelFont(subLabel, false, Font.PLAIN, 14);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        text.add(titleLabel);
        text.add(javax.swing.Box.createVerticalStrut(4));
        text.add(subLabel);
        banner.add(text, java.awt.BorderLayout.CENTER);
        return banner;
    }

    public static JTextField createTextField() {
        JTextField field = new JTextField();
        applyInputStyle(field);
        return field;
    }

    public static JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        applyInputStyle(field);
        return field;
    }

    public static JButton createPrimaryButton(String text) {
        JButton button = new RoundedRectTextButton(text, 14, 12, 18);
        applyLocalizedButtonFont(button, true, Font.BOLD, 20);
        return button;
    }

    public static JButton createTextButton(String text) {
        JButton button = new RoundedRectTextButton(text, 12, 8, 14);
        applyLocalizedButtonFont(button, true, Font.BOLD, 18);
        return button;
    }

    public static JLabel createErrorLabel() {
        JLabel label = new JLabel(" ", SwingConstants.CENTER);
        label.setForeground(new Color(231, 111, 81));
        applyLocalizedLabelFont(label, false, Font.PLAIN, 13);
        return label;
    }

    private static void applyInputStyle(JComponent field) {
        if (field instanceof JTextComponent) {
            applyLocalizedTextComponentFont((JTextComponent) field, false, Font.PLAIN, 16);
        } else {
            field.setFont(new Font(BODY_FONT_FAMILY, Font.PLAIN, 16));
        }
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(158, 158, 158), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));
        field.setBackground(TEXTBOX_BG);
        field.setOpaque(true);
        if (field instanceof JTextField) {
            ((JTextField) field).setHorizontalAlignment(SwingConstants.CENTER);
        }
    }

    private static String resolveFontFamily(String... candidates) {
        Set<String> available = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : candidates) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return "SansSerif";
    }

    public static Font localizedSerifFont(String text, int englishStyle, int size) {
        return localizedFont(text, true, englishStyle, size);
    }

    public static Font localizedSansFont(String text, int englishStyle, int size) {
        return localizedFont(text, false, englishStyle, size);
    }

    public static void applyLocalizedLabelFont(JLabel label, boolean serif, int englishStyle, int size) {
        label.setFont(localizedFont(label.getText(), serif, englishStyle, size));
        label.addPropertyChangeListener("text", evt ->
                label.setFont(localizedFont((String) evt.getNewValue(), serif, englishStyle, size)));
    }

    public static void applyLocalizedButtonFont(AbstractButton button, boolean serif, int englishStyle, int size) {
        button.setFont(localizedFont(button.getText(), serif, englishStyle, size));
        button.addPropertyChangeListener("text", evt ->
                button.setFont(localizedFont((String) evt.getNewValue(), serif, englishStyle, size)));
    }

    public static void applyLocalizedTextComponentFont(JTextComponent textComponent, boolean serif, int englishStyle, int size) {
        textComponent.setFont(localizedFont(textComponent.getText(), serif, englishStyle, size));
        textComponent.getDocument().addDocumentListener(new DocumentListener() {
            private void refresh() {
                textComponent.setFont(localizedFont(textComponent.getText(), serif, englishStyle, size));
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });
    }

    private static Font localizedFont(String text, boolean serif, int englishStyle, int size) {
        if (containsChinese(text)) {
            String chineseFamily = serif ? CHINESE_SERIF_FONT_FAMILY : CHINESE_SANS_FONT_FAMILY;
            // SemiBold face is encoded in family name, keep style plain to avoid over-bolding.
            return new Font(chineseFamily, Font.PLAIN, size);
        }
        String englishFamily = serif ? TITLE_FONT_FAMILY : BODY_FONT_FAMILY;
        return new Font(englishFamily, englishStyle, size);
    }

    private static boolean containsChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if ((codePoint >= 0x4E00 && codePoint <= 0x9FFF)
                    || (codePoint >= 0x3400 && codePoint <= 0x4DBF)
                    || (codePoint >= 0x20000 && codePoint <= 0x2A6DF)
                    || (codePoint >= 0x2A700 && codePoint <= 0x2B73F)
                    || (codePoint >= 0x2B740 && codePoint <= 0x2B81F)
                    || (codePoint >= 0x2B820 && codePoint <= 0x2CEAF)
                    || (codePoint >= 0xF900 && codePoint <= 0xFAFF)) {
                return true;
            }
            i += Character.charCount(codePoint);
        }
        return false;
    }

    private static final class RoundedRectTextButton extends JButton {
        private final int arc;

        private RoundedRectTextButton(String text, int arc, int vPad, int hPad) {
            super(text);
            this.arc = arc;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setForeground(BUTTON_TEXT);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setMargin(new Insets(vPad, hPad, vPad, hPad));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? new Color(251, 249, 245) : BUTTON_BG);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, arc, arc));
            g2.setColor(new Color(35, 35, 35));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, arc, arc));

            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }
}