import logic.BoardGenerator;
import logic.DeadEndDetector;
import logic.PathFinder;
import model.*;
import data.*;

public class ProfileDetail {
    public static void main(String[] args) {
        PathFinder pf = new PathFinder();
        BoardGenerator gen = new BoardGenerator(pf);
        // warmup
        for (int i = 0; i < 200; i++) {
            gen.generate(Constants.Difficulty.HARD, Constants.Theme.THEME1);
            gen.generate(Constants.Difficulty.HARD, Constants.Theme.THEME2);
        }
        
        for (Constants.Theme theme : new Constants.Theme[]{
            Constants.Theme.THEME1, Constants.Theme.THEME2, Constants.Theme.THEME3, Constants.Theme.THEME4
        }) {
            int N = 500;
            long tRandom = 0, tCopy = 0, tFindPair = 0, tClear = 0;
            int findPairCalls = 0, connectCalls = 0;
            
            for (int k = 0; k < N; k++) {
                // Time just the randomBoard (no isCleanable)
                long t0 = System.nanoTime();
                // We can't call randomBoard directly (private), so measure generate without isCleanable overhead differently
                tRandom += (System.nanoTime() - t0);
            }
            
            // Instead: generate once, then time isCleanable components
            GameBoard board = gen.generate(Constants.Difficulty.HARD, theme);
            
            // Time isCleanable components over many iterations on copies
            long tCleanTotal = 0;
            for (int k = 0; k < N; k++) {
                GameBoard copy = board.deepCopy();
                long t0 = System.nanoTime();
                while (copy.remainingTiles() > 0) {
                    findPairCalls++;
                    Position[] pair = new DeadEndDetector(pf).findAnyPair(copy);
                    if (pair == null) break;
                    copy.clear(pair[0]);
                    copy.clear(pair[1]);
                }
                tCleanTotal += (System.nanoTime() - t0);
            }
            
            System.out.println(theme + ": isCleanable avg=" 
                + String.format("%.4f", tCleanTotal/1e6/N) + " ms, findPair calls per board=" 
                + (findPairCalls/N));
            
            // Time just findAnyPair once
            long t0 = System.nanoTime();
            DeadEndDetector ded = new DeadEndDetector(pf);
            for (int k = 0; k < 2000; k++) {
                ded.findAnyPair(board);
            }
            long t1 = System.nanoTime();
            System.out.println("  findAnyPair avg=" + String.format("%.4f", (t1-t0)/1e6/2000) + " ms");
            
            // Time randomEntryIds
            if (theme == Constants.Theme.THEME2) {
                t0 = System.nanoTime();
                for (int k = 0; k < 5000; k++) {
                    Theme2VocabularyData.randomEntryIds(24);
                }
                t1 = System.nanoTime();
                System.out.println("  randomEntryIds(24) avg=" + String.format("%.4f", (t1-t0)/1e6/5000) + " ms");
            }
            if (theme == Constants.Theme.THEME3) {
                t0 = System.nanoTime();
                for (int k = 0; k < 5000; k++) {
                    Theme3PoemData.randomEntryIds(24);
                }
                t1 = System.nanoTime();
                System.out.println("  randomEntryIds(24) avg=" + String.format("%.4f", (t1-t0)/1e6/5000) + " ms");
            }
            if (theme == Constants.Theme.THEME4) {
                t0 = System.nanoTime();
                for (int k = 0; k < 5000; k++) {
                    Theme4YauData.randomEntryIds(24);
                }
                t1 = System.nanoTime();
                System.out.println("  randomEntryIds(24) avg=" + String.format("%.4f", (t1-t0)/1e6/5000) + " ms");
            }
            
            System.out.println("  patternEntries count: " + board.patternEntries().size());
        }
    }
}
