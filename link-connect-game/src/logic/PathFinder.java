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
     * 判断两格是否可连接,构建路径细节,
     * 返回 {@code null} 若不存在路径。
     *
     * @param board
     * @param start
     * @param end
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
     * 检查两格是否可连接，不构建路径细节。
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

        /**
         * 沿四个方向扫描潜在转折点 p。
         * 首先考虑较远的方向
         * 对每条射线在{start, end}选择 origin，使 p 朝远离 target 的方向移动，
         * 从而 corner -> target 的直线段只会变长，一旦有阻挡就可以永久剪枝。
         */
        
        for (int d = 0; d < 4; d++) {
            boolean verticalRay = DR[d] != 0;
            boolean horizontalRay = DC[d] != 0;

            Position origin, target;
            if (verticalRay) {
                boolean awayFromEnd = (DR[d] < 0 && start.row() < end.row())
                                   || (DR[d] > 0 && start.row() > end.row());
                origin = awayFromEnd ? start : end;
                target = awayFromEnd ? end : start;
            } else {
                boolean awayFromEnd = (DC[d] < 0 && start.col() < end.col())
                                   || (DC[d] > 0 && start.col() > end.col());
                origin = awayFromEnd ? start : end;
                target = awayFromEnd ? end : start;
            }

            // corner1 = (p.row, target.col): 在水平射线上不变
            // corner2 = (target.row, p.col): 在竖直射线上不变
            // 固定角落的 isPassableRC 结果可永久剪枝；corner -> target 直线始终可以剪枝（p 远离 target）
            boolean corner1PassableBlocked = false;
            boolean corner2PassableBlocked = false;
            boolean corner1ClearBlocked = false;
            boolean corner2ClearBlocked = false;

            int r = origin.row();
            int c = origin.col();

            int times = 0;

            while (true) {
                r += DR[d];
                c += DC[d];

                if (!isWithinExtended(board, r, c)) {
                    break;
                }

                if (!isPassableRC(board, r, c, origin, target)) {
                    break;
                }

                times++;

                // 2 折路径: origin -> p -> (p.row, target.col) -> target 对于竖直射线
                int corner1r = r;
                int corner1c = target.col();
                if (verticalRay && !corner1ClearBlocked && !corner1PassableBlocked) {
                    if (!isPassableRC(board, corner1r, corner1c, origin, target)) {
                        corner1PassableBlocked = true;
                    } else { // 第2次循环开始无需重复判断先前位置是否为空
                        if (times == 1 && !isStraightClear(board, corner1r, corner1c, target.row(), target.col(), origin, target)) {
                            corner1ClearBlocked = true;
                        } else if (isStraightClear(board, r, c, corner1r, corner1c, origin, target)) {
                            return origin == start
                                    ? buildCompactWaypoints(start,
                                            new Position(r, c),
                                            new Position(corner1r, corner1c),
                                            end)
                                    : buildCompactWaypoints(target,
                                            new Position(corner1r, corner1c),
                                            new Position(r, c),
                                            origin);
                        }
                    }
                }

                // 2 折路径: origin -> p -> (target.row, p.col) -> target 对于水平射线
                int corner2r = target.row();
                int corner2c = c;
                if (horizontalRay && !corner2ClearBlocked && !corner2PassableBlocked) {
                    if (!isPassableRC(board, corner2r, corner2c, origin, target)) {
                            corner2PassableBlocked = true;
                    } else {
                        if (times == 1 && !isStraightClear(board, corner2r, corner2c, target.row(), target.col(), origin, target)) {
                            corner2ClearBlocked = true;
                        } else if (isStraightClear(board, r, c, corner2r, corner2c, origin, target)) {
                            return origin == start
                                    ? buildCompactWaypoints(start,
                                            new Position(r, c),
                                            new Position(corner2r, corner2c),
                                            end)
                                    : buildCompactWaypoints(target,
                                            new Position(corner2r, corner2c),
                                            new Position(r, c),
                                            origin);
                        }
                    }
                }

                boolean corner1Hopeless = corner1ClearBlocked || (verticalRay && corner1PassableBlocked);
                boolean corner2Hopeless = corner2ClearBlocked || (horizontalRay && corner2PassableBlocked);
                if (corner1Hopeless && corner2Hopeless) {
                    break;
                }
            }
        }

        /**
         * 其次考虑较近路线
         * 在 start -> end 之间扫描 2 折路径（转角落在起终点矩形内）。
         * 此时 p 位于 start 与 end 之间，利用 findBlocker 跳过阻挡格快速寻找。
         */

        // corner1: start -> p -> (p.row, end.col) -> end — 仅竖直射线（水平已在 1 折判断）
        if (start.row() != end.row()) {
            int dr = Integer.compare(end.row(), start.row());
            int r = start.row() + dr;
            int c = start.col();

            Position blocker = findBlocker(board, end.row(), end.col(), r, end.col(), start, end);
            int times = 0;

            while ((dr > 0 && r < end.row()) || (dr < 0 && r > end.row())) {
                times++;
                if (!isPassableRC(board, r, c, start, end)) {
                    break;
                }
                
                if (times == 1 && blocker == null) {
                    if (findBlocker(board, r, c, r, end.col(), start, end) == null && isPassableRC(board, r, end.col(), start, end)) {
                        return buildCompactWaypoints(start, new Position(r, c), new Position(r, end.col()), end);
                    }
                } else {
                    r = (times == 1) ? blocker.row() + dr : r + dr; // 第1次循环时跳过阻挡格，之后正常递增
                    if (!isPassableRC(board, r, c, start, end)) {
                    break;
                    }
                    if ((dr > 0 && r >= end.row()) || (dr < 0 && r <= end.row())) {
                        break;
                    }
                    if (findBlocker(board, start.row(), start.col(), r, c, start, end) != null) {
                        break;
                    }
                    if (findBlocker(board, r, c, r, end.col(), start, end) == null && isPassableRC(board, r, end.col(), start, end)) {
                        return buildCompactWaypoints(start, new Position(r, c), new Position(r, end.col()), end);
                    }
                }
            }
        }

        // corner2: start -> p -> (end.row, p.col) -> end — 仅水平射线（竖直已在 1 折判断）
        if (start.col() != end.col()) {
            int dc = Integer.compare(end.col(), start.col());
            int r = start.row();
            int c = start.col() + dc;

            Position blocker = findBlocker(board, end.row(), end.col(), end.row(), c, start, end);
            int times = 0;

            while ((dc > 0 && c < end.col()) || (dc < 0 && c > end.col())) {
                times++;
                if (!isPassableRC(board, r, c, start, end)) {
                    break;
                }
                
                if (times == 1 && blocker == null) {
                    if (findBlocker(board, r, c, end.row(), c, start, end) == null && isPassableRC(board, end.row(), c, start, end)) {
                        return buildCompactWaypoints(start, new Position(r, c), new Position(end.row(), c), end);
                    }
                } else {
                    c = (times == 1) ? blocker.col() + dc : c + dc; // 第1次循环时跳过阻挡格，之后正常递增
                    if (!isPassableRC(board, r, c, start, end)) {
                    break;
                    }
                    if ((dc > 0 && c >= end.col()) || (dc < 0 && c <= end.col())) {
                        break;
                    }
                    if (findBlocker(board, start.row(), start.col(), r, c, start, end) != null) {
                        break;
                    }
                    if (findBlocker(board, r, c, end.row(), c, start, end) == null && isPassableRC(board, end.row(), c, start, end)) {
                        return buildCompactWaypoints(start, new Position(r, c), new Position(end.row(), c), end);
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
            unique.add(p);
        }

        return unique;
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
     * 确认二者直线相通。
     */
    private boolean isStraightClear(GameBoard board,
                                    int aRow,
                                    int aCol,
                                    int bRow,
                                    int bCol,
                                    Position start,
                                    Position end) {
        if (aRow != bRow && aCol != bCol) {
            return false;
        }
        return findBlocker(board, aRow, aCol, bRow, bCol, start, end) == null;
    }

    /**
     * 检测水平/竖直线段上的第一个阻挡格。
     *
     * <p>调用方保证 (aRow,aCol)→(bRow,bCol) 同行或同列。
     *
     * @return 阻挡格坐标，全程通畅则返回 {@code null}
     */
    private Position findBlocker(GameBoard board,
                                 int aRow,
                                 int aCol,
                                 int bRow,
                                 int bCol,
                                 Position start,
                                 Position end) {
        if (aRow == bRow && aCol == bCol) {
            return null;
        }

        int rowStep = Integer.compare(bRow, aRow);
        int colStep = Integer.compare(bCol, aCol);
        int r = aRow + rowStep;
        int c = aCol + colStep;
        while (r != bRow || c != bCol) {
            if (!isPassableRC(board, r, c, start, end)) {
                return new Position(r, c);
            }
            r += rowStep;
            c += colStep;
        }
        return null;
    }

}
