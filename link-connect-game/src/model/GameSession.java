package model;

import data.Theme2VocabularyData;
import data.Theme3PoemData;
import data.Theme4YauData;
import logic.ComboManager;
import logic.DeadEndDetector;
import logic.PathFinder;

/**
 * GameSession 记录单句游戏的状态
 *
 * <p>包括：
 * <ul>
 *   <li> {@link GameBoard} 棋盘</li>
 *   <li>分数及时间</li>
 *   <li>Combo 管理 {@link ComboManager}.</li>
 *   <li>输赢判断</li>
 *   <li>操作记录供 HUD 显示</li>
 * </ul>
 *
 * <p><b>死局检测：</b> 只在成功消除和刷新后更改。 {@link #hasLost()} 在其他地方多次调用但很简单
 */
public class GameSession {

    private final Constants.Difficulty difficulty;

    private final Constants.Theme theme;

    private final GameBoard board;

    private final PathFinder pathFinder;

    private final DeadEndDetector deadEndDetector;

    private final ComboManager comboManager;

    private final OperationLog operationLog;

    private int score;

    private int timeLeft;

    private Path lastPath;

    private boolean noValidMoves;

    /**
     * 构建新的 GameSession
     *
     * @param difficulty
     * @param board
     * @param pathFinder
     * @param deadEndDetector
     */
    public GameSession(Constants.Difficulty difficulty,
                       Constants.Theme theme,
                       GameBoard board,
                       PathFinder pathFinder,
                       DeadEndDetector deadEndDetector) {
        this.difficulty = difficulty;
        this.theme = theme == null ? Constants.Theme.THEME1 : theme;
        this.board = board;
        this.pathFinder = pathFinder;
        this.deadEndDetector = deadEndDetector;
        this.comboManager = new ComboManager();
        this.operationLog = new OperationLog();
        this.timeLeft = difficulty.timeLimitSeconds();
        refreshNoValidMovesState();
    }

    /**
     * 尝试消除 {@code p1} 和 {@code p2}.
     *
     * <p>条件已由 {@link PathFinder} 保证
     *
     * <p>成功后刷新状态：消除，保留消除线，combo 计算，分数
     *
     * <p>失败重置 combo
     *
     * @param p1
     * @param p2
     * @return {@code true} 若成功消除
     */
    public boolean eliminate(Position p1, Position p2) {
        Path path = pathFinder.findPath(board, p1, p2);
        if (path == null) {
            comboManager.reset();
            operationLog.update("Unconnectable");
            return false;
        }

        board.clear(p1);
        board.clear(p2);
        lastPath = path;

        int added = comboManager.onEliminate();
        score += added;
        operationLog.update("Matched x2, +" + added + " points");

        refreshNoValidMovesState();
        return true;
    }

    public void tickSecond() {
        if (timeLeft > 0) {
            timeLeft--;
        }
    }

    public boolean hasWon() {
        return board.remainingTiles() == 0;
    }

    public boolean hasLost() {
        if (hasWon()) {
            return false;
        }
        if (timeLeft <= 0) {
            return true;
        }
        return noValidMoves;
    }

    public Position[] hintPair() {
        return deadEndDetector.findAnyPair(board);
    }

    /**
     * 在 {@link logic.BoardGenerator#reshuffle} 之后调用以刷新死局状态。
     */
    public void refreshNoValidMovesState() {
        noValidMoves = !deadEndDetector.hasAnyValidMove(board);
    }

    /**
     * 序列化当前 session 状态到 {@link GameSnapshot} 用于存档
     *
     * @param username
     * @return 一个状态快照
     */
    public GameSnapshot toSnapshot(String username) {
        String snapshotStatus = buildSnapshotStatus();
        return new GameSnapshot(
                username,
                difficulty,
                theme,
                board.exportPatternGrid(),
                score,
                timeLeft,
                comboManager.comboCount(),
                snapshotStatus
        );
    }

    private String buildSnapshotStatus() {
        if (hasWon()) {
            return "Victory";
        }
        if (timeLeft <= 0) {
            return "Defeat (Time Out)";
        }
        if (hasLost()) {
            return "Defeat (No Moves)";
        }
        return operationLog.lastMessage();
    }

    /**
     * 在 {@link GameSnapshot} 恢复 {@link GameSession}
     *
     * @param snapshot
     * @param pathFinder
     * @param deadEndDetector
     * @return {@link GameSession}
     */
    public static GameSession fromSnapshot(GameSnapshot snapshot,
                                           PathFinder pathFinder,
                                           DeadEndDetector deadEndDetector) {
        GameBoard board = new GameBoard(
                snapshot.difficulty().rows(),
                snapshot.difficulty().cols(),
                snapshot.theme() == null ? Constants.Theme.THEME1 : snapshot.theme()
        );
        board.importPatternGrid(snapshot.boardGrid());

        GameSession session = new GameSession(
                snapshot.difficulty(),
                snapshot.theme() == null ? Constants.Theme.THEME1 : snapshot.theme(),
                board,
                pathFinder,
                deadEndDetector
        );

        session.score = snapshot.score();
        session.timeLeft = snapshot.timeLeft();

        session.comboManager.setComboCount(snapshot.comboCount());

        session.operationLog.update(snapshot.operationMessage());

        session.refreshNoValidMovesState();
        return session;
    }

    // -------------------------------------------------------------------------
    // gettet setter 和其他查询函数
    // -------------------------------------------------------------------------

    public Path lastPath() {
        return lastPath;
    }

    public void clearLastPath() {
        lastPath = null;
    }

    public int score() {
        return score;
    }

    public Constants.Theme theme() {
        return theme;
    }

    public boolean isChinesePattern(int patternId) {
        if (theme == Constants.Theme.THEME2) {
            return !Theme2VocabularyData.isEnglishPattern(patternId);
        }
        if (theme == Constants.Theme.THEME3) {
            return true;
        }
        if (theme == Constants.Theme.THEME4) {
            return true;
        }
        return false;
    }

    public String labelForPattern(int patternId) {
        if (theme == Constants.Theme.THEME1) {
            return String.valueOf(patternId);
        }
        if (theme == Constants.Theme.THEME2) {
            if (Theme2VocabularyData.isEnglishPattern(patternId)) {
                String en = Theme2VocabularyData.englishForPattern(patternId);
                return en == null ? String.valueOf(patternId) : en;
            }
            String zh = Theme2VocabularyData.chineseForPattern(patternId);
            return zh == null ? String.valueOf(patternId) : zh;
        }
        if (theme == Constants.Theme.THEME3) {
            if (Theme3PoemData.isFirstLinePattern(patternId)) {
                String first = Theme3PoemData.firstLineForPattern(patternId);
                return first == null ? String.valueOf(patternId) : first;
            }
            String second = Theme3PoemData.secondLineForPattern(patternId);
            return second == null ? String.valueOf(patternId) : second;
        }
        if (Theme4YauData.isFirstLinePattern(patternId)) {
            String first = Theme4YauData.firstLineForPattern(patternId);
            return first == null ? String.valueOf(patternId) : first;
        }
        String second = Theme4YauData.secondLineForPattern(patternId);
        return second == null ? String.valueOf(patternId) : second;
    }

    public int timeLeft() {
        return timeLeft;
    }

    public int comboCount() {
        return comboManager.comboCount();
    }

    public String lastOperation() {
        return operationLog.lastMessage();
    }

    public Constants.Difficulty difficulty() {
        return difficulty;
    }

    public GameBoard board() {
        return board;
    }
}
