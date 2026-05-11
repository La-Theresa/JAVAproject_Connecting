import logic.BoardGenerator;
import logic.PathFinder;
import model.Constants;

public class BenchBoardGen {
    public static void main(String[] args) {
        PathFinder pf = new PathFinder();
        BoardGenerator gen = new BoardGenerator(pf);
        int[] attempts = new int[10000];
        long start = System.currentTimeMillis();
        for (int k = 0; k < 10000; k++) {
            for (int attempt = 1; attempt <= 500; attempt++) {
                try {
                    gen.generate(Constants.Difficulty.HARD, Constants.Theme.THEME2);
                    attempts[k] = attempt;
                    break;
                } catch (Exception e) {
                    if (attempt >= 500) { attempts[k] = -1; break; }
                }
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("Time for 10000 boards: " + (end - start) + " ms");
        System.out.println("Average time: " + (end - start) / 10000.0 + " ms");
        int sum = 0, max = 0;
        for (int a : attempts) { sum += a; if (a > max) max = a; }
        System.out.println("Average attempts: " + (sum / 10000.0));
        System.out.println("Max attempts: " + max);
    }
}
