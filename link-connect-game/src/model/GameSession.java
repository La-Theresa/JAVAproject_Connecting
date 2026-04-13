package model;

import logic.ComboManager;
import logic.DeadEndDetector;
import logic.PathFinder;

/**
 * 游戏会话状态：分数、计时、消除判断、胜负状态。
 */
public class GameSession {
    private final Constants.Difficulty difficulty;
    private final GameBoard board;
    private final PathFinder pathFinder;
    private final DeadEndDetector deadEndDetector;
    private final ComboManager comboManager;
    private final OperationLog operationLog;

    private int score;
    private int timeLeft;
    private Path lastPath;

    public GameSession(Constants.Difficulty difficulty,
                       GameBoard board,
                       PathFinder pathFinder,
                       DeadEndDetector deadEndDetector) {
        this.difficulty = difficulty;
        this.board = board;
        this.pathFinder = pathFinder;
        this.deadEndDetector = deadEndDetector;
        this.comboManager = new ComboManager();
        this.operationLog = new OperationLog();
        this.timeLeft = difficulty.timeLimitSeconds();
    }

    /**
     * 尝试消除两个位置，成功返回true。
     */
    public boolean eliminate(Position p1, Position p2) {
        Path path = pathFinder.findPath(board, p1, p2);
        if (path == null) {
            comboManager.reset();
            operationLog.update("Unconnectable");
            return false;
        }
        int pattern = board.tileAt(p1).patternId();
        board.clear(p1);
        board.clear(p2);
        lastPath = path;
        int add = comboManager.onEliminate();
        score += add;
        operationLog.update("Eliminated [" + pattern + "] x2, +" + add + " points");
        return true;
    }

    /**
     * 每秒更新计时。
     */
    public void tickSecond() {
        if (timeLeft > 0) {
            timeLeft--;
        }
    }

    /**
     * 判断是否胜利（棋盘清空）。
     */
    public boolean hasWon() {
        return board.remainingTiles() == 0;
    }

    /**
     * 判断是否失败（超时或死局）。
     */
    public boolean hasLost() {
        if (hasWon()) {
            return false;
        }
        return timeLeft <= 0 || !deadEndDetector.hasAnyValidMove(board);
    }

    /**
     * 返回可提示的一组可消除坐标。
     */
    public Position[] hintPair() {
        return deadEndDetector.findAnyPair(board);
    }

    /**
     * 导出当前会话存档。
     */
    public GameSnapshot toSnapshot(String username) {
        return new GameSnapshot(
                username,
                difficulty,
                board.exportPatternGrid(),
                score,
                timeLeft,
                comboManager.comboCount(),
                operationLog.lastMessage()
        );
    }

    /**
     * 从快照恢复会话状态。
     */
    public static GameSession fromSnapshot(GameSnapshot snapshot,
                                           PathFinder pathFinder,
                                           DeadEndDetector deadEndDetector) {
        GameBoard board = new GameBoard(snapshot.difficulty().rows(), snapshot.difficulty().cols());
        board.importPatternGrid(snapshot.boardGrid());
        GameSession session = new GameSession(snapshot.difficulty(), board, pathFinder, deadEndDetector);
        session.score = snapshot.score();
        session.timeLeft = snapshot.timeLeft();
        for (int i = 0; i < snapshot.comboCount(); i++) {
            session.comboManager.onEliminate();
        }
        session.operationLog.update(snapshot.operationMessage());
        return session;
    }

    /**
     * 返回最近成功连接路径。
     */
    public Path lastPath() {
        return lastPath;
    }

    /**
     * 清除最近路径显示。
     */
    public void clearLastPath() {
        lastPath = null;
    }

    /**
     * 获取分数。
     */
    public int score() {
        return score;
    }

    /**
     * 获取剩余时间（秒）。
     */
    public int timeLeft() {
        return timeLeft;
    }

    /**
     * 获取当前连击数。
     */
    public int comboCount() {
        return comboManager.comboCount();
    }

    /**
     * 获取最近操作信息。
     */
    public String lastOperation() {
        return operationLog.lastMessage();
    }

    /**
     * 获取难度。
     */
    public Constants.Difficulty difficulty() {
        return difficulty;
    }

    /**
     * 获取棋盘对象。
     */
    public GameBoard board() {
        return board;
    }
}
