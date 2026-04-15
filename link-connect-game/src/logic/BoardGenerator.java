package logic;

import data.Theme2VocabularyData;
import data.Theme3PoemData;
import data.Theme4YauData;
import model.Constants;
import model.GameBoard;
import model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * BoardGenerator creates randomised game boards and guarantees that every
 * generated board is fully clearable (solvable) before returning it.
 *
 * <p>Generation strategy:
 * <ol>
 *   <li>Fill the grid with randomly shuffled, paired pattern IDs so that every
 *       pattern appears an even number of times (i.e. the board is always
 *       paired correctly by construction).</li>
 *   <li>Simulate a greedy clearing pass using {@link DeadEndDetector} to find
 *       any connectable pair, remove it, and repeat until the board is empty
 *       or no valid move remains.</li>
 *   <li>If the board cannot be fully cleared, discard it and retry (up to
 *       {@code MAX_RETRIES} times).</li>
 *   <li>After all retries, fall back to returning an unvalidated board rather
 *       than blocking indefinitely — the in-game reshuffle feature handles
 *       the rare case of a board that happens to reach a dead end.</li>
 * </ol>
 */
public class BoardGenerator {

    /**
     * Maximum number of attempts to generate a fully clearable board before
     * falling back to an unvalidated one. 200 is sufficient for HARD (10×10)
     * boards while keeping generation latency imperceptible.
     */
    private static final int MAX_RETRIES = 200;

    /** Used to verify generated paths during the solvability simulation. */
    private final PathFinder pathFinder;

    /** Used to find any removable pair during the solvability simulation. */
    private final DeadEndDetector deadEndDetector;

    /**
     * Constructs a BoardGenerator with the given path finder.
     * A {@link DeadEndDetector} is created internally and shares the same
     * path finder instance.
     *
     * @param pathFinder the {@link PathFinder} to use for validation;
     *                   must not be {@code null}
     */
    public BoardGenerator(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
        this.deadEndDetector = new DeadEndDetector(pathFinder);
    }

    /**
     * Generates a fully clearable game board for the given difficulty.
     *
     * <p>Attempts up to {@link #MAX_RETRIES} random boards, validating each
     * via a simulated greedy clearing pass. Returns the first board that passes,
     * or an unvalidated board if all retries fail.
     *
     * @param difficulty the difficulty level that determines grid size and
     *                   the number of distinct pattern types
     * @return a randomly generated {@link GameBoard}, validated for clearability
     *         in the common case
     */
    public GameBoard generate(Constants.Difficulty difficulty, Constants.Theme theme) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            GameBoard board = randomBoard(difficulty, theme);
            // Simulate clearing on a deep copy so the original remains intact.
            if (isCleanable(board.deepCopy())) {
                return board;
            }
        }
        // Fallback: return an unvalidated board. The in-game reshuffle can
        // rescue the player if a dead-end is reached during play.
        return randomBoard(difficulty, theme);
    }

    /**
     * Reshuffles the positions of all remaining tiles on the board in-place,
     * preserving the multiset of pattern IDs but randomising their positions.
     *
     * <p>This operation does not guarantee that the resulting board is
     * clearable, but after calling this the session must invalidate its
     * dead-end detection cache (see {@link model.GameSession#invalidateMoveCache()}).
     *
     * @param board the board whose remaining tiles should be reshuffled
     */
    public void reshuffle(GameBoard board) {
        // Collect the pattern IDs of all remaining (non-empty) tiles.
        List<Position> positions = board.nonEmptyPositions();
        List<Integer> patterns = new ArrayList<>(positions.size());
        for (Position p : positions) {
            patterns.add(board.tileAt(p).patternId());
        }

        // Shuffle the pattern IDs randomly.
        Collections.shuffle(patterns, ThreadLocalRandom.current());

        // Write the shuffled patterns back into the same set of positions.
        int idx = 0;
        for (Position p : positions) {
            board.setPattern(p, patterns.get(idx++));
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a randomly filled board for the given difficulty without checking
     * solvability.
     *
     * <p>Pattern IDs are distributed as follows:
     * <ul>
     *   <li>Patterns cycle from 1 to {@code patternCount} in the inner loop.</li>
     *   <li>Each pattern appears exactly twice per cycle, so the total is always
     *       an even number of tiles (guaranteed because {@code rows × cols} is
     *       enforced to be even by the {@link Constants.Difficulty} definition).</li>
     * </ul>
     *
     * @param difficulty the difficulty whose dimensions and pattern count are used
     * @return a freshly constructed, randomly filled {@link GameBoard}
     * @throws IllegalStateException if {@code rows × cols} is odd (would leave
     *         one tile without a matching partner)
     */
    private GameBoard randomBoard(Constants.Difficulty difficulty, Constants.Theme theme) {
        int rows = difficulty.rows();
        int cols = difficulty.cols();
        int total = rows * cols;

        // Guard: the board must have an even number of cells so every tile has a pair.
        if (total % 2 != 0) {
            throw new IllegalStateException(
                "Board size " + rows + "x" + cols + " (" + total
                + " cells) is odd — each pattern requires a pair, so an even "
                + "cell count is required. Adjust the difficulty configuration."
            );
        }

        Constants.Theme safeTheme = theme == null ? Constants.Theme.THEME1 : theme;
        if (safeTheme == Constants.Theme.THEME2 && !Theme2VocabularyData.isAvailable()) {
            safeTheme = Constants.Theme.THEME1;
        }
        if (safeTheme == Constants.Theme.THEME3 && !Theme3PoemData.isAvailable()) {
            safeTheme = Constants.Theme.THEME1;
        }
        if (safeTheme == Constants.Theme.THEME4 && !Theme4YauData.isAvailable()) {
            safeTheme = Constants.Theme.THEME1;
        }

        GameBoard board = new GameBoard(rows, cols, safeTheme);

        // Build a list of paired pattern IDs and shuffle it.
        List<Integer> ids = new ArrayList<>(total);
        if (safeTheme == Constants.Theme.THEME2) {
            int pairSlots = total / 2;
            int uniqueTarget = Math.min(pairSlots, Math.max(8, difficulty.patternCount() * 2));
            List<Integer> selectedIds = Theme2VocabularyData.randomEntryIds(uniqueTarget);
            if (selectedIds.isEmpty()) {
                safeTheme = Constants.Theme.THEME1;
                board = new GameBoard(rows, cols, safeTheme);
            } else {
                for (int i = 0; i < pairSlots; i++) {
                    int vocabId = selectedIds.get(i % selectedIds.size());
                    ids.add(Theme2VocabularyData.englishPatternId(vocabId));
                    ids.add(Theme2VocabularyData.chinesePatternId(vocabId));
                }
            }
        } else if (safeTheme == Constants.Theme.THEME3) {
            int pairSlots = total / 2;
            int uniqueTarget = Math.min(pairSlots, Math.max(8, difficulty.patternCount() * 2));
            List<Integer> selectedIds = Theme3PoemData.randomEntryIds(uniqueTarget);
            if (selectedIds.isEmpty()) {
                safeTheme = Constants.Theme.THEME1;
                board = new GameBoard(rows, cols, safeTheme);
            } else {
                for (int i = 0; i < pairSlots; i++) {
                    int poemId = selectedIds.get(i % selectedIds.size());
                    ids.add(Theme3PoemData.firstPatternId(poemId));
                    ids.add(Theme3PoemData.secondPatternId(poemId));
                }
            }
        } else if (safeTheme == Constants.Theme.THEME4) {
            int pairSlots = total / 2;
            int uniqueTarget = Math.min(pairSlots, Math.max(8, difficulty.patternCount() * 2));
            List<Integer> selectedIds = Theme4YauData.randomEntryIds(uniqueTarget);
            if (selectedIds.isEmpty()) {
                safeTheme = Constants.Theme.THEME1;
                board = new GameBoard(rows, cols, safeTheme);
            } else {
                for (int i = 0; i < pairSlots; i++) {
                    int yauId = selectedIds.get(i % selectedIds.size());
                    ids.add(Theme4YauData.firstPatternId(yauId));
                    ids.add(Theme4YauData.secondPatternId(yauId));
                }
            }
        }

        if (safeTheme == Constants.Theme.THEME1) {
            int patternCount = difficulty.patternCount();
            for (int i = 0; i < total / 2; i++) {
                // Cycle through pattern IDs 1..patternCount, adding each one twice.
                int pattern = (i % patternCount) + 1;
                ids.add(pattern);
                ids.add(pattern);
            }
        }
        Collections.shuffle(ids, ThreadLocalRandom.current());

        // Place the shuffled patterns onto the board row-by-row.
        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                board.setPattern(new Position(r, c), ids.get(idx++));
            }
        }
        return board;
    }

    /**
     * Simulates a greedy clearing pass to determine whether the given board
     * can be completely cleared.
     *
     * <p>On each step, {@link DeadEndDetector#findAnyPair} finds any removable
     * pair, which is then cleared from the board. The loop continues until the
     * board is empty (success) or no pair can be found (dead-end = failure).
     *
     * <p>A {@code guard} counter prevents an infinite loop in the pathological
     * case where the detector and path finder disagree (should not happen in
     * practice, but protects against bugs during development).
     *
     * @param board a <em>copy</em> of the board to simulate on; will be mutated
     * @return {@code true} if the board can be fully cleared, {@code false} otherwise
     */
    private boolean isCleanable(GameBoard board) {
        // Upper bound on iterations: at most (rows×cols / 2) pairs can be removed.
        // Multiply by 4 for safety to guard against any unexpected re-visits.
        int guard = board.rows() * board.cols() * 4;

        while (board.remainingTiles() > 0 && guard-- > 0) {
            Position[] pair = deadEndDetector.findAnyPair(board);
            if (pair == null) {
                // No valid move found — board is stuck.
                return false;
            }
            // Verify path exists (redundant with findAnyPair but kept as a
            // sanity check during generation to catch any detector–finder mismatch).
            if (pathFinder.findPath(board, pair[0], pair[1]) == null) {
                return false;
            }
            board.clear(pair[0]);
            board.clear(pair[1]);
        }

        return board.remainingTiles() == 0;
    }
}
