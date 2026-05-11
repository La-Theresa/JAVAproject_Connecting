import logic.BoardGenerator;
import logic.DeadEndDetector;
import logic.PathFinder;
import model.*;

public class ProfileAll {
    public static void main(String[] args) throws Exception {
        PathFinder pf = new PathFinder();
        BoardGenerator gen = new BoardGenerator(pf);
        var method = BoardGenerator.class.getDeclaredMethod("randomBoard",
            Constants.Difficulty.class, Constants.Theme.class);
        method.setAccessible(true);
        var cleanMethod = BoardGenerator.class.getDeclaredMethod("isCleanable", GameBoard.class);
        cleanMethod.setAccessible(true);
        
        // warmup
        for (int i = 0; i < 500; i++)
            for (Constants.Theme t : Constants.Theme.values())
                gen.generate(Constants.Difficulty.HARD, t);
        
        for (Constants.Theme theme : Constants.Theme.values()) {
            int N = 1000;
            
            // Time full generate
            long t0 = System.nanoTime();
            for (int k = 0; k < N; k++)
                gen.generate(Constants.Difficulty.HARD, theme);
            long tFull = System.nanoTime() - t0;
            
            // Get a board to profile components
            GameBoard board = gen.generate(Constants.Difficulty.HARD, theme);
            
            // Time randomBoard
            t0 = System.nanoTime();
            for (int k = 0; k < N; k++)
                method.invoke(gen, Constants.Difficulty.HARD, theme);
            long tRB = System.nanoTime() - t0;
            
            // Time deepCopy + isCleanable
            t0 = System.nanoTime();
            for (int k = 0; k < N; k++) {
                GameBoard copy = board.deepCopy();
                cleanMethod.invoke(gen, copy);
            }
            long tDC = System.nanoTime() - t0;
            
            // Time deepCopy only
            t0 = System.nanoTime();
            for (int k = 0; k < N; k++)
                board.deepCopy();
            long tDeepCopyOnly = System.nanoTime() - t0;
            
            System.out.println(theme + ":");
            System.out.println("  full generate:      " + String.format("%.4f", tFull/1e6/N) + " ms");
            System.out.println("  randomBoard:        " + String.format("%.4f", tRB/1e6/N) + " ms");
            System.out.println("  deepCopy+isCleanable: " + String.format("%.4f", tDC/1e6/N) + " ms");
            System.out.println("  deepCopy only:      " + String.format("%.4f", tDeepCopyOnly/1e6/N) + " ms");
            System.out.println("  implied isCleanable:" + String.format("%.4f", (tDC-tDeepCopyOnly)/1e6/N) + " ms");
        }
    }
}
