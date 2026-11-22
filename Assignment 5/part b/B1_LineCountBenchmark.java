import java.io.*;
import java.nio.file.*;

/**
 * B1.3 - Unbuffered vs buffered line count on 1, 10, 50 MB files.
 * Usage: java B1_LineCountBenchmark            (all sizes)
 *        java B1_LineCountBenchmark 1 10       (only these sizes, in MB)
 * Prediction from A2.1: unbuffered = file size read() calls; buffered(8192) = size/8192 calls.
 */
public class B1_LineCountBenchmark {
    public static void main(String[] args) throws IOException {
        int[] sizes = args.length == 0 ? new int[]{1, 10, 50} : java.util.Arrays.stream(args).mapToInt(Integer::parseInt).toArray();
        // JIT warm-up so the first measurement is fair
        Path w = A2_LineCounter.makeLog(1);
        for (int i = 0; i < 3; i++) { A2_LineCounter.countBuffered(w, 8192); }
        Files.delete(w);

        System.out.println("MB | unbuffered ms | calls        | buffered ms (median of 5) | calls   | speed-up | predicted calls (unbuf / buf)");
        for (int mb : sizes) {
            Path f = A2_LineCounter.makeLog(mb);
            long size = Files.size(f);
            long t = System.nanoTime();
            long l1 = A2_LineCounter.countUnbuffered(f); long c1 = A2_LineCounter.readCalls;
            double unbuf = (System.nanoTime() - t) / 1e6;
            double[] runs = new double[5]; long l2 = 0, c2 = 0;
            for (int i = 0; i < 5; i++) {
                t = System.nanoTime();
                l2 = A2_LineCounter.countBuffered(f, 8192); c2 = A2_LineCounter.readCalls;
                runs[i] = (System.nanoTime() - t) / 1e6;
            }
            java.util.Arrays.sort(runs);
            double buf = runs[2];
            if (l1 != l2) throw new IllegalStateException("line counts differ!");
            System.out.printf("%2d | %13.1f | %,12d | %25.1f | %,7d | %7.0fx | %,d / %,d%n",
                    mb, unbuf, c1, buf, c2, unbuf / buf, size + 1, (size + 8191) / 8192 + 1);
            Files.delete(f);
        }
    }
}
