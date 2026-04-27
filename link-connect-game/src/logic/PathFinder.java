package logic;

import model.GameBoard;
import model.Path;
import model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * PathFinder 是这里的核心逻辑
 *
 * <p>判断是否可连，返回路径或NULL
 *
 * <p>通过两折的路径限制
 * <ul>
 *   <li>判断存在 ({@link #canConnect(GameBoard, Position, Position)}), and</li>
 *   <li>创造路径以显示 ({@link #findPath(GameBoard, Position, Position)}).</li>
 * </ul>
 */
public class PathFinder {

    /** 后续水平移动参数 */
    private static final int[] DR = {-1, 1, 0, 0};

    /** 后续垂直移动参数 */
    private static final int[] DC = {0, 0, -1, 1};

    /**
     * 判断两格是否可连接（存在满足条件的路径），不构建路径细节。
     * 返回 {@code null} 若不存在路径。
     *
     * <p>条件判断:
     * <ul>
     *   <li>不为同一点</li>
     *   <li>在棋盘内</li>
     *   <li>非空</li>
     *   <li>相同图案</li>
     * </ul>
     *
     * @param board 当前棋盘
     * @param start 第一格位置 (0-indexed行列)
     * @param end   第二格位置 (0-indexed行列)
     * @return 路径节点 {@link Path} 或 {@code null}
     */
    public Path findPath(GameBoard board, Position start, Position end) {
        List<Position> fastWaypoints = findWaypointsByKeyCorners(board, start, end);
        if (fastWaypoints != null) {
            return new Path(fastWaypoints);
        }
        return null;
    }

    /**
     * 检查两格是否可连接（存在满足条件的路径），不构建路径细节。
     *
     * <p>实现思路：基于最多两转的规则，使用几何寻找连接路径：
     * <ol>
     *   <li>从点 {@code start}, 沿四个方向射线扫描合法转折点
     *       (包括边界).</li>
     *   <li>在每个折点 {@code p}, 判断{@code p -> end} 是否可连
     *       (1 折), 或者存在2 折路径:
     *       {@code p -> (p.row,end.col) -> end} / {@code p -> (end.row,p.col) -> end}.</li>
     * </ol>
     *
     * <p>这可以优化BFS的搜索空间，避免了冗余路径探索。
     *
     * @param board 当前棋盘
     * @param start 起点 (选择的第一个点)
     * @param end   终点 (选择的第二个点)
     * @return {@code true} 存在路径
     */
    public boolean canConnect(GameBoard board, Position start, Position end) {
        return findWaypointsByKeyCorners(board, start, end) != null;
    }

    // -------------------------------------------------------------------------
    // 具体实现方法
    // -------------------------------------------------------------------------

    /**
     * 使用关键转折点（起点、终点、两候选转折点、以及沿四射线的潜在转折点）寻找连接路径。
     * 如果存在，返回包含起点、转折点、终点的路径 {@link #buildCompactWaypoints(Position...)}；否则返回 {@code null}。
     */
    private List<Position> findWaypointsByKeyCorners(GameBoard board, Position start, Position end) {
        if (!isPairConnectable(board, start, end)) {
            return null;
        }

        // 0 折点连接：直接判断两者是否相通。
        if (isStraightClear(board, start.row(), start.col(), end.row(), end.col(), start, end)) {
            return buildCompactWaypoints(start, end);
        }

        // 1 折点连接：尝试两个候选转折点 (start.row, end.col) 和 (end.row, start.col)，
        int c1r = start.row();
        int c1c = end.col();
        if (isPassableRC(board, c1r, c1c, start, end)
                && isStraightClear(board, start.row(), start.col(), c1r, c1c, start, end)
                && isStraightClear(board, c1r, c1c, end.row(), end.col(), start, end)) {
            return buildCompactWaypoints(start, new Position(c1r, c1c), end);
        }

        int c2r = end.row();
        int c2c = start.col();
        if (isPassableRC(board, c2r, c2c, start, end)
                && isStraightClear(board, start.row(), start.col(), c2r, c2c, start, end)
                && isStraightClear(board, c2r, c2c, end.row(), end.col(), start, end)) {
            return buildCompactWaypoints(start, new Position(c2r, c2c), end);
        }

        // 考虑四个方向的射线，寻找潜在的转折点 p。
        // 对于每个 p，尝试两个转折组合，并利用剪枝优化同一分支的后续 p。
        for (int d = 0; d < 4; d++) {
            int r = start.row();
            int c = start.col();
            boolean horizontalRay = DR[d] == 0;
            boolean verticalRay = DC[d] == 0;
            boolean pruneCorner1Branch = false;
            boolean pruneCorner2Branch = false;

            while (true) {
                r += DR[d];
                c += DC[d];

                if (!isWithinExtended(board, r, c)) {
                    break;
                }

                if (!isPassableRC(board, r, c, start, end)) {
                    break;
                }

                // 1 折点路径: start -> p -> end. 已经在外层循环里直接判断了。

                // 2 折点路径: start -> p -> (p.row, end.col) -> end.
                // 对于上下射线的情况重复，但为了代码对称性和清晰度保留。
                int corner1r = r;
                int corner1c = end.col();
                if (!pruneCorner1Branch) {
                    if (!isPassableRC(board, corner1r, corner1c, start, end)) {
                        // For left/right rays, corner1 is fixed and will stay blocked.
                        if (horizontalRay) {
                            pruneCorner1Branch = true;
                        }
                    } else {
                        boolean corner1ToEndClear = isStraightClear(
                                board,
                                corner1r,
                                corner1c,
                                end.row(),
                                end.col(),
                                start,
                                end
                        );
                        if (!corner1ToEndClear) {
                            // For left/right rays, this end-leg is fixed across the ray.
                            if (horizontalRay) {
                                pruneCorner1Branch = true;
                            }
                        } else if (isStraightClear(board, r, c, corner1r, corner1c, start, end)) {
                            return buildCompactWaypoints(
                                    start,
                                    new Position(r, c),
                                    new Position(corner1r, corner1c),
                                    end
                            );
                        }
                    }
                }

                // 2 折点路径: start -> p -> (end.row, p.col) -> end.
                // 对于竖射线的情况重复，但为了代码对称性和清晰度保留。
                int corner2r = end.row();
                int corner2c = c;
                if (!pruneCorner2Branch) {
                    if (!isPassableRC(board, corner2r, corner2c, start, end)) {
                        // For up/down rays, corner2 is fixed and will stay blocked.
                        if (verticalRay) {
                            pruneCorner2Branch = true;
                        }
                    } else {
                        boolean corner2ToEndClear = isStraightClear(
                                board,
                                corner2r,
                                corner2c,
                                end.row(),
                                end.col(),
                                start,
                                end
                        );
                        if (!corner2ToEndClear) {
                            // For up/down rays, this end-leg is fixed across the ray.
                            if (verticalRay) {
                                pruneCorner2Branch = true;
                            }
                        } else if (isStraightClear(board, r, c, corner2r, corner2c, start, end)) {
                            return buildCompactWaypoints(
                                    start,
                                    new Position(r, c),
                                    new Position(corner2r, corner2c),
                                    end
                            );
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 判断两格是否满足基本连接条件（同类、非空、不同格）。
     */
    private boolean isPairConnectable(GameBoard board, Position start, Position end) {
        if (start.equals(end)) {
            return false;
        }
        if (!board.isValid(start) || !board.isValid(end)) {
            return false;
        }
        if (board.isEmpty(start) || board.isEmpty(end)) {
            return false;
        }
        return board.canMatchPatterns(board.tileAt(start).patternId(), board.tileAt(end).patternId());
    }

    /**
     * 创造路径转折点和起点
     */
    private List<Position> buildCompactWaypoints(Position... candidates) {
        List<Position> unique = new ArrayList<>();
        for (Position p : candidates) {
            if (p == null) {
                continue;
            }
            if (!unique.isEmpty() && unique.get(unique.size() - 1).equals(p)) {
                continue;
            }
            unique.add(p);
        }

        if (unique.size() <= 2) {
            return unique;
        }

        List<Position> compact = new ArrayList<>();
        compact.add(unique.get(0));
        for (int i = 1; i < unique.size() - 1; i++) {
            Position prev = compact.get(compact.size() - 1);
            Position curr = unique.get(i);
            Position next = unique.get(i + 1);

            int dr1 = Integer.compare(curr.row() - prev.row(), 0);
            int dc1 = Integer.compare(curr.col() - prev.col(), 0);
            int dr2 = Integer.compare(next.row() - curr.row(), 0);
            int dc2 = Integer.compare(next.col() - curr.col(), 0);
            if (dr1 != dr2 || dc1 != dc2) {
                compact.add(curr);
            }
        }
        compact.add(unique.get(unique.size() - 1));
        return compact;
    }

    /**
     * 确认在合法的扩展范围内（包含虚拟边界）
     */
    private boolean isWithinExtended(GameBoard board, int row, int col) {
        return row >= -1 && row <= board.rows() && col >= -1 && col <= board.cols();
    }

    /**
     * 合法转折点
     */
    private boolean isPassableRC(GameBoard board, int row, int col, Position start, Position end) {
        if (row == -1 || row == board.rows() || col == -1 || col == board.cols()) {
            return true;
        }
        if (row < 0 || row >= board.rows() || col < 0 || col >= board.cols()) {
            return false;
        }
        if ((row == start.row() && col == start.col())
                || (row == end.row() && col == end.col())) {
            return true;
        }
        return board.isEmpty(new Position(row, col));
    }

    /**
     * 确认二者相通
     */
    private boolean isStraightClear(GameBoard board,
                                    int aRow,
                                    int aCol,
                                    int bRow,
                                    int bCol,
                                    Position start,
                                    Position end) {
        if (aRow == bRow && aCol == bCol) {
            return true;
        }
        // reject diagonals before any scanning loop
        if (aRow != bRow && aCol != bCol) {
            return false;
        }

        int rowStep = Integer.compare(bRow, aRow);
        int colStep = Integer.compare(bCol, aCol);
        int r = aRow + rowStep;
        int c = aCol + colStep;
        while (r != bRow || c != bCol) {
            if (!isPassableRC(board, r, c, start, end)) {
                return false;
            }
            r += rowStep;
            c += colStep;
        }
        return true;
    }

}
