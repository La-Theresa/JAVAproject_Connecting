package logic;

import model.GameBoard;
import model.Path;
import model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * PathFinder implements the core pathfinding logic for the Link Connect game.
 *
 * <p>It determines whether two tiles can be connected by a path with at most 2
 * direction changes (turns), and returns the connecting path as a sequence of
 * key waypoints if one exists.
 *
 * <p>This implementation uses a single geometric probing strategy based on the
 * at-most-2-turns rule. It can both:
 * <ul>
 *   <li>answer existence queries quickly ({@link #canConnect(GameBoard, Position, Position)}), and</li>
 *   <li>build drawable key-waypoint paths ({@link #findPath(GameBoard, Position, Position)}).</li>
 * </ul>
 */
public class PathFinder {

    /** Row deltas for the 4 movement directions: up, down, left, right. */
    private static final int[] DR = {-1, 1, 0, 0};

    /** Column deltas for the 4 movement directions: up, down, left, right. */
    private static final int[] DC = {0, 0, -1, 1};

    /**
     * Finds a valid path connecting {@code start} to {@code end} on the board,
     * respecting the at-most-2-turns constraint, or returns {@code null} if no
     * such path exists.
     *
     * <p>Pre-conditions checked:
     * <ul>
     *   <li>Start and end are not the same position.</li>
     *   <li>Both positions are within board bounds.</li>
     *   <li>Neither position is empty (patternId == 0).</li>
     *   <li>Both tiles carry the same pattern.</li>
     * </ul>
     *
     * @param board the current game board
     * @param start the first tile position (0-indexed row/col)
     * @param end   the second tile position (0-indexed row/col)
     * @return a {@link Path} of key waypoints if connectable, or {@code null}
     */
    public Path findPath(GameBoard board, Position start, Position end) {
        List<Position> fastWaypoints = findWaypointsByKeyCorners(board, start, end);
        if (fastWaypoints != null) {
            return new Path(fastWaypoints);
        }
        return null;
    }

    /**
     * Checks whether two positions on the board can be connected.
     *
     * <p>This method is an existence-only fast path:
     * <ol>
     *   <li>From {@code start}, scan along 4 rays in the extended board
     *       (including virtual border).</li>
     *   <li>For each ray point {@code p}, check if {@code p -> end} is directly clear
     *       (1 turn), or if either corner composition is clear:
     *       {@code p -> (p.row,end.col) -> end} / {@code p -> (end.row,p.col) -> end}
     *       (2 turns).</li>
     * </ol>
     *
     * <p>Because the game only needs an existence judgment in many hot paths,
     * this avoids full-board BFS expansion.
     *
     * @param board the current game board
     * @param start the first tile position
     * @param end   the second tile position
     * @return {@code true} if a valid connecting path exists
     */
    public boolean canConnect(GameBoard board, Position start, Position end) {
        return findWaypointsByKeyCorners(board, start, end) != null;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Uses directional probing to find a connectable route and returns compact
     * waypoints (start, up to two corners, end). Returns {@code null} if none.
     */
    private List<Position> findWaypointsByKeyCorners(GameBoard board, Position start, Position end) {
        if (!isPairConnectable(board, start, end)) {
            return null;
        }

        // 0-turn direct connection.
        if (isStraightClear(board, start.row(), start.col(), end.row(), end.col(), start, end)) {
            return buildCompactWaypoints(start, end);
        }

        // 1-turn fast precheck: only two L-shape corner candidates exist.
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

        // Directional probing from start: scan key candidates on four rays.
        // Jump pruning: if a corner branch's end-leg is direction-invariant and
        // already blocked, skip that branch for the rest of this ray.
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

                // 1-turn candidate: start -> p -> end.
                if (isStraightClear(board, r, c, end.row(), end.col(), start, end)) {
                    return buildCompactWaypoints(start, new Position(r, c), end);
                }

                // 2-turn candidate: start -> p -> (p.row, end.col) -> end.
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

                // 2-turn candidate: start -> p -> (end.row, p.col) -> end.
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
     * Shared precondition check for connectivity/path queries.
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
     * Builds a compact waypoint list by removing duplicate points and
     * collapsing collinear middle points.
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
     * Returns {@code true} if a coordinate is inside the logical board extended
     * by one virtual border cell on each side.
     */
    private boolean isWithinExtended(GameBoard board, int row, int col) {
        return row >= -1 && row <= board.rows() && col >= -1 && col <= board.cols();
    }

    /**
     * A probing point is passable when it is virtual border, start/end, or an
     * empty in-board cell.
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
     * Checks whether two points are aligned and all intermediate cells are
     * passable for a route segment.
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
