package ui;

import data.SaveManager;
import model.Constants;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 项目内统一风格弹窗组件，避免系统对话框破坏视觉一致性。
 */
public final class StyledDialogs {
    private StyledDialogs() {
    }

    public static void showMessage(Component parent, String title, String message, boolean error) {
        JDialog dialog = createBaseDialog(parent, title, 520, 260);
        JPanel content = createBodyContainer();

        JLabel msg = new JLabel("<html><div style='text-align:center;'>" + escape(message) + "</div></html>", SwingConstants.CENTER);
        AuthUiKit.applyLocalizedLabelFont(msg, false, Font.PLAIN, 15);
        msg.setForeground(error ? new java.awt.Color(170, 44, 44) : new java.awt.Color(30, 30, 30));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        msg.setHorizontalAlignment(SwingConstants.CENTER);
        msg.setVerticalAlignment(SwingConstants.CENTER);

        content.add(Box.createVerticalGlue());
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.add(msg, BorderLayout.CENTER);
        content.add(line);
        content.add(Box.createVerticalStrut(22));

        javax.swing.JButton ok = AuthUiKit.createPrimaryButton("OK");
        ok.setAlignmentX(Component.CENTER_ALIGNMENT);
        ok.addActionListener(e -> dialog.dispose());
        ok.setPreferredSize(new Dimension(140, 42));
        content.add(ok);
        content.add(Box.createVerticalGlue());

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    public static Constants.Difficulty chooseDifficulty(Component parent) {
        JDialog dialog = createBaseDialog(parent, "Difficulty", 560, 300);
        JPanel content = createBodyContainer();

        JLabel title = new JLabel("Choose difficulty", SwingConstants.CENTER);
        AuthUiKit.applyLocalizedLabelFont(title, true, Font.BOLD, 32);
        title.setForeground(new java.awt.Color(20, 20, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JRadioButton easy = createRadio("Easy", true);
        JRadioButton hard = createRadio("Hard", false);
        ButtonGroup group = new ButtonGroup();
        group.add(easy);
        group.add(hard);

        JPanel options = new JPanel();
        options.setOpaque(false);
        options.add(easy);
        options.add(Box.createHorizontalStrut(18));
        options.add(hard);

        AtomicReference<Constants.Difficulty> result = new AtomicReference<>(Constants.Difficulty.EASY);

        javax.swing.JButton confirm = AuthUiKit.createPrimaryButton("Confirm");
        confirm.addActionListener(e -> {
            result.set(hard.isSelected() ? Constants.Difficulty.HARD : Constants.Difficulty.EASY);
            dialog.dispose();
        });

        javax.swing.JButton cancel = AuthUiKit.createTextButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(confirm);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(cancel);

        content.add(Box.createVerticalStrut(18));
        content.add(title);
        content.add(Box.createVerticalStrut(18));
        content.add(options);
        content.add(Box.createVerticalStrut(22));
        content.add(actions);
        content.add(Box.createVerticalGlue());

        dialog.setContentPane(content);
        dialog.setVisible(true);
        return result.get();
    }

    public static Constants.Theme chooseTheme(Component parent) {
        JDialog dialog = createBaseDialog(parent, "Theme", 560, 420);
        JPanel content = createBodyContainer();

        JLabel title = new JLabel("Choose theme", SwingConstants.CENTER);
        AuthUiKit.applyLocalizedLabelFont(title, true, Font.BOLD, 32);
        title.setForeground(new java.awt.Color(20, 20, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JRadioButton theme1 = createRadio("Theme 1 (Icons)", true);
        JRadioButton theme2 = createRadio("Theme 2 (English-Chinese)", false);
        JRadioButton theme3 = createRadio("Theme 3 (Poems)", false);
        JRadioButton theme4 = createRadio("Theme 4 (YAU)", false);
        ButtonGroup group = new ButtonGroup();
        group.add(theme1);
        group.add(theme2);
        group.add(theme3);
        group.add(theme4);

        JPanel options = new JPanel();
        options.setOpaque(false);
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        theme1.setAlignmentX(Component.CENTER_ALIGNMENT);
        theme2.setAlignmentX(Component.CENTER_ALIGNMENT);
        theme3.setAlignmentX(Component.CENTER_ALIGNMENT);
        theme4.setAlignmentX(Component.CENTER_ALIGNMENT);
        options.add(theme1);
        options.add(Box.createVerticalStrut(8));
        options.add(theme2);
        options.add(Box.createVerticalStrut(8));
        options.add(theme3);
        options.add(Box.createVerticalStrut(8));
        options.add(theme4);

        AtomicReference<Constants.Theme> result = new AtomicReference<>(null);

        javax.swing.JButton confirm = AuthUiKit.createPrimaryButton("Start");
        confirm.addActionListener(e -> {
            if (theme4.isSelected()) {
                result.set(Constants.Theme.THEME4);
            } else if (theme3.isSelected()) {
                result.set(Constants.Theme.THEME3);
            } else if (theme2.isSelected()) {
                result.set(Constants.Theme.THEME2);
            } else {
                result.set(Constants.Theme.THEME1);
            }
            dialog.dispose();
        });

        javax.swing.JButton cancel = AuthUiKit.createTextButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(confirm);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(cancel);

        content.add(Box.createVerticalStrut(18));
        content.add(title);
        content.add(Box.createVerticalStrut(16));
        content.add(options);
        content.add(Box.createVerticalStrut(20));
        content.add(actions);
        content.add(Box.createVerticalGlue());

        dialog.setContentPane(content);
        dialog.setVisible(true);
        return result.get();
    }

    public static SaveManager.SaveSlot chooseSaveSlot(Component parent, List<SaveManager.SaveSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return null;
        }
        JDialog dialog = createBaseDialog(parent, "Load", 680, 420);
        JPanel root = createBodyContainer();

        JLabel title = new JLabel("Select a save to load", SwingConstants.CENTER);
        AuthUiKit.applyLocalizedLabelFont(title, true, Font.BOLD, 30);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        javax.swing.DefaultListModel<String> model = new javax.swing.DefaultListModel<>();
        for (SaveManager.SaveSlot slot : slots) {
            model.addElement(slot.displayName());
        }
        javax.swing.JList<String> list = new javax.swing.JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setFont(AuthUiKit.localizedSansFont(model.isEmpty() ? "" : model.get(0), Font.PLAIN, 14));
        list.setBackground(AuthUiKit.TEXTBOX_BG);
        list.setFixedCellHeight(30);
        JScrollPane pane = new JScrollPane(list);
        pane.setPreferredSize(new Dimension(600, 220));
        pane.setBorder(BorderFactory.createLineBorder(new java.awt.Color(210, 208, 200), 1, true));

        AtomicReference<SaveManager.SaveSlot> result = new AtomicReference<>(null);

        javax.swing.JButton loadBtn = AuthUiKit.createPrimaryButton("Load");
        loadBtn.addActionListener(e -> {
            int index = list.getSelectedIndex();
            if (index >= 0 && index < slots.size()) {
                result.set(slots.get(index));
            }
            dialog.dispose();
        });

        javax.swing.JButton cancel = AuthUiKit.createTextButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(loadBtn);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(cancel);

        root.add(Box.createVerticalStrut(16));
        root.add(title);
        root.add(Box.createVerticalStrut(14));
        root.add(pane);
        root.add(Box.createVerticalStrut(16));
        root.add(actions);
        root.add(Box.createVerticalGlue());

        dialog.setContentPane(root);
        dialog.setVisible(true);
        return result.get();
    }

    private static JDialog createBaseDialog(Component parent, String title, int width, int height) {
        java.awt.Window owner = parent == null ? null : javax.swing.SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        return dialog;
    }

    private static JPanel createBodyContainer() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(AuthUiKit.APP_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));
        return panel;
    }

    private static JRadioButton createRadio(String text, boolean selected) {
        JRadioButton radio = new JRadioButton(text, selected);
        radio.setOpaque(false);
        AuthUiKit.applyLocalizedButtonFont(radio, true, Font.BOLD, 15);
        return radio;
    }

    private static String escape(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");
    }
}