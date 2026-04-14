package ui;

import controller.GameController;
import data.SaveManager;
import model.Constants;
import model.GameSession;
import model.Position;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * 主窗口，仅负责界面组合、卡片切换和视图刷新。
 */
public class GameFrame extends JFrame implements GameView {
    private static final String CARD_MENU = "menu";
    private static final String CARD_LOGIN = "login";
    private static final String CARD_REGISTER = "register";
    private static final String CARD_GAME = "game";
    private static final String CARD_LEADERBOARD = "leaderboard";

    private final GameController controller;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);

    private final MainMenuPanel menuPanel;
    private final LoginPanel loginPanel;
    private final RegisterPanel registerPanel;
    private final LeaderboardPanel leaderboardPanel;

    private final JPanel gameContainer = new JPanel(new BorderLayout());
    private final GamePanel gamePanel;
    private final JLabel statusLabel = new JLabel("User: Guest");
    private final JLabel scoreLabel = new JLabel("Score: 0");
    private final JLabel timeLabel = new JLabel("Time: 0");
    private final JLabel comboLabel = new JLabel("Combo: 0");
    private final JLabel operationLabel = new JLabel("Ready");

    /**
     * 构造主窗口并初始化所有UI组件。
     * @param controller 游戏控制器
     */
    public GameFrame(GameController controller) {
        super("Link Connect Game");
        this.controller = controller;
        this.controller.attachView(this);
        this.gamePanel = new GamePanel(controller);
        this.menuPanel = new MainMenuPanel(controller);
        this.loginPanel = new LoginPanel(controller);
        this.registerPanel = new RegisterPanel(controller);
        this.leaderboardPanel = new LeaderboardPanel(controller);

        setSize(Constants.FRAME_WIDTH, Constants.FRAME_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildGameCard();
        root.add(menuPanel, CARD_MENU);
        root.add(loginPanel, CARD_LOGIN);
        root.add(registerPanel, CARD_REGISTER);
        root.add(gameContainer, CARD_GAME);
        root.add(leaderboardPanel, CARD_LEADERBOARD);

        setContentPane(root);
        root.setBackground(AuthUiKit.APP_BG);
        gameContainer.setBackground(AuthUiKit.APP_BG);
        showMenuCard();
    }

    /**
     * 展示主菜单卡片。
     */
    @Override
    public void showMenuCard() {
        menuPanel.refreshState();
        showCard(CARD_MENU);
    }

    /**
     * 展示登录卡片。
     */
    @Override
    public void showLoginCard() {
        showCard(CARD_LOGIN);
    }

    /**
     * 展示注册卡片。
     */
    @Override
    public void showRegisterCard() {
        showCard(CARD_REGISTER);
    }

    /**
     * 展示游戏卡片。
     */
    @Override
    public void showGameCard() {
        showCard(CARD_GAME);
    }

    /**
     * 展示排行榜卡片。
     */
    @Override
    public void showLeaderboardCard() {
        showCard(CARD_LEADERBOARD);
    }

    /**
     * 切换显示指定卡片。
     * @param cardName 卡片名称
     */
    private void showCard(String cardName) {
        cardLayout.show(root, cardName);
        root.revalidate();
        root.repaint();
    }

    /**
     * 绑定游戏会话到游戏面板。
     * @param session 游戏会话
     */
    @Override
    public void setGameSession(GameSession session) {
        gamePanel.bindSession(session);
    }

    /**
     * 设置提示高亮方块对。
     * @param pair 提示对位置数组
     */
    @Override
    public void setHintPair(Position[] pair) {
        gamePanel.setHintPair(pair);
    }

    /**
     * 更新HUD显示区域。
     * @param userText 用户信息
     * @param difficultyText 难度文本
     * @param score 当前分数
     * @param timeLeft 剩余时间
     * @param combo 连击数
     * @param operation 操作信息
     */
    @Override
    public void updateHud(String userText, String difficultyText, int score, int timeLeft, int combo, String operation) {
        statusLabel.setText("User: " + userText + " | Difficulty: " + difficultyText);
        scoreLabel.setText("Score: " + score);
        timeLabel.setText("Time: " + timeLeft);
        comboLabel.setText("Combo: " + combo);
        operationLabel.setText(operation);
    }

    /**
     * 更新排行榜显示内容。
     * @param lines 排行榜文本行
     */
    @Override
    public void updateLeaderboard(List<String> lines) {
        leaderboardPanel.updateLines(lines);
    }

    /**
     * 触发游戏面板重绘。
     */
    @Override
    public void repaintGame() {
        gamePanel.repaint();
    }

    /**
     * 弹出难度选择对话框。
     * @return 选择的难度
     */
    @Override
    public Constants.Difficulty chooseDifficulty() {
        return StyledDialogs.chooseDifficulty(this);
    }

    /**
     * 弹出存档槽选择对话框。
     * @param slots 可用存档列表
     * @return 选中的存档槽，未选择返回null
     */
    @Override
    public SaveManager.SaveSlot chooseSaveSlot(List<SaveManager.SaveSlot> slots) {
        return StyledDialogs.chooseSaveSlot(this, slots);
    }

    /**
     * 弹出信息提示对话框。
     * @param title 对话框标题
     * @param message 提示信息
     */
    @Override
    public void showInfoMessage(String title, String message) {
        StyledDialogs.showMessage(this, title, message, false);
    }

    /**
     * 弹出错误提示对话框。
     * @param title 对话框标题
     * @param message 错误信息
     */
    @Override
    public void showErrorMessage(String title, String message) {
        StyledDialogs.showMessage(this, title, message, true);
    }

    /**
     * 展示排行榜。
     */
    public void showLeaderboard() {
        controller.openLeaderboard();
    }

    /**
     * 构建游戏界面：HUD + 棋盘面板。
     */
    private void buildGameCard() {
        JPanel hud = new JPanel(new BorderLayout(0, 8));
        hud.setBackground(AuthUiKit.APP_BG);

        JPanel infoRow = new JPanel(new GridLayout(1, 5, 10, 0));
        infoRow.setBackground(AuthUiKit.APP_BG);
        styleHudLabel(statusLabel);
        styleHudLabel(scoreLabel);
        styleHudLabel(timeLabel);
        styleHudLabel(comboLabel);
        styleHudLabel(operationLabel);
        infoRow.add(statusLabel);
        infoRow.add(scoreLabel);
        infoRow.add(timeLabel);
        infoRow.add(comboLabel);
        infoRow.add(operationLabel);

        JPanel actionRow = new JPanel(new GridBagLayout());
        actionRow.setBackground(AuthUiKit.APP_BG);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 12, 0, 12);
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.CENTER;

        JButton hintBtn = createDebouncedButton("Hint", 220, e -> controller.requestHint());
        c.gridx = 0;
        actionRow.add(hintBtn, c);

        JButton shuffleBtn = createDebouncedButton("Shuffle", 220, e -> controller.shuffleBoard());
        c.gridx = 1;
        actionRow.add(shuffleBtn, c);

        JButton saveBtn = createDebouncedButton("Save", 500, e -> controller.saveCurrentGame());
        c.gridx = 2;
        actionRow.add(saveBtn, c);

        JButton loadBtn = createDebouncedButton("Load", 500, e -> controller.loadCurrentGame());
        c.gridx = 3;
        actionRow.add(loadBtn, c);

        JButton exitBtn = createDebouncedButton("Exit", 500, e -> controller.exitCurrentGame());
        c.gridx = 4;
        actionRow.add(exitBtn, c);

        hud.add(infoRow, BorderLayout.NORTH);
        hud.add(actionRow, BorderLayout.CENTER);

        gameContainer.add(hud, BorderLayout.NORTH);
        gameContainer.add(gamePanel, BorderLayout.CENTER);
    }

    /**
     * 创建带防抖功能的按钮，防止快速连续点击。
     * @param text 按钮文本
     * @param debounceMs 防抖时间（毫秒）
     * @param action 点击后执行的动作
     * @return 配置好的按钮
     */
    private JButton createDebouncedButton(String text, int debounceMs, ActionListener action) {
        JButton button = AuthUiKit.createPrimaryButton(text);
        button.setPreferredSize(new Dimension(116, 36));
        button.addActionListener((ActionEvent e) -> {
            if (!button.isEnabled()) {
                return;
            }
            button.setEnabled(false);
            action.actionPerformed(e);
            Timer timer = new Timer(debounceMs, ignored -> button.setEnabled(true));
            timer.setRepeats(false);
            timer.start();
        });
        return button;
    }

    private void styleHudLabel(JLabel label) {
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(AuthUiKit.TEXTBOX_BG);
        label.setFont(new Font(AuthUiKit.BODY_FONT_FAMILY, Font.PLAIN, 14));
    }
}
