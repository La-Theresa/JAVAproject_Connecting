import logic.*;
import model.*;
import data.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ProfileRB {
    public static void main(String[] args) throws Exception {
        // Use reflection to access private randomBoard
        var method = BoardGenerator.class.getDeclaredMethod("randomBoard", 
            Constants.Difficulty.class, Constants.Theme.class);
        method.setAccessible(true);
        
        PathFinder pf = new PathFinder();
        BoardGenerator gen = new BoardGenerator(pf);
        
        // warmup
        for (int i = 0; i < 500; i++) {
            method.invoke(gen, Constants.Difficulty.HARD, Constants.Theme.THEME1);
            method.invoke(gen, Constants.Difficulty.HARD, Constants.Theme.THEME2);
        }
        
        int N = 1000;
        for (Constants.Theme theme : new Constants.Theme[]{
            Constants.Theme.THEME1, Constants.Theme.THEME2, Constants.Theme.THEME3, Constants.Theme.THEME4
        }) {
            long t0 = System.nanoTime();
            for (int k = 0; k < N; k++) {
                method.invoke(gen, Constants.Difficulty.HARD, theme);
            }
            long t1 = System.nanoTime();
            System.out.println(theme + " randomBoard: " + String.format("%.4f", (t1-t0)/1e6/N) + " ms");
        }
    }
}
