import logic.BoardGenerator;
import logic.PathFinder;
import model.Constants;

public class BenchAcc {
    public static void main(String[] args) {
        PathFinder pf = new PathFinder();
        BoardGenerator gen = new BoardGenerator(pf);
        
        for (int i = 0; i < 300; i++)
            for (Constants.Theme t : Constants.Theme.values())
                gen.generate(Constants.Difficulty.HARD, t);
        
        for (Constants.Theme theme : Constants.Theme.values()) {
            BoardGenerator.resetAcc();
            int N = 500;
            long t0 = System.nanoTime();
            for (int k = 0; k < N; k++)
                gen.generate(Constants.Difficulty.HARD, theme);
            long t1 = System.nanoTime();
            
            long calls = BoardGenerator._accCalls;
            System.out.println(theme + ": total=" + String.format("%.2f", (t1-t0)/1e6) + " ms, avg=" 
                + String.format("%.4f", (t1-t0)/1e6/N) + " ms/call"
                + "  calls=" + calls);
            System.out.println("  RB=" + String.format("%.4f", BoardGenerator._accRB/1e6/calls)
                + "  DC=" + String.format("%.4f", BoardGenerator._accDC/1e6/calls)
                + "  CL=" + String.format("%.4f", BoardGenerator._accCL/1e6/calls) + " ms");
            System.out.println("  sum=" + String.format("%.4f", (BoardGenerator._accRB+BoardGenerator._accDC+BoardGenerator._accCL)/1e6/calls) + " ms");
        }
    }
}
