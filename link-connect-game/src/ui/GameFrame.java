package ui;

import data.RecordManager;
import data.SaveManager;
import data.UserManager;
import logic.BoardGenerator;
import logic.DeadEndDetector;
import logic.PathFinder;
import model.Constants;
import model.GameBoard;
import model.GameSession;
import model.GameSnapshot;
import model.Position;
import model.User;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.io.IOException;

/**
 * 主窗口，负责面板切换与业务编排。
 */
public class GameFrame extends JFrame {
    private static final String CARD_MENU = "menu";
    private static final String CARD_LOGIN = "login";
    private static final String CARD_REGISTER = "register";
    private static final String CARD_GAME = "game";
    private static final String CARD_LEADERBOARD = "leaderboard";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);

    private final UserManager userManager = new UserManager();
    private final RecordManager recordManager = new RecordManager(userManager);
    private final SaveManager saveManager = new SaveManager();

    private final PathFinder pathFinder = new PathFinder();
    private final DeadEndDetector deadEndDetector = new DeadEndDetector(pathFinder);
    private final BoardGenerator boardGenerator = new BoardGenerator(pathFinder);

    private final MainMenuPanel menuPanel = new MainMenuPanel(this);
    private final LoginPanel loginPanel = new LoginPanel(this);
    private final RegisterPanel registerPanel = new RegisterPanel(this);
    private final LeaderboardPanel leaderboardPanel = new LeaderboardPanel(this);

    private final JPanel gameContainer = new JPanel(new BorderLayout());
    private final GamePanel gamePanel = new GamePanel(this);
    private final JLabel statusLabel = new JLabel("User: Guest");
    private final JLabel scoreLabel = new JLabel("Score: 0");
    private final JLabel timeLabel = new JLabel("Time: 0");
    private final JLabel comboLabel = new JLabel("Combo: 0");
    private final JLabel operationLabel = new JLabel("Ready");

    private User currentUser;
    private GameSession session;
    private Timer timer;

    public GameFrame() {
        super("Link Connect Game");
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
        showMenu();
    }

    /**
     * 展示主菜单。
     */
    public void showMenu() {
        cardLayout.show(root, CARD_MENU);
    }

    /**
     * 展示登录面板。
     */
    public void showLogin() {
        cardLayout.show(root, CARD_LOGIN);
    }

    /**
     * 展示注册面板。
     */
    public void showRegister() {
        cardLayout.show(root, CARD_REGISTER);
    }

    /**
     * 展示排行榜。
     */
    public void showLeaderboard() {
        leaderboardPanel.updateLines(recordManager.topN(10));
        cardLayout.show(root, CARD_LEADERBOARD);
    }

    /**
     * 游客模式启动新游戏。
     */
    public void startGameAsGuest(Constants.Difficulty difficulty) {
        currentUser = null;
        startNewSession(difficulty);
    }

    /**
     * 注册账号。
     */
    public boolean register(String username, String password) {
        return userManager.register(username, password);
    }

    /**
     * 登录并进入难度选择。
     */
    public boolean login(String username, String password) {
        User user = userManager.login(username, password);
        if (user == null) {
            return false;
        }
        currentUser = user;
        Object[] options = {"Easy", "Hard"};
        int index = JOptionPane.showOptionDialog(
                this,
                "Choose difficulty",
                "Difficulty",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );
        Constants.Difficulty difficulty = index == 1 ? Constants.Difficulty.HARD : Constants.Difficulty.EASY;
        startNewSession(difficulty);
        return true;
    }

    /**
     * 刷新HUD显示。
     */
    public void refreshHud() {
        if (session == null) {
            return;
        }
        String userText = currentUser == null ? "Guest" : currentUser.username();
        statusLabel.setText("User: " + userText + " | Difficulty: " + session.difficulty().name());
        scoreLabel.setText("Score: " + session.score());
        timeLabel.setText("Time: " + session.timeLeft());
        comboLabel.setText("Combo: " + session.comboCount());
        operationLabel.setText(session.lastOperation());
    }

    /**
     * 检查胜负并弹窗。
     */
    public void checkGameStateAndPrompt() {
        if (session == null) {
            return;
        }
        if (session.hasWon()) {
            onGameFinished("Victory");
            return;
        }
        if (session.hasLost()) {
            onGameFinished("Defeat");
        }
    }

    private void buildGameCard() {
        JPanel hud = new JPanel(new GridLayout(2, 4));
        hud.add(statusLabel);
        hud.add(scoreLabel);
        hud.add(timeLabel);
        hud.add(comboLabel);
        hud.add(operationLabel);

        JButton hintBtn = new JButton("Hint");
        hintBtn.addActionListener(e -> {
            Position[] pair = session == null ? null : session.hintPair();
            gamePanel.showHint(pair);
            if (pair == null) {
                operationLabel.setText("No available pair");
            }
        });
        hud.add(hintBtn);

        JButton shuffleBtn = new JButton("Shuffle");
        shuffleBtn.addActionListener(e -> {
            if (session == null) {
                return;
            }
            boardGenerator.reshuffle(session.board());
            refreshHud();
            gamePanel.repaint();
        });
        hud.add(shuffleBtn);

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveCurrentGame());
        hud.add(saveBtn);

        JButton loadBtn = new JButton("Load");
        loadBtn.addActionListener(e -> loadGameForCurrentUser());
        hud.add(loadBtn);

        gameContainer.add(hud, BorderLayout.NORTH);
        gameContainer.add(gamePanel, BorderLayout.CENTER);
    }

    private void startNewSession(Constants.Difficulty difficulty) {
        GameBoard board = boardGenerator.generate(difficulty);
        session = new GameSession(difficulty, board, pathFinder, deadEndDetector);
        gamePanel.bindSession(session);
        startTimer();
        refreshHud();
        cardLayout.show(root, CARD_GAME);
    }

    private void startTimer() {
        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(Constants.TIMER_TICK_MS, e -> {
            if (session == null) {
                return;
            }
            session.tickSecond();
            refreshHud();
            checkGameStateAndPrompt();
        });
        timer.start();
    }

    private void onGameFinished(String title) {
        if (timer != null) {
            timer.stop();
        }
        if (currentUser != null && session != null) {
            userManager.recordGame(currentUser, session.score());
        }
        JOptionPane.showMessageDialog(this, title + " | Score: " + session.score());
        showMenu();
    }

    private void saveCurrentGame() {
        if (session == null) {
            return;
        }
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Guest mode cannot save");
            return;
        }
        try {
            saveManager.save(session.toSnapshot(currentUser.username()));
            operationLabel.setText("Saved");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Save failed");
        }
    }

    private void loadGameForCurrentUser() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please login first");
            return;
        }
        try {
            GameSnapshot snapshot = saveManager.load(currentUser.username());
            if (snapshot == null) {
                JOptionPane.showMessageDialog(this, "No save file");
                return;
            }
            session = GameSession.fromSnapshot(snapshot, pathFinder, deadEndDetector);
            gamePanel.bindSession(session);
            startTimer();
            refreshHud();
            cardLayout.show(root, CARD_GAME);
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Invalid save file");
            showMenu();
        }
    }
}
