package logic;

import model.Constants;

/**
 * 连击计分管理器。
 */
public class ComboManager {
    private int comboCount;

    /**
     * 记录一次成功消除并返回本次加分。
     */
    public int onEliminate() {
        comboCount++;
        if (comboCount < Constants.COMBO_THRESHOLD) {
            return Constants.BASE_SCORE;
        }
        int bonus = (comboCount - Constants.COMBO_THRESHOLD + 1) * 5;
        return Constants.BASE_SCORE + bonus;
    }

    /**
     * 消除失败时重置连击。
     */
    public void reset() {
        comboCount = 0;
    }

    /**
     * 返回当前连击数。
     */
    public int comboCount() {
        return comboCount;
    }
}
