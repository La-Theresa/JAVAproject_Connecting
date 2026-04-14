package logic;

import model.GameBoard;
import model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DeadEndDetector checks whether the current game board has at least one valid
 * move remaining, and can return a concrete connectable pair for use as a hint.
 *
 * <p>Optimisation: pair iteration uses a double-index approach over the
 * {@code patternLocations} index to avoid duplicated symmetric checks.
 * For each pattern group, pairs are only evaluated as {@code (i, j)} where
 * {@code i < j}, cutting the work roughly in half compared to a naive
 * nested loop over all non-empty positions.
 */
public class DeadEndDetector {

    /** The path finder used to verify whether two tiles can actually be connected. */
    private final PathFinder pathFinder;

    /**
     * Constructs a DeadEndDetector backed by the given path finder.
     *
     * @param pathFinder the {@link PathFinder} instance to use for connectivity
     *                   checks; must not be {@code null}
     */
    public DeadEndDetector(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }

    /**
     * Returns {@code true} if the board has at least one pair of tiles that
     * can currently be connected and removed.
     *
     * @param board the game board to inspect
     * @return {@code true} if any valid move exists, {@code false} if the board
     *         is in a dead-end state (no removable pair)
     */
    public boolean hasAnyValidMove(GameBoard board) {
        return findAnyPair(board) != null;
    }

    /**
     * Searches the board for any pair of same-pattern tiles that can be
     * connected under the at-most-2-turns rule, and returns them.
     *
     * <p>Strategy: for each pattern type still present on the board, iterate
     * over all positions carrying that pattern as an ordered list and only
     * check pairs {@code (i, j)} with {@code i < j}. This avoids redundant
     * symmetric checks (checking both {@code (p,q)} and {@code (q,p)}).
     *
     * @param board the game board to inspect
     * @return a two-element {@code Position[]} {@code {p, q}} representing a
     *         connectable pair, or {@code null} if no such pair exists
     */
    public Position[] findAnyPair(GameBoard board) {
        /*
         * Iterate over each pattern group's position set. Using the internal
         * patternLocations index avoids scanning the entire grid — we only
         * visit positions that actually contain tiles.
         */
        for (Map.Entry<Integer, Set<Position>> entry : board.patternEntries()) {
            List<Position> group = new ArrayList<>(entry.getValue());
            int size = group.size();

            // With only one tile of this pattern left, no pair is possible.
            if (size < 2) {
                continue;
            }

            /*
             * For each unique unordered pair (i, j) with i < j, check
             * connectivity. Checking only i < j cuts iterations by ~50%
             * versus checking all ordered pairs.
             */
            for (int i = 0; i < size - 1; i++) {
                for (int j = i + 1; j < size; j++) {
                    if (pathFinder.canConnect(board, group.get(i), group.get(j))) {
                        return new Position[]{group.get(i), group.get(j)};
                    }
                }
            }
        }
        return null; // No connectable pair found anywhere on the board.
    }
}
