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
 * BoardGenerator 创造随机可消棋盘
 *
 * <p>逻辑：
 * <ol>
 *   <li>随机填充消除对</li>
 *   <li>通过 {@link DeadEndDetector} 模拟消除</li>
 *   <li>若无法全部消除，则丢弃重试(最多尝试 {@code MAX_RETRIES} 次)。</li>
 *   <li>若失败，则随机生成，避免超时 (可能无法全部消除)</li>
 * </ol>
 */
public class BoardGenerator {

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRIES = 250;

    private final PathFinder pathFinder;

    private final DeadEndDetector deadEndDetector;

    /**
     * 构建 BoardGenerator 实例，需提供 PathFinder 以供生成时验证棋盘可消性。
     * 其中 DeadEndDetector 也会使用同一个 PathFinder 来保持一致的路径判断逻辑。
     */
    public BoardGenerator(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
        this.deadEndDetector = new DeadEndDetector(pathFinder);
    }

    /**
     * 按照难度创建一个棋盘
     *
     * <p>最多尝试 {@link #MAX_RETRIES} 次，返回第一个成功棋盘，否则返回一个未经验证的随机棋盘。
     *
     * @param difficulty 难度选择，与棋盘格式有关
     * @return 随机生成的 {@link GameBoard}，保证在大多数情况下是可消的
     */
    public GameBoard generate(Constants.Difficulty difficulty, Constants.Theme theme) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            GameBoard board = randomBoard(difficulty, theme);
            // Simulate clearing on a deep copy so the original remains intact.
            if (isCleanable(board.deepCopy())) {
                return board;
            }
        }
        // Fallback: 返回一个随机棋盘
        return randomBoard(difficulty, theme);
    }

    /**
     * 打乱棋盘，重新分布剩余的图案但不改变数量和位置。
     *
     * <p>不保证可消, 但消除死局检测缓存(see {@link model.GameSession#invalidateMoveCache()}).
     *
     * @param board 预打乱的棋盘
     */
    public void reshuffle(GameBoard board) {
        // 收集棋盘信息
        List<Position> positions = board.nonEmptyPositions();
        List<Integer> patterns = new ArrayList<>(positions.size());
        for (Position p : positions) {
            patterns.add(board.tileAt(p).patternId());
        }

        // 打乱ID
        Collections.shuffle(patterns, ThreadLocalRandom.current());

        // 配对图案与ID
        int idx = 0;
        for (Position p : positions) {
            board.setPattern(p, patterns.get(idx++));
        }
    }

    // -------------------------------------------------------------------------
    // 具体方法
    // -------------------------------------------------------------------------

    /**
     * 随机创建棋盘
     * 
     * <p>包括实际棋盘和外围一圈虚拟棋盘 (不填充图案)</p>
     * 
     * <p>由于EASY难度布局特殊，新开了一个函数 {@link #randomEasyBoard}</p>
     *
    * <p>分布ID
     * <ul>
     *   <li>从 1 到 {@code patternCount} 循环图案ID</li>
     *   <li>每图案在每循环出现两次，保证可消前提</li>
     * </ul>
     *
     * @param difficulty 难度决定棋盘尺寸和图案数量
     * @return 随机的 {@link GameBoard}
     * @throws IllegalStateException 若 {@code playableRows × playableCols} 为奇数。
     */
    private GameBoard randomBoard(Constants.Difficulty difficulty, Constants.Theme theme) {
        int rows = difficulty.rows();
        int cols = difficulty.cols();

        if (difficulty == Constants.Difficulty.EASY) {
            return randomEasyBoard(rows, cols, theme, difficulty.patternCount());
        }

        int playableRows = rows - 2;
        int playableCols = cols - 2;
        int playableTotal = playableRows * playableCols;
        if (playableTotal % 2 != 0) {
            throw new IllegalStateException(
                "Playable board size " + playableRows + "x" + playableCols + " (" + playableTotal
                + " cells) is odd — each pattern requires a pair, so an even "
                + "playable cell count is required. Adjust the difficulty configuration."
            );
        }

        // 确保主题可用 (由于数据特殊)
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

        // 创建ID并打乱，这里全部为了避免文字型主题的数据错误加了回退
        List<Integer> ids = new ArrayList<>(playableTotal);
        if (safeTheme == Constants.Theme.THEME2) {
            int pairSlots = playableTotal / 2;
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
            int pairSlots = playableTotal / 2;
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
            int pairSlots = playableTotal / 2;
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
            for (int i = 0; i < playableTotal / 2; i++) {
                int pattern = (i % patternCount) + 1;
                ids.add(pattern);
                ids.add(pattern);
            }
        }
        Collections.shuffle(ids, ThreadLocalRandom.current());

        // 在实际棋盘中放置图案
        int idx = 0;
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                board.setPattern(new Position(r, c), ids.get(idx++));
            }
        }
        return board;
    }

    /**
     * 创建简单棋盘
     */
    private GameBoard randomEasyBoard(int rows, int cols, Constants.Theme theme, int patternCount) {
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
        List<Position> occupiedPositions = buildEasyOccupiedPositions();
        List<Integer> ids = buildPatternIdsForPositions(occupiedPositions.size(), safeTheme, patternCount);
        if (ids.isEmpty()) {
            return board;
        }

        Collections.shuffle(ids, ThreadLocalRandom.current());
        for (int i = 0; i < occupiedPositions.size(); i++) {
            board.setPattern(occupiedPositions.get(i), ids.get(i));
        }
        return board;
    }

    /**
     * 两个 4*4 方块
     */
    private List<Position> buildEasyOccupiedPositions() {
        List<Position> positions = new ArrayList<>(32);
        addBlockPositions(positions, 1, 1, 4, 4);
        addBlockPositions(positions, 6, 6, 4, 4);
        return positions;
    }

    private void addBlockPositions(List<Position> positions, int startRow, int startCol, int height, int width) {
        for (int r = startRow; r < startRow + height; r++) {
            for (int c = startCol; c < startCol + width; c++) {
                positions.add(new Position(r, c));
            }
        }
    }

    /**
     * 构建图案ID列表, 仍然有回退，被{@link #randomEasyBoard}调用
     */
    private List<Integer> buildPatternIdsForPositions(int tileCount, Constants.Theme safeTheme, int patternCount) {
        List<Integer> ids = new ArrayList<>(tileCount);
        if (safeTheme == Constants.Theme.THEME2) {
            int pairSlots = tileCount / 2;
            int uniqueTarget = Math.min(pairSlots, Math.max(8, patternCount * 2));
            List<Integer> selectedIds = Theme2VocabularyData.randomEntryIds(uniqueTarget);
            if (selectedIds.isEmpty()) {
                for (int i = 0; i < tileCount / 2; i++) {
                int pattern = (i % patternCount) + 1;
                ids.add(pattern);
                ids.add(pattern);
                }
                return ids;
            }
            for (int i = 0; i < pairSlots; i++) {
                int vocabId = selectedIds.get(i % selectedIds.size());
                ids.add(Theme2VocabularyData.englishPatternId(vocabId));
                ids.add(Theme2VocabularyData.chinesePatternId(vocabId));
            }
            return ids;
        }
        if (safeTheme == Constants.Theme.THEME3) {
            int pairSlots = tileCount / 2;
            int uniqueTarget = Math.min(pairSlots, Math.max(8, patternCount * 2));
            List<Integer> selectedIds = Theme3PoemData.randomEntryIds(uniqueTarget);
            if (selectedIds.isEmpty()) {
                for (int i = 0; i < tileCount / 2; i++) {
                int pattern = (i % patternCount) + 1;
                ids.add(pattern);
                ids.add(pattern);
                }
                return ids;
            }
            for (int i = 0; i < pairSlots; i++) {
                int poemId = selectedIds.get(i % selectedIds.size());
                ids.add(Theme3PoemData.firstPatternId(poemId));
                ids.add(Theme3PoemData.secondPatternId(poemId));
            }
            return ids;
        }
        if (safeTheme == Constants.Theme.THEME4) {
            int pairSlots = tileCount / 2;
            int uniqueTarget = Math.min(pairSlots, Math.max(8, patternCount * 2));
            List<Integer> selectedIds = Theme4YauData.randomEntryIds(uniqueTarget);
            if (selectedIds.isEmpty()) {
                for (int i = 0; i < tileCount / 2; i++) {
                int pattern = (i % patternCount) + 1;
                ids.add(pattern);
                ids.add(pattern);
                }
                return ids;
            }
            for (int i = 0; i < pairSlots; i++) {
                int yauId = selectedIds.get(i % selectedIds.size());
                ids.add(Theme4YauData.firstPatternId(yauId));
                ids.add(Theme4YauData.secondPatternId(yauId));
            }
            return ids;
        }


        if (safeTheme == Constants.Theme.THEME1) {
            for (int i = 0; i < tileCount / 2; i++) {
                int pattern = (i % patternCount) + 1;
                ids.add(pattern);
                ids.add(pattern);
            }
        }
        return ids;
    }

    /**
     * 模拟消除以判断棋盘是否可消
     *
     * <p>通过 {@link DeadEndDetector#findAnyPair} 寻找可消对直至无
     *
     * <p>{@code guard} 保证不会死循环 (应该不会用到)
     *
     * @param board  <em>copy</em> 一个副本，以免修改原棋盘
     * @return {@code true} 若完全可消
     */
    private boolean isCleanable(GameBoard board) {

        // 最多(rows×cols / 2) 次，但乘以8保证足够
        int guard = board.rows() * board.cols() * 4;

        while (board.remainingTiles() > 0 && guard-- > 0) {
            Position[] pair = deadEndDetector.findAnyPair(board);
            if (pair == null) {
                return false;
            }

            // 复查
            if (pathFinder.findPath(board, pair[0], pair[1]) == null) {
                return false;
            }
            board.clear(pair[0]);
            board.clear(pair[1]);
        }

        return board.remainingTiles() == 0;
    }
}
