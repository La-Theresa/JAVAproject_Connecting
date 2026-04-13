package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表示连接路径，按顺序保存关键节点。
 */
public class Path implements Serializable {
    private final List<Position> points;

    public Path(List<Position> points) {
        this.points = new ArrayList<>(points);
    }

    /**
     * 返回不可变路径点集合。
     */
    public List<Position> points() {
        return Collections.unmodifiableList(points);
    }

    /**
     * 计算路径转角数。
     */
    public int turns() {
        if (points.size() < 3) {
            return 0;
        }
        int turns = 0;
        for (int i = 1; i < points.size() - 1; i++) {
            Position prev = points.get(i - 1);
            Position curr = points.get(i);
            Position next = points.get(i + 1);
            int dr1 = Integer.compare(curr.row() - prev.row(), 0);
            int dc1 = Integer.compare(curr.col() - prev.col(), 0);
            int dr2 = Integer.compare(next.row() - curr.row(), 0);
            int dc2 = Integer.compare(next.col() - curr.col(), 0);
            if (dr1 != dr2 || dc1 != dc2) {
                turns++;
            }
        }
        return turns;
    }
}
