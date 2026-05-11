import logic.BoardGenerator;
import logic.PathFinder;
import model.Constants;

public class BenchWarm {
    public static void main(String[] args) {
        PathFinder pf = new PathFinder();
        BoardGenerator gen = new BoardGenerator(pf);
        
        // Warmup
        for (int i = 0; i < 300; i++) {
            for (Constants.Theme t : Constants.Theme.values())
                gen.generate(Constants.Difficulty.HARD, t);
        }
        
        for (Constants.Theme theme : Constants.Theme.values()) {
            int total = 2000;
            long start = System.nanoTime();
            for (int k = 0; k < total; k++) {
                gen.generate(Constants.Difficulty.HARD, theme);
            }
            long end = System.nanoTime();
            System.out.println(theme + ": " + String.format("%.4f", (end-start)/1e6/total) + " ms/board");
        }
    }
}
