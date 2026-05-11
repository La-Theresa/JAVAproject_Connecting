package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表示连接路径，按顺序保存关键节点（起点、转角点、终点）
 *
 * <p>节点由 {@link logic.PathFinder#buildCompactWaypoints} 保证无共线点，
 * 因此转角数 = points.size() - 2
 */
public class Path implements Serializable {
    private final List<Position> points;

    public Path(List<Position> points) {
        this.points = new ArrayList<>(points);
    }

    public List<Position> points() {
        return Collections.unmodifiableList(points);
    }

    public int turns() {
        return Math.max(0, points.size() - 2);
    }
}
