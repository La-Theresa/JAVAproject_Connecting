import logic.BoardGenerator;
import logic.PathFinder;
import model.Constants;

public class BenchAllThemes {
    public static void main(String[] args) {
        PathFinder pf = new PathFinder();
        BoardGenerator gen = new BoardGenerator(pf);
        Constants.Theme[] themes = {Constants.Theme.THEME1, Constants.Theme.THEME2, Constants.Theme.THEME3, Constants.Theme.THEME4};
        for (Constants.Theme theme : themes) {
            long start = System.currentTimeMillis();
            int total = 10000;
            int maxAttempt = 0;
            int sumAttempt = 0;
            for (int k = 0; k < total; k++) {
                for (int attempt = 1; attempt <= 500; attempt++) {
                    try {
                        gen.generate(Constants.Difficulty.HARD, theme);
                        sumAttempt += attempt;
                        if (attempt > maxAttempt) maxAttempt = attempt;
                        break;
                    } catch (Exception e) {
                        if (attempt >= 500) {
                            System.out.println(theme + " FAILED after 500 attempts at board " + k);
                            return;
                        }
                    }
                }
            }
            long end = System.currentTimeMillis();
            System.out.println(theme + ": " + (end - start) + "ms total, avg " 
                + ((end - start) / (double)total) + "ms, avgAttempts " 
                + (sumAttempt / (double)total) + ", maxAttempts " + maxAttempt);
        }
        System.out.println("All themes OK");
    }
}
