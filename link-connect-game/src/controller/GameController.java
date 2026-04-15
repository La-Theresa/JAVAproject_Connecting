package controller;

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
import ui.GameView;

import javax.swing.Timer;
import java.io.IOException;
import java.util.List;

/**
 * 游戏控制器，负责业务流程、状态推进和视图调度。
 */
public class GameController {
    private static final long SAVE_CLICK_COOLDOWN_MS = 700;
    private static final long LOAD_CLICK_COOLDOWN_MS = 700;

    private final UserManager userManager = new UserManager();
    private final RecordManager recordManager = new RecordManager(userManager);
    private final SaveManager saveManager = new SaveManager();
    private final PathFinder pathFinder = new PathFinder();
    private final DeadEndDetector deadEndDetector = new DeadEndDetector(pathFinder);
    private final BoardGenerator boardGenerator = new BoardGenerator(pathFinder);

    private GameView view;
    private User currentUser;
    private GameSession session;
    private Timer timer;
    private Position selectedFirst;
    private Position selectedSecond;
    private Position[] hintPair;
    private long lastSaveClickAt;
    private long lastLoadClickAt;
    private boolean finishing;

    /**
     * 绑定视图接口，实现MVC模式。
     * @param view 游戏视图实现
     */
    public void attachView(GameView view) {
        this.view = view;
    }

    /**
     * 打开登录界面。
     */
    public void openLogin() {
        if (view != null) {
            view.showLoginCard();
        }
    }

    /**11
     * 打开注册界面。
     */
    public void openRegister() {
        if (view != null) {
            view.showRegisterCard();
        }
    }

    /**
     * 打开排行榜界面。
     */
    public void openLeaderboard() {
        if (view != null) {
            view.updateLeaderboard(recordManager.topN(10));
            view.showLeaderboardCard();
        }
    }

    /**
     * 返回主菜单，停止游戏会话。
     */
    public void goToMenu() {
        stopTimer();
        finishing = false;
        session = null;
        clearSelections();
        if (view != null) {
            view.setGameSession(null);
            view.setHintPair(null);
            view.showMenuCard();
            view.repaintGame();
        }
    }

    /**
     * 以游客模式开始游戏。
     * @param difficulty 游戏难度
     */
    public void startGuestGame(Constants.Difficulty difficulty) {
        currentUser = null;
        Constants.Theme theme = view == null ? Constants.Theme.THEME1 : view.chooseTheme();
        if (theme == null) {
            return;
        }
        startNewSession(difficulty, theme);
    }

    /**
     * 以登录用户身份开始游戏。
     * @param difficulty 游戏难度
     */
    public void startGame(Constants.Difficulty difficulty) {
        Constants.Theme theme = view == null ? Constants.Theme.THEME1 : view.chooseTheme();
        if (theme == null) {
            return;
        }
        startNewSession(difficulty, theme);
    }

    /**
     * 注册新用户。
     * @param username 用户名
     * @param password 密码
     * @return 注册成功返回true
     */
    public boolean register(String username, String password) {
        return userManager.register(username, password);
    }

    /**
     * 用户登录。
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回true
     */
    public boolean login(String username, String password) {
        User user = userManager.login(username, password);
        if (user == null) {
            return false;
        }
        currentUser = user;
        if (view != null) {
            view.showMenuCard();
        }
        return true;
    }

    /**
     * 请求提示，显示一对可消除方块。
     */
    public void requestHint() {
        if (session == null) {
            return;
        }
        hintPair = deadEndDetector.findAnyPair(session.board());
        if (view != null) {
            view.setHintPair(hintPair);
            view.repaintGame();
            if (hintPair == null) {
                view.showInfoMessage("Hint", "No available pair");
            }
        }
    }

    /**
     * 打乱当前棋盘剩余方块。
     */
    public void shuffleBoard() {
        if (session == null) {
            return;
        }
        boardGenerator.reshuffle(session.board());
        // The board layout changed — invalidate the cached dead-end result so
        // the next hasLost() call re-evaluates valid moves on the new layout.
        session.invalidateMoveCache();
        clearSelections();
        if (view != null) {
            view.setHintPair(null);
            refreshHud();
            view.repaintGame();
        }
    }

    /**
     * 保存当前游戏进度。
     */
    public void saveCurrentGame() {
        if (isTooFrequent(true)) {
            return;
        }
        if (currentUser == null) {
            if (view != null) {
                view.showErrorMessage("Save", "Guest mode cannot save");
            }
            return;
        }
        if (session == null) {
            return;
        }
        try {
            SaveManager.SaveSlot slot = saveManager.save(session.toSnapshot(currentUser.username()));
            if (view != null) {
                view.showInfoMessage("Save", "Game saved: " + slot.displayName());
            }
        } catch (IOException e) {
            if (view != null) {
                view.showErrorMessage("Save", "Save failed");
            }
        }
    }

    /**
     * 加载并恢复游戏进度。
     */
    public void loadCurrentGame() {
        if (isTooFrequent(false)) {
            return;
        }
        if (currentUser == null) {
            if (view != null) {
                view.showErrorMessage("Load", "Please login first");
            }
            return;
        }
        try {
            stopTimer();
            List<SaveManager.SaveSlot> slots = saveManager.listSaves(currentUser.username());
            if (slots.isEmpty()) {
                if (view != null) {
                    view.showInfoMessage("Load", "No save file");
                }
                return;
            }

            SaveManager.SaveSlot selected = view == null ? slots.get(0) : view.chooseSaveSlot(slots);
            if (selected == null) {
                return;
            }

            GameSnapshot snapshot = saveManager.load(currentUser.username(), selected.fileName());
            if (snapshot == null) {
                if (view != null) {
                    view.showInfoMessage("Load", "Selected save does not exist");
                }
                return;
            }
            session = GameSession.fromSnapshot(snapshot, pathFinder, deadEndDetector);
            finishing = false;
            clearSelections();
            if (view != null) {
                view.setGameSession(session);
                view.setHintPair(null);
                view.showGameCard();
                refreshHud();
                view.repaintGame();
            }
            startTimer();
        } catch (IOException | ClassNotFoundException e) {
            if (view != null) {
                view.showErrorMessage("Load", "Invalid save file");
            }
            goToMenu();
        }
    }

    /**
     * 退出当前游戏并自动保存存档（已登录用户）。
     */
    public void exitCurrentGame() {
        if (session == null) {
            goToMenu();
            return;
        }

        if (currentUser != null) {
            try {
                SaveManager.SaveSlot slot = saveManager.save(session.toSnapshot(currentUser.username()));
                if (view != null) {
                    view.showInfoMessage("Exit", "Auto-saved: " + slot.displayName());
                }
            } catch (IOException e) {
                if (view != null) {
                    view.showErrorMessage("Exit", "Auto-save failed");
                }
            }
        }
        goToMenu();
    }

    /**
     * 检测存档/读档操作是否过于频繁。
     * @param saveAction 是否为存档操作
     * @return 过于频繁返回true
     */
    private boolean isTooFrequent(boolean saveAction) {
        long now = System.currentTimeMillis();
        if (saveAction) {
            if (now - lastSaveClickAt < SAVE_CLICK_COOLDOWN_MS) {
                return true;
            }
            lastSaveClickAt = now;
            return false;
        }
        if (now - lastLoadClickAt < LOAD_CLICK_COOLDOWN_MS) {
            return true;
        }
        lastLoadClickAt = now;
        return false;
    }

    /**
     * 处理棋盘点击事件，执行选中和消除逻辑。
     * @param position 点击的坐标
     */
    public void handleBoardClick(Position position) {
        if (session == null || position == null || session.hasWon() || session.hasLost()) {
            return;
        }
        GameBoard board = session.board();
        if (board.isEmpty(position)) {
            return;
        }

        if (selectedFirst == null) {
            selectedFirst = position;
            selectedSecond = null;
            hintPair = null;
            refreshSelections();
            return;
        }

        if (selectedFirst.equals(position)) {
            clearSelections();
            refreshSelections();
            return;
        }

        int firstPattern = board.tileAt(selectedFirst).patternId();
        int secondPattern = board.tileAt(position).patternId();
        if (!board.canMatchPatterns(firstPattern, secondPattern)) {
            selectedFirst = position;
            selectedSecond = null;
            hintPair = null;
            refreshSelections();
            return;
        }

        selectedSecond = position;
        boolean eliminated = session.eliminate(selectedFirst, selectedSecond);
        selectedFirst = null;
        selectedSecond = null;
        hintPair = null;
        refreshSelections();
        refreshHud();

        if (eliminated) {
            schedulePathClear();
        }

        if (session.hasWon()) {
            finishGame("Victory");
            return;
        }
        if (session.hasLost()) {
            finishGame("Defeat");
        }
    }

    /**
     * 获取当前游戏会话。
     * @return 游戏会话实例
     */
    public GameSession getSession() {
        return session;
    }

    /**
     * 获取第一次选中的方块位置。
     * @return 第一次选中位置，无则返回null
     */
    public Position getSelectedFirst() {
        return selectedFirst;
    }

    /**
     * 获取第二次选中的方块位置。
     * @return 第二次选中位置，无则返回null
     */
    public Position getSelectedSecond() {
        return selectedSecond;
    }

    /**
     * 获取当前提示对位置。
     * @return 提示对数组，无则返回null
     */
    public Position[] getHintPair() {
        if (hintPair == null) {
            return null;
        }
        return new Position[]{hintPair[0], hintPair[1]};
    }

    /**
     * 判断当前是否已登录。
     * @return 已登录返回true
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * 获取当前登录用户名。
     * @return 用户名，未登录返回null
     */
    public String getCurrentUserName() {
        return currentUser == null ? null : currentUser.username();
    }

    /**
     * 获取当前用户名的首字母作为头像。
     * @return 首字母大写，未登录返回"A"
     */
    public String getCurrentUserInitial() {
        if (currentUser == null || currentUser.username() == null || currentUser.username().isBlank()) {
            return "A";
        }
        return currentUser.username().substring(0, 1).toUpperCase();
    }

    /**
     * 启动新的游戏会话。
     * @param difficulty 游戏难度
     */
    private void startNewSession(Constants.Difficulty difficulty, Constants.Theme theme) {
        stopTimer();
        GameBoard board = boardGenerator.generate(difficulty, theme);
        if (theme != Constants.Theme.THEME1 && board.theme() == Constants.Theme.THEME1 && view != null) {
            view.showInfoMessage("Theme", theme.id() + " resource not found, switched to theme1.");
        }
        session = new GameSession(difficulty, board.theme(), board, pathFinder, deadEndDetector);
        finishing = false;
        clearSelections();
        if (view != null) {
            view.setGameSession(session);
            view.setHintPair(null);
            view.showGameCard();
            refreshHud();
            view.repaintGame();
        }
        startTimer();
    }

    /**
     * 启动游戏计时器，每秒更新游戏状态。
     */
    private void startTimer() {
        if (timer == null) {
            timer = new Timer(Constants.TIMER_TICK_MS, e -> {
                if (session == null || finishing) {
                    return;
                }
                session.tickSecond();
                refreshHud();
                if (session.hasWon()) {
                    finishGame("Victory");
                    return;
                }
                if (session.hasLost()) {
                    finishGame("Defeat");
                }
            });
        }
        timer.stop();
        timer.start();
    }

    /**
     * 结束游戏，处理胜负逻辑和状态清理。
     * @param title 游戏结果标题（Victory/Defeat）
     */
    private void finishGame(String title) {
        if (session == null || finishing) {
            return;
        }
        finishing = true;
        stopTimer();
        if (currentUser != null && session != null) {
            userManager.recordGame(currentUser, session.score());
        }
        if (view != null && session != null) {
            view.showInfoMessage(title, title + " | Score: " + session.score());
        }
        session = null;
        clearSelections();
        if (view != null) {
            view.setGameSession(null);
            view.setHintPair(null);
            view.showMenuCard();
            view.repaintGame();
        }
        finishing = false;
    }

    /**
     * 刷新HUD面板显示的游戏状态信息。
     */
    private void refreshHud() {
        if (view == null || session == null) {
            return;
        }
        String userText = currentUser == null ? "Guest" : currentUser.username();
        view.updateHud(
                userText,
                session.difficulty().name() + " | " + session.theme().id(),
                session.score(),
                session.timeLeft(),
                session.comboCount(),
                session.lastOperation()
        );
    }

    /**
     * 刷新选中状态显示。
     */
    private void refreshSelections() {
        if (view != null) {
            view.setHintPair(hintPair);
            view.repaintGame();
        }
    }

    /**
     * 清除所有选中状态。
     */
    private void clearSelections() {
        selectedFirst = null;
        selectedSecond = null;
        hintPair = null;
    }

    /**
     * 延迟清除连接路径显示效果。
     */
    private void schedulePathClear() {
        Timer clearTimer = new Timer(220, e -> {
            if (session != null) {
                session.clearLastPath();
                if (view != null) {
                    view.repaintGame();
                }
            }
        });
        clearTimer.setRepeats(false);
        clearTimer.start();
    }

    /**
     * 停止并销毁游戏计时器。
     */
    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }
}