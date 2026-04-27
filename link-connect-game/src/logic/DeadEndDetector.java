package logic;

import data.Theme2VocabularyData;
import data.Theme3PoemData;
import data.Theme4YauData;
import model.Constants;
import model.GameBoard;
import model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DeadEndDetector 判断是否死局，并可返回一个可连接的配对作 hint。
 *
 * <p>优化: 只判断C^{n}_{2}个存在块，这里显然</p>
 */
public class DeadEndDetector {

    private final PathFinder pathFinder;

    /**
     * 构造 DeadEndDetector
     *
     * @param pathFinder 用于检测路径的 PathFinder
     */
    public DeadEndDetector(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }

    /**
     * 返回 {@code true} 若存在可连对
     */
    public boolean hasAnyValidMove(GameBoard board) {
        return findAnyPair(board) != null;
    }

    /**
     * 寻找可连对并返回
     *
     * <p>对于存在块i 和 块j，只判断 {@code (i, j)} 中 {@code i < j}。避免对称重复检查。</p>
     * 
     * <p>对于不同theme, 直接复制到其他函数
     * 
     * @param board 游戏棋盘
     * @return 两个 {@code Position[]} {@code {p, q}} 代表可连对， 或 {@code null} 若无
     */
    public Position[] findAnyPair(GameBoard board) {
        if (board.theme() == Constants.Theme.THEME2) {
            return findTheme2Pair(board);
        }
        if (board.theme() == Constants.Theme.THEME3) {
            return findTheme3Pair(board);
        }
        if (board.theme() == Constants.Theme.THEME4) {
            return findTheme4Pair(board);
        }

        for (Map.Entry<Integer, Set<Position>> entry : board.patternEntries()) {
            List<Position> group = new ArrayList<>(entry.getValue());
            int size = group.size();

            // 该图案清空或仅剩1块，不可能有配对
            if (size < 2) {
                continue;
            }

            /*
             * 判断两个块 (i, j) 中 i < j
             */
            for (int i = 0; i < size - 1; i++) {
                for (int j = i + 1; j < size; j++) {
                    if (pathFinder.canConnect(board, group.get(i), group.get(j))) {
                        return new Position[]{group.get(i), group.get(j)};
                    }
                }
            }
        }
        return null;
    }

    /**
     * 查找 Theme2 中的可连接配对
     * 跨图案配对，调用 parrtern ID
     * @param board 游戏棋盘
     * @return 可连接的两个 Position，或 null 若无
     */
    private Position[] findTheme2Pair(GameBoard board) {
        for (Map.Entry<Integer, Set<Position>> entry : board.patternEntries()) {
            int pattern = entry.getKey();
            if (pattern <= 0) {
                continue;
            }

            int pairId = Theme2VocabularyData.pairId(pattern);
            if (pairId <= 0) {
                continue;
            }

            int firstPattern = Theme2VocabularyData.englishPatternId(pairId);
            int secondPattern = Theme2VocabularyData.chinesePatternId(pairId);

            Position[] pair = findConnectableAcrossTwoPatterns(board, firstPattern, secondPattern);
            if (pair != null) {
                return pair;
            }
        }
        return null;
    }

    /**
     * 查找 Theme3 中的可连接配对
     * 跨图案配对，调用 parrtern ID
     * @param board 游戏棋盘
     * @return 可连接的两个 Position，或 null 若无
     */
    private Position[] findTheme3Pair(GameBoard board) {
        for (Map.Entry<Integer, Set<Position>> entry : board.patternEntries()) {
            int pattern = entry.getKey();
            if (pattern <= 0) {
                continue;
            }

            int pairId = Theme3PoemData.pairId(pattern);
            if (pairId <= 0) {
                continue;
            }

            int firstPattern = Theme3PoemData.firstPatternId(pairId);
            int secondPattern = Theme3PoemData.secondPatternId(pairId);

            Position[] pair = findConnectableAcrossTwoPatterns(board, firstPattern, secondPattern);
            if (pair != null) {
                return pair;
            }
        }
        return null;
    }

    /**
     * 查找 Theme4中的可连接配对
     * 跨图案配对，调用 parrtern ID
     * @param board 游戏棋盘
     * @return 可连接的两个 Position，或 null 若无
     */
    private Position[] findTheme4Pair(GameBoard board) {
        for (Map.Entry<Integer, Set<Position>> entry : board.patternEntries()) {
            int pattern = entry.getKey();
            if (pattern <= 0) {
                continue;
            }

            int pairId = Theme4YauData.pairId(pattern);
            if (pairId <= 0) {
                continue;
            }

            int firstPattern = Theme4YauData.firstPatternId(pairId);
            int secondPattern = Theme4YauData.secondPatternId(pairId);

            Position[] pair = findConnectableAcrossTwoPatterns(board, firstPattern, secondPattern);
            if (pair != null) {
                return pair;
            }
        }
        return null;
    }

    /**
     * 在两种不同图案之间查找可连接配对
     *
     * @param board        游戏棋盘
     * @param firstPattern  第一个图案 ID
     * @param secondPattern 第二个图案 ID
     * @return 可连接的两个 Position，或 null 若无
     */
    private Position[] findConnectableAcrossTwoPatterns(GameBoard board, int firstPattern, int secondPattern) {
        if (firstPattern <= 0 || secondPattern <= 0) {
            return null;
        }

        Set<Position> firstSet = board.getPatternPositions(firstPattern);
        Set<Position> secondSet = board.getPatternPositions(secondPattern);
        if (firstSet.isEmpty() || secondSet.isEmpty()) {
            return null;
        }

        List<Position> firstList = new ArrayList<>(firstSet);
        List<Position> secondList = new ArrayList<>(secondSet);
        for (Position first : firstList) {
            for (Position second : secondList) {
                if (pathFinder.canConnect(board, first, second)) {
                    return new Position[]{first, second};
                }
            }
        }
        return null;
    }
}
