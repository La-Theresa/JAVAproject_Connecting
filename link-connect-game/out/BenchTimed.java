import logic.BoardGenerator;
import logic.PathFinder;
import model.Constants;

public class BenchTimed {
    public static void main(String[] args) {
        PathFinder pf = new PathFinder();
        BoardGenerator gen = new BoardGenerator(pf);
        
        for (int i = 0; i < 300; i++)
            for (Constants.Theme t : Constants.Theme.values())
                gen.generate(Constants.Difficulty.HARD, t);
        
        for (Constants.Theme theme : Constants.Theme.values()) {
            int N = 500;
            long t0 = System.nanoTime();
            for (int k = 0; k < N; k++)
                gen.generate(Constants.Difficulty.HARD, theme);
            long t1 = System.nanoTime();
            System.out.println(theme + ": full=" + String.format("%.3f", (t1-t0)/1e6/N)
                + " ms  RB=" + String.format("%.4f", BoardGenerator._tRB/1000.0)
                + "  DC=" + String.format("%.4f", BoardGenerator._tDC/1000.0)
                + "  CL=" + String.format("%.4f", BoardGenerator._tCL/1000.0) + " ms");
        }
    }
}
