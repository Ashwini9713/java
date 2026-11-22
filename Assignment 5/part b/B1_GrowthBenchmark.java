import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * B1.4 - Time to find a pass as the file grows 1,000 -> 100,000 records.
 * Also the ALTERNATIVE: load once into HashMap<String,String[]> (O(n) once, then O(1) per query).
 * Worst case = ID at the end of the file (or absent) -> the scan reads all n lines  => O(n).
 */
public class B1_GrowthBenchmark {
    public static void main(String[] args) throws IOException {
        int[] ns = {1_000, 5_000, 10_000, 50_000, 100_000};
        int q = 100;                                        // number of searches for the HashMap comparison
        System.out.println("n        | scan: find LAST ms | scan: find MISSING ms | scan q=" + q + " total ms | HashMap load ms | HashMap q=" + q + " total ms | file KB");
        for (int n : ns) {
            Path f = Files.createTempFile("growth", ".csv");
            PassStore s = new PassStore(f);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) sb.append("P").append(1001 + i).append(",19231").append(String.format("%04d", i % 10000))
                    .append(",Student").append(i).append(',').append(1 + i % 40).append(',').append(3000 + i % 2000).append(".0\n");
            Files.writeString(f, sb.toString());
            String last = "P" + (1000 + n);

            s.find(last);                                   // warm-up
            double tLast = time(() -> s.find(last));
            double tMiss = time(() -> s.find("P0000"));
            Random rnd = new Random(1);
            long t = System.nanoTime();
            for (int i = 0; i < q; i++) s.find("P" + (1001 + rnd.nextInt(n)));
            double tQ = (System.nanoTime() - t) / 1e6;

            t = System.nanoTime();
            Map<String, String[]> map = new HashMap<>();
            for (String line : Files.readAllLines(f)) { String[] a = line.split(",", -1); if (a.length == 5) map.put(a[0], a); }
            double tLoad = (System.nanoTime() - t) / 1e6;
            rnd = new Random(1);
            t = System.nanoTime();
            for (int i = 0; i < q; i++) map.get("P" + (1001 + rnd.nextInt(n)));
            double tMap = (System.nanoTime() - t) / 1e6;

            System.out.printf("%,8d | %18.2f | %21.2f | %18.1f | %15.1f | %20.3f | %d%n",
                    n, tLast, tMiss, tQ, tLoad, tMap, Files.size(f) / 1024);
            Files.delete(f);
        }
        System.out.println("Trend: scan time grows ~linearly with n (O(n)); HashMap pays O(n) once then ~O(1) per query. "
                + "Memory: scan O(1) extra (one line at a time); HashMap O(n) (all passes on the heap).");
    }
    interface Op { void run() throws IOException; }
    static double time(Op op) throws IOException {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) { long t = System.nanoTime(); op.run(); best = Math.min(best, (System.nanoTime() - t) / 1e6); }
        return best;
    }
}
