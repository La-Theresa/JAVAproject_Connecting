package logic;

import model.Constants;

/**
 * ComboManager tracks consecutive successful tile eliminations and computes
 * the score earned for each elimination, including combo bonuses.
 *
 * <p>Scoring rules:
 * <ul>
 *   <li>The first {@link Constants#COMBO_THRESHOLD} eliminations in a row each
 *       earn exactly {@link Constants#BASE_SCORE} points.</li>
 *   <li>Every additional elimination beyond the threshold earns
 *       {@code BASE_SCORE + (rank * 5)} where {@code rank} starts at 1 for
 *       the first above-threshold elimination and increases by 1 each time.</li>
 * </ul>
 *
 * <p>The combo count is reset to zero whenever an elimination attempt fails
 * (see {@link #reset()}).
 */
public class ComboManager {

    /**
     * Number of consecutive successful eliminations made so far in the current
     * combo chain. Reset to 0 on any failed elimination.
     */
    private int comboCount;

    /**
     * Records a successful elimination, increments the combo counter, and
     * returns the score earned for this elimination.
     *
     * <p>The bonus formula for above-threshold combos:
     * <pre>
     *   rank  = comboCount - COMBO_THRESHOLD + 1   (1-based, ≥ 1)
     *   score = BASE_SCORE + rank * 5
     * </pre>
     *
     * @return the score earned: {@link Constants#BASE_SCORE} for normal
     *         eliminations, or an elevated value for combo streaks
     */
    public int onEliminate() {
        comboCount++;

        if (comboCount < Constants.COMBO_THRESHOLD) {
            // Still below the combo threshold — return the flat base score.
            return Constants.BASE_SCORE;
        }

        /*
         * Combo bonus: each consecutive hit above the threshold adds 5 extra
         * points. 'rank' is 1 for the first above-threshold hit, 2 for the
         * second, and so on.
         */
        int rank = comboCount - Constants.COMBO_THRESHOLD + 1;
        return Constants.BASE_SCORE + rank * 5;
    }

    /**
     * Resets the combo counter to zero. Should be called whenever a tile
     * connection attempt fails so the streak is broken.
     */
    public void reset() {
        comboCount = 0;
    }

    /**
     * Directly sets the combo count to the specified value without awarding
     * any score. Used exclusively when restoring a saved game session from a
     * {@link model.GameSnapshot} so that the combo state is recovered without
     * the side-effect of adding score points.
     *
     * @param count the combo count to restore (must be ≥ 0)
     */
    public void setComboCount(int count) {
        // Clamp to zero in case the snapshot contains an invalid negative value.
        this.comboCount = Math.max(0, count);
    }

    /**
     * Returns the current combo count (number of consecutive successful
     * eliminations since the last reset).
     *
     * @return the current combo streak length (≥ 0)
     */
    public int comboCount() {
        return comboCount;
    }
}
