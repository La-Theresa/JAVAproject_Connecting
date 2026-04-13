package logic;

import model.Constants;
import model.GameBoard;
import model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机可解棋盘生成器。
 */
public class BoardGenerator {
    private final PathFinder pathFinder;
    private final DeadEndDetector deadEndDetector;

    public BoardGenerator(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
        this.deadEndDetector = new DeadEndDetector(pathFinder);
    }

    /**
     * 生成指定难度棋盘，若生成失败会自动重试。
     */
    public GameBoard generate(Constants.Difficulty difficulty) {
        int maxRetries = 200;
        for (int i = 0; i < maxRetries; i++) {
            GameBoard board = randomBoard(difficulty);
            if (isCleanable(board.deepCopy())) {
                return board;
            }
        }
        return randomBoard(difficulty);
    }

    /**
     * 对当前棋盘剩余方块进行重排，避免死局。
     */
    public void reshuffle(GameBoard board) {
        List<Integer> remaining = new ArrayList<>();
        for (Position p : board.nonEmptyPositions()) {
            remaining.add(board.tileAt(p).patternId());
        }
        Collections.shuffle(remaining, ThreadLocalRandom.current());
        int idx = 0;
        for (Position p : board.nonEmptyPositions()) {
            board.setPattern(p, remaining.get(idx++));
        }
    }

    private GameBoard randomBoard(Constants.Difficulty difficulty) {
        int rows = difficulty.rows();
        int cols = difficulty.cols();
        int total = rows * cols;
        GameBoard board = new GameBoard(rows, cols);

        List<Integer> ids = new ArrayList<>(total);
        int patternCount = difficulty.patternCount();
        for (int i = 0; i < total / 2; i++) {
            int pattern = (i % patternCount) + 1;
            ids.add(pattern);
            ids.add(pattern);
        }
        Collections.shuffle(ids, ThreadLocalRandom.current());

        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                board.setPattern(new Position(r, c), ids.get(idx++));
            }
        }
        return board;
    }

    private boolean isCleanable(GameBoard board) {
        int guard = board.rows() * board.cols() * 4;
        while (board.remainingTiles() > 0 && guard-- > 0) {
            Position[] pair = deadEndDetector.findAnyPair(board);
            if (pair == null) {
                return false;
            }
            if (pathFinder.findPath(board, pair[0], pair[1]) == null) {
                return false;
            }
            board.clear(pair[0]);
            board.clear(pair[1]);
        }
        return board.remainingTiles() == 0;
    }
}
