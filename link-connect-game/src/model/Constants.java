package model;

import java.awt.Color;

/**
 * 全局常量配置，集中管理难度、分数、UI参数和文件路径。
 */
public final class Constants {
    public static final int FRAME_WIDTH = 900;
    public static final int FRAME_HEIGHT = 1040;
    public static final int HUD_HEIGHT = 120;
    public static final int TIMER_TICK_MS = 1000;

    public static final int BASE_SCORE = 10;
    public static final int COMBO_THRESHOLD = 3;

    public static final Color SELECTED_COLOR = new Color(255, 204, 0);
    public static final Color PATH_COLOR = new Color(255, 215, 0);

    public static final String DATA_DIR = "link-connect-game/data";
    public static final String USER_FILE = DATA_DIR + "/users.dat";
    public static final String SAVE_DIR = DATA_DIR + "/saves";

    /**
     * 游戏难度参数。
     */
    public enum Difficulty {
        EASY(4, 4, 60, 5),
        HARD(10, 10, 120, 12);

        private final int rows;
        private final int cols;
        private final int timeLimitSeconds;
        private final int patternCount;

        Difficulty(int rows, int cols, int timeLimitSeconds, int patternCount) {
            this.rows = rows;
            this.cols = cols;
            this.timeLimitSeconds = timeLimitSeconds;
            this.patternCount = patternCount;
        }

        /**
         * 返回棋盘行数。
         */
        public int rows() {
            return rows;
        }

        /**
         * 返回棋盘列数。
         */
        public int cols() {
            return cols;
        }

        /**
         * 返回该难度时间限制（秒）。
         */
        public int timeLimitSeconds() {
            return timeLimitSeconds;
        }

        /**
         * 返回该难度图案种类数量。
         */
        public int patternCount() {
            return patternCount;
        }
    }

    private Constants() {
    }
}
