package logic;

import model.GameBoard;
import model.Path;
import model.Position;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路径查找器：支持0/1/2转角，最多三段直线。
 */
public class PathFinder {
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    /**
     * 查找两点连通路径，成功返回路径，失败返回null。
     */
    public Path findPath(GameBoard board, Position start, Position end) {
        if (start.equals(end)) {
            return null;
        }
        if (!board.isValid(start) || !board.isValid(end)) {
            return null;
        }
        if (board.isEmpty(start) || board.isEmpty(end)) {
            return null;
        }
        if (!board.tileAt(start).hasSamePattern(board.tileAt(end))) {
            return null;
        }

        int rows = board.rows() + 2;
        int cols = board.cols() + 2;

        Position s = shift(start);
        Position t = shift(end);

        int[][][] bestTurns = new int[rows][cols][4];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                for (int d = 0; d < 4; d++) {
                    bestTurns[r][c][d] = Integer.MAX_VALUE;
                }
            }
        }

        Deque<State> queue = new ArrayDeque<>();
        Map<State, State> parent = new HashMap<>();

        for (int d = 0; d < 4; d++) {
            State init = new State(s.row(), s.col(), d, 0);
            bestTurns[s.row()][s.col()][d] = 0;
            queue.offer(init);
        }

        while (!queue.isEmpty()) {
            State cur = queue.pollFirst();
            for (int nd = 0; nd < 4; nd++) {
                int nr = cur.row + DR[nd];
                int nc = cur.col + DC[nd];
                int nt = cur.turns + (cur.dir == nd ? 0 : 1);
                if (nt > 2) {
                    continue;
                }
                if (!isWalkable(board, nr, nc, t)) {
                    continue;
                }
                if (nt >= bestTurns[nr][nc][nd]) {
                    continue;
                }
                State next = new State(nr, nc, nd, nt);
                bestTurns[nr][nc][nd] = nt;
                parent.put(next, cur);
                if (nr == t.row() && nc == t.col()) {
                    return rebuildPath(parent, next);
                }
                if (cur.dir == nd) {
                    queue.offerFirst(next);
                } else {
                    queue.offerLast(next);
                }
            }
        }
        return null;
    }

    /**
     * 判断两点是否可连。
     */
    public boolean canConnect(GameBoard board, Position start, Position end) {
        return findPath(board, start, end) != null;
    }

    private Position shift(Position p) {
        return new Position(p.row() + 1, p.col() + 1);
    }

    private Position unshift(Position p) {
        return new Position(p.row() - 1, p.col() - 1);
    }

    private boolean isWalkable(GameBoard board, int sr, int sc, Position targetShifted) {
        int rows = board.rows() + 2;
        int cols = board.cols() + 2;
        if (sr < 0 || sr >= rows || sc < 0 || sc >= cols) {
            return false;
        }
        if (sr == targetShifted.row() && sc == targetShifted.col()) {
            return true;
        }
        if (sr == 0 || sc == 0 || sr == rows - 1 || sc == cols - 1) {
            return true;
        }
        Position original = new Position(sr - 1, sc - 1);
        return board.isEmpty(original);
    }

    private Path rebuildPath(Map<State, State> parent, State end) {
        List<Position> shifted = new ArrayList<>();
        State cur = end;
        shifted.add(new Position(cur.row, cur.col));
        while (parent.containsKey(cur)) {
            cur = parent.get(cur);
            shifted.add(new Position(cur.row, cur.col));
        }
        List<Position> path = new ArrayList<>();
        Position prevDir = null;
        for (int i = shifted.size() - 1; i >= 0; i--) {
            Position current = shifted.get(i);
            Position unshifted = unshift(current);
            if (path.isEmpty()) {
                path.add(unshifted);
                continue;
            }
            Position last = path.get(path.size() - 1);
            Position currentDir = new Position(
                    Integer.compare(unshifted.row() - last.row(), 0),
                    Integer.compare(unshifted.col() - last.col(), 0)
            );
            if (prevDir == null || !prevDir.equals(currentDir)) {
                path.add(unshifted);
            } else {
                path.set(path.size() - 1, unshifted);
            }
            prevDir = currentDir;
        }
        return new Path(path);
    }

    /**
     * BFS节点状态。
     */
    private static final class State {
        private final int row;
        private final int col;
        private final int dir;
        private final int turns;

        private State(int row, int col, int dir, int turns) {
            this.row = row;
            this.col = col;
            this.dir = dir;
            this.turns = turns;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof State)) {
                return false;
            }
            State o = (State) obj;
            return row == o.row && col == o.col && dir == o.dir && turns == o.turns;
        }

        @Override
        public int hashCode() {
            int h = row;
            h = h * 31 + col;
            h = h * 31 + dir;
            h = h * 31 + turns;
            return h;
        }
    }
}
