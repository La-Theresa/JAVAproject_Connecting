import logic.BoardGenerator;
import logic.DeadEndDetector;
import logic.PathFinder;
import model.*;

public class ProfileThemes {
    public static void main(String[] args) {
        PathFinder pf = new PathFinder();
        BoardGenerator gen = new BoardGenerator(pf);
        DeadEndDetector ded = new DeadEndDetector(pf);
        Constants.Theme[] themes = {Constants.Theme.THEME1, Constants.Theme.THEME2, Constants.Theme.THEME3, Constants.Theme.THEME4};
        
        for (Constants.Theme theme : themes) {
            // Time board generation
            long t0 = System.nanoTime();
            GameBoard board = gen.generate(Constants.Difficulty.HARD, theme);
            long t1 = System.nanoTime();
            
            // Time isCleanable via deep copy simulation
            long tClean = 0;
            int cleanCalls = 0;
            long tFindPair = 0;
            int pairCalls = 0;
            
            GameBoard copy = board.deepCopy();
            while (copy.remainingTiles() > 0) {
                cleanCalls++;
                long tp0 = System.nanoTime();
                Position[] pair = ded.findAnyPair(copy);
                long tp1 = System.nanoTime();
                tFindPair += (tp1 - tp0);
                pairCalls++;
                if (pair == null) break;
                copy.clear(pair[0]);
                copy.clear(pair[1]);
            }
            tClean = System.nanoTime() - t1;
            
            System.out.println(theme + ":");
            System.out.println("  board gen: " + (t1-t0)/1e6 + " ms");
            System.out.println("  cleanable total: " + tClean/1e6 + " ms, calls=" + cleanCalls);
            System.out.println("  findAnyPair total: " + tFindPair/1e6 + " ms, calls=" + pairCalls + 
                             ", avg=" + String.format("%.3f", tFindPair/1e6/pairCalls) + " ms");
            System.out.println("  board size: " + board.rows() + "x" + board.cols() +
                             ", remaining=" + board.remainingTiles());
            System.out.println("  pattern entries: " + board.patternEntries().size());
        }
    }
}
