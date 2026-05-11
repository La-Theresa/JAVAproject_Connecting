import logic.BoardGenerator;
import logic.PathFinder;
import model.*;

public class ProfileDC {
    public static void main(String[] args) {
        PathFinder pf = new PathFinder();
        BoardGenerator gen = new BoardGenerator(pf);
        
        // warmup
        for (int i = 0; i < 500; i++) {
            for (Constants.Theme t : Constants.Theme.values())
                gen.generate(Constants.Difficulty.HARD, t);
        }
        
        int N = 3000;
        for (Constants.Theme theme : Constants.Theme.values()) {
            GameBoard board = gen.generate(Constants.Difficulty.HARD, theme);
            
            long t0 = System.nanoTime();
            for (int k = 0; k < N; k++) {
                GameBoard copy = board.deepCopy();
            }
            long t1 = System.nanoTime();
            System.out.println(theme + " deepCopy: " + String.format("%.4f", (t1-t0)/1e6/N) + " ms"
                + "  (entries=" + board.patternEntries().size() + ")");
        }
    }
}
