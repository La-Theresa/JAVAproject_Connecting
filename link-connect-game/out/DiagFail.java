import logic.DeadEndDetector;
import logic.PathFinder;
import logic.BoardGenerator;
import model.*;
import java.util.*;

public class DiagFail {
    public static void main(String[] args) {
        PathFinder pf = new PathFinder();
        BoardGenerator gen = new BoardGenerator(pf);
        DeadEndDetector ded = new DeadEndDetector(pf);
        
        int failures = 0, totalAttempts = 0;
        for (int k = 0; k < 200; k++) {
            totalAttempts++;
            GameBoard board = gen.generate(Constants.Difficulty.HARD, Constants.Theme.THEME2);
        }
        System.out.println("(warmup done)");
        
        // Now generate a board and trace isCleanable
        for (int k = 0; k < 50; k++) {
            // Use reflection to call randomBoard
            GameBoard board = null;
            try {
                var m = BoardGenerator.class.getDeclaredMethod("randomBoard",
                    Constants.Difficulty.class, Constants.Theme.class);
                m.setAccessible(true);
                board = (GameBoard) m.invoke(gen, Constants.Difficulty.HARD, Constants.Theme.THEME2);
            } catch(Exception e) { e.printStackTrace(); return; }
            
            GameBoard copy = board.deepCopy();
            
            while (copy.remainingTiles() > 0) {
                Position[] pair = ded.findAnyPair(copy);
                if (pair == null) {
                    failures++;
                    System.out.println("FAIL at remaining=" + copy.remainingTiles());
                    System.out.println("  patternEntries: " + copy.patternEntries().size());
                    // Print all remaining pattern groups
                    int total = 0;
                    for (var entry : copy.patternEntries()) {
                        int sz = entry.getValue().size();
                        if (sz > 0) {
                            total += sz;
                            System.out.println("  pattern " + entry.getKey() + ": " + sz + " tiles");
                        }
                    }
                    System.out.println("  total tiles: " + total);
                    break;
                }
                copy.clear(pair[0]);
                copy.clear(pair[1]);
            }
            if (failures >= 5) break;
        }
        System.out.println("Failures: " + failures + " / 50 boards");
    }
}
