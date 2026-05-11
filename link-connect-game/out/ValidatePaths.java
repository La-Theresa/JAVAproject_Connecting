import logic.PathFinder;
import logic.BoardGenerator;
import model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ValidatePaths {
    static PathFinder pf = new PathFinder();
    
    public static void main(String[] args) {
        BoardGenerator gen = new BoardGenerator(pf);
        int errors = 0;
        int total = 500;
        
        for (int k = 0; k < total; k++) {
            GameBoard board = gen.generate(Constants.Difficulty.HARD, Constants.Theme.THEME1);
            for (Map.Entry<Integer, Set<Position>> entry : board.patternEntries()) {
                List<Position> positions = new ArrayList<>(entry.getValue());
                for (int i = 0; i < positions.size(); i++) {
                    for (int j = i + 1; j < positions.size(); j++) {
                        Path path = pf.findPath(board, positions.get(i), positions.get(j));
                        if (path != null) {
                            if (!validatePath(board, path, positions.get(i), positions.get(j))) {
                                errors++;
                                if (errors <= 3) {
                                    System.out.println("INVALID PATH on board " + k);
                                    System.out.println("  start=" + positions.get(i) + " end=" + positions.get(j));
                                    System.out.println("  waypoints=" + path.points());
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (errors == 0) {
            System.out.println("All " + total + " boards validated OK — no invalid paths");
        } else {
            System.out.println("FOUND " + errors + " INVALID PATHS across " + total + " boards");
        }
    }
    
    static boolean validatePath(GameBoard board, Path path, Position start, Position end) {
        List<Position> points = path.points();
        if (points.isEmpty()) return false;
        if (!points.get(0).equals(start)) return false;
        if (!points.get(points.size() - 1).equals(end)) return false;
        if (path.turns() > 2) return false;
        
        for (int i = 0; i < points.size() - 1; i++) {
            Position a = points.get(i);
            Position b = points.get(i + 1);
            if (a.row() != b.row() && a.col() != b.col()) return false;
            
            int dr = Integer.compare(b.row(), a.row());
            int dc = Integer.compare(b.col(), a.col());
            int r = a.row() + dr;
            int c = a.col() + dc;
            while (r != b.row() || c != b.col()) {
                if (r >= 0 && r < board.rows() && c >= 0 && c < board.cols() && !board.isEmpty(new Position(r, c))) {
                    return false;
                }
                r += dr;
                c += dc;
            }
        }
        return true;
    }
}
