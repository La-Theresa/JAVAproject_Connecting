package model;

import data.Theme2VocabularyData;
import data.Theme3PoemData;
import data.Theme4YauData;
import logic.ComboManager;
import logic.DeadEndDetector;
import logic.PathFinder;

/**
 * GameSession is the central game-state object for a single play session.
 *
 * <p>It owns and coordinates:
 * <ul>
 *   <li>The {@link GameBoard} — the live tile grid.</li>
 *   <li>Score and time tracking.</li>
 *   <li>Combo management via {@link ComboManager}.</li>
 *   <li>Win/loss evaluation.</li>
 *   <li>An operation log for HUD feedback.</li>
 * </ul>
 *
 * <p><b>Dead-end detection caching:</b> checking whether any valid move exists
 * is an O(n²) operation (all same-pattern pairs × pathfinding). To avoid
 * running this check on every timer tick <em>and</em> every click, the result
 * is cached in {@code noValidMovesCache}:
 * <ul>
 *   <li>{@code null}  — not yet computed for the current board state.</li>
 *   <li>{@code false} — at least one valid move was found (cache valid).</li>
 *   <li>{@code true}  — no valid moves remain (cache valid, board is stuck).</li>
 * </ul>
 * The cache is invalidated (set to {@code null}) after any successful
 * elimination or board reshuffle, since both operations change the board state.
 */
public class GameSession {

    /** The difficulty chosen for this session (affects board size and time limit). */
    private final Constants.Difficulty difficulty;

    /** 当前会话主题。 */
    private final Constants.Theme theme;

    /** The live tile grid for this session. */
    private final GameBoard board;

    /** Shared path finder; stateless, safe to reuse across sessions. */
    private final PathFinder pathFinder;

    /** Dead-end detector used for hint generation and lose-condition checking. */
    private final DeadEndDetector deadEndDetector;

    /** Tracks consecutive successful eliminations and calculates score bonuses. */
    private final ComboManager comboManager;

    /** Records the most recent operation message for HUD display. */
    private final OperationLog operationLog;

    /** Accumulated score for this session. Updated by every successful elimination. */
    private int score;

    /**
     * Seconds remaining in the session countdown. Decremented each tick by
     * {@link #tickSecond()}. The game is lost when this reaches 0.
     */
    private int timeLeft;

    /**
     * The path drawn for the most recently completed elimination, used by the
     * UI to animate the connection line. Cleared shortly after by the controller.
     */
    private Path lastPath;

    /**
     * Cached dead-end detection result for the current board state.
     * {@code null} means the cache is invalid and must be recomputed.
     * {@code true} means no valid moves exist; {@code false} means at least one does.
     *
     * <p>Invalidate by setting to {@code null} after any board mutation
     * (elimination or reshuffle).
     */
    private Boolean noValidMovesCache;

    /**
     * Constructs a new game session with all state initialised to defaults.
     *
     * @param difficulty      the chosen difficulty (determines board size and time limit)
     * @param board           the pre-generated, pre-filled game board
     * @param pathFinder      the path finder to use for elimination checks
     * @param deadEndDetector the dead-end detector for hint and lose-condition logic
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
        this.noValidMovesCache = null; // unknown until first check
    }

    /**
     * Attempts to eliminate the two tiles at {@code p1} and {@code p2}.
     *
     * <p>Elimination succeeds only if:
     * <ol>
     *   <li>The path finder finds a valid connecting path (≤ 2 turns).</li>
     *   <li>Both tiles carry the same non-zero pattern (enforced inside {@link PathFinder}).</li>
     * </ol>
     *
     * <p>On success: both tiles are cleared, the last path is stored for UI
     * animation, the combo counter is incremented, score is awarded, and the
     * dead-end cache is invalidated.
     *
     * <p>On failure: the combo counter is reset to zero and the cache is left
     * unchanged (a failed attempt does not alter the board).
     *
     * @param p1 the first selected tile position
     * @param p2 the second selected tile position
     * @return {@code true} if the tiles were successfully connected and removed
     */
    public boolean eliminate(Position p1, Position p2) {
        Path path = pathFinder.findPath(board, p1, p2);
        if (path == null) {
            // Failed attempt — break the combo streak, but board is unchanged.
            comboManager.reset();
            operationLog.update("Unconnectable");
            return false;
        }

        int pattern = board.tileAt(p1).patternId();
        int pattern2 = board.tileAt(p2).patternId();
        board.clear(p1);
        board.clear(p2);
        lastPath = path;

        int added = comboManager.onEliminate();
        score += added;
        if (theme == Constants.Theme.THEME2) {
            operationLog.update("Matched [" + labelForPattern(pattern) + " - "
                    + labelForPattern(pattern2) + "], +" + added + " points");
        } else {
            operationLog.update("Eliminated [" + pattern + "] x2, +" + added + " points");
        }

        // Board state changed — cached dead-end result is no longer valid.
        noValidMovesCache = null;
        return true;
    }

    /**
     * Advances the countdown timer by one second. Should be called once per
     * second by the game controller's timer.
     * Has no effect once the timer has already reached zero.
     */
    public void tickSecond() {
        if (timeLeft > 0) {
            timeLeft--;
        }
    }

    /**
     * Returns {@code true} if the board has been completely cleared (all tiles
     * eliminated). This is the win condition.
     *
     * @return {@code true} if no tiles remain on the board
     */
    public boolean hasWon() {
        return board.remainingTiles() == 0;
    }

    /**
     * Returns {@code true} if the session is in a losing state — either the
     * timer has expired, or no valid moves remain on the board.
     *
     * <p>A won session is never also considered lost. Dead-end detection is
     * cached: the expensive O(n²) check runs only when the cache is invalid
     * (i.e. after an elimination, reshuffle, or session start).
     *
     * @return {@code true} if the player has lost
     */
    public boolean hasLost() {
        if (hasWon()) {
            return false; // winning takes priority
        }
        if (timeLeft <= 0) {
            return true; // timed out
        }

        /*
         * Check for dead-end (no available valid move). The result is cached
         * because this method is called on every timer tick AND every click.
         * We only recompute after the board state actually changes.
         */
        if (noValidMovesCache == null) {
            noValidMovesCache = !deadEndDetector.hasAnyValidMove(board);
        }
        return noValidMovesCache;
    }

    /**
     * Finds and returns a connectable tile pair for the hint system.
     * Returns {@code null} if the board is in a dead-end state.
     *
     * @return a two-element {@code Position[]} hint pair, or {@code null}
     */
    public Position[] hintPair() {
        return deadEndDetector.findAnyPair(board);
    }

    /**
     * Invalidates the dead-end detection cache. Must be called whenever the
     * board is externally mutated in a way that is not tracked by
     * {@link #eliminate} — specifically after a board reshuffle performed by
     * {@link logic.BoardGenerator#reshuffle}.
     */
    public void invalidateMoveCache() {
        noValidMovesCache = null;
    }

    /**
     * Serialises the current session state into a {@link GameSnapshot} for
     * save-file persistence.
     *
     * @param username the username to embed in the snapshot
     * @return a snapshot capturing score, time, combo, board, and difficulty
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
     * Restores a {@link GameSession} from a previously saved {@link GameSnapshot}.
     *
     * <p>The combo count is restored via {@link ComboManager#setComboCount}
     * instead of repeated calls to {@link ComboManager#onEliminate}, which
     * would incorrectly inflate the score.
     *
     * @param snapshot        the snapshot to restore from
     * @param pathFinder      the path finder to wire into the restored session
     * @param deadEndDetector the dead-end detector to wire into the restored session
     * @return a fully restored {@link GameSession} with state matching the snapshot
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

        // Restore scalar state directly — these fields are set from the snapshot
        // without triggering any side-effects (scoring, etc.).
        session.score = snapshot.score();
        session.timeLeft = snapshot.timeLeft();

        // Use setComboCount() to restore the combo streak without awarding score.
        // (Previously, calling onEliminate() N times would incorrectly add score
        //  N times even though the score is already captured in snapshot.score().)
        session.comboManager.setComboCount(snapshot.comboCount());

        session.operationLog.update(snapshot.operationMessage());

        // Cache is unknown until the first hasLost() call after restore.
        session.noValidMovesCache = null;
        return session;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the path drawn for the most recently completed elimination, or
     * {@code null} if no elimination has occurred yet or the path was cleared.
     *
     * @return the last connecting path, or {@code null}
     */
    public Path lastPath() {
        return lastPath;
    }

    /**
     * Clears the stored last-path so the UI stops drawing the connection line.
     * Typically called by the controller a short time after each elimination.
     */
    public void clearLastPath() {
        lastPath = null;
    }

    /**
     * Returns the current accumulated score for this session.
     *
     * @return score (≥ 0)
     */
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

    /**
     * Returns the number of seconds remaining in the timer countdown.
     *
     * @return seconds left (≥ 0)
     */
    public int timeLeft() {
        return timeLeft;
    }

    /**
     * Returns the current length of the combo streak (consecutive successful
     * eliminations since the last failure or session start).
     *
     * @return combo count (≥ 0)
     */
    public int comboCount() {
        return comboManager.comboCount();
    }

    /**
     * Returns the most recent operation message, used to populate the HUD
     * status line (e.g. "Eliminated [3] x2, +15 points" or "Unconnectable").
     *
     * @return a non-null operation description string
     */
    public String lastOperation() {
        return operationLog.lastMessage();
    }

    /**
     * Returns the difficulty level associated with this session.
     *
     * @return the {@link Constants.Difficulty} enum value
     */
    public Constants.Difficulty difficulty() {
        return difficulty;
    }

    /**
     * Returns the live {@link GameBoard} underlying this session.
     * Callers should treat this as read-only unless they subsequently call
     * {@link #invalidateMoveCache()} to keep the session state consistent.
     *
     * @return the current game board
     */
    public GameBoard board() {
        return board;
    }
}
