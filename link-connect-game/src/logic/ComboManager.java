package logic;

import model.Constants;

/**
 * ComboManager 通过记录连续成功次数计算得分
 *
 * <p>规则：
 * <ul>
 *   <li>前 {@link Constants#COMBO_THRESHOLD} 个消除得到
 *  {@link Constants#BASE_SCORE} 分</li>
 *   <li>每一个额外消除按照下列公式增加分数：
 *       {@code BASE_SCORE + (rank * 5)} ，这里 {@code rank} 从 1 开始，每次增加 1</li>
 * </ul>
 *
 * <p>错误(选择了两个类型 <em>相同</em> 但不可连接) 时重置
 * (参见 {@link #reset()}).
 */
public class ComboManager {

    private int comboCount;

    /**
     * 记录成功消除和增加连击次数，并计算分数
     *
     * <p>对于额外连击的额外分数
     * <pre>
     *   rank  = comboCount - COMBO_THRESHOLD + 1   (1-based, ≥ 1)
     *   score = BASE_SCORE + rank * 5
     * </pre>
     *
     * @return 该次得分 {@link Constants#BASE_SCORE} 包括基本与额外
     */
    public int onEliminate() {
        comboCount++;

        if (comboCount < Constants.COMBO_THRESHOLD) {
            return Constants.BASE_SCORE;
        }

        int rank = comboCount - Constants.COMBO_THRESHOLD + 1;
        return Constants.BASE_SCORE + rank * 5;
    }

    public void reset() {
        comboCount = 0;
    }

    /**
     * 记录连击次数，给存档
     * {@link model.GameSnapshot} 使用，并不修改分数和加分
     *
     * @param count 需记录的连击次数
     */
    public void setComboCount(int count) {
        this.comboCount = Math.max(0, count);
    }

    public int comboCount() {
        return comboCount;
    }
}
