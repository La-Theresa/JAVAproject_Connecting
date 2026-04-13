package logic;

import model.GameBoard;
import model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 死局检测器与提示对查找。
 */
public class DeadEndDetector {
    private final PathFinder pathFinder;

    public DeadEndDetector(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }

    /**
     * 判断当前棋盘是否还有至少一对可消除方块。
     */
    public boolean hasAnyValidMove(GameBoard board) {
        return findAnyPair(board) != null;
    }

    /**
     * 返回任意一对可消除方块位置，不存在时返回null。
     */
    public Position[] findAnyPair(GameBoard board) {
        List<Position> nonEmpty = board.nonEmptyPositions();
        for (Position p : nonEmpty) {
            int pattern = board.tileAt(p).patternId();
            Set<Position> group = board.getPatternPositions(pattern);
            for (Position q : new ArrayList<>(group)) {
                if (p.equals(q)) {
                    continue;
                }
                if (pathFinder.canConnect(board, p, q)) {
                    return new Position[]{p, q};
                }
            }
        }
        return null;
    }
}
