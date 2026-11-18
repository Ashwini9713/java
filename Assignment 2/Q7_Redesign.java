import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Q7 - Diagnose, redesign, validate.  Chosen solution: Q1 e-ticket builder (String '+' in a loop).
 * Original  : s = s + field + ";"             -> O(n^2) chars copied, ~2n temp Strings per ticket
 * Redesign A: one StringBuilder, default cap  -> O(n) but several internal array regrowths
 * Redesign B: SIZE-AWARE builder              -> pre-compute exact length, allocate ONCE, O(n), no regrowth
 * Redesign C: B + per-thread REUSED builder   -> removes per-ticket builder/array allocation as well
 */
public class Q7_Redesign {
    static String original(String[] f) { String s = ""; for (String x : f) s = s + x + ";"; return s; }
    static String designA(String[] f) { StringBuilder sb = new StringBuilder(); for (String x : f) sb.append(x).append(';'); return sb.toString(); }
    static String designB(String[] f) {
        int len = 0; for (String x : f) len += x.length() + 1;           // preprocessing pass: O(n) reads, no copies
        StringBuilder sb = new StringBuilder(len); for (String x : f) sb.append(x).append(';'); return sb.toString();
    }
    // Design C: ThreadLocal => no sharing between threads, so no synchronisation (StringBuilder, not StringBuffer, is safe here)
    static final ThreadLocal<StringBuilder> TL = ThreadLocal.withInitial(() -> new StringBuilder(4096));
    static String designC(String[] f) {
        StringBuilder sb = TL.get(); sb.setLength(0);                     // keeps backing array, drops content
        for (String x : f) sb.append(x).append(';'); return sb.toString(); // only the final String is allocated
    }
    interface B { String build(String[] f); }
    static final com.sun.management.ThreadMXBean MX = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    static volatile int sink;

    public static void main(String[] args) {
        String[] names = {"Original (+)", "A: StringBuilder", "B: presized SB", "C: presized+reused"};
        B[] bs = {Q7_Redesign::original, Q7_Redesign::designA, Q7_Redesign::designB, Q7_Redesign::designC};
        int[][] loads = {{50, 10_000}, {2_000, 100}, {20_000, 10}};  // {fields, tickets}; identical for every design
        for (int[] ld : loads) {
            int n = ld[0], tk = ld[1];
            String[] f = new String[n]; for (int i = 0; i < n; i++) f[i] = "F" + i + "=Value" + i;
            String ref = original(f);
            System.out.printf("%n=== %d fields x %d tickets ===%n%-20s %10s %14s %10s %s%n", n, tk, "design", "time ms", "allocated MB", "speed-up", "same output?");
            double base = 0;
            for (int d = 0; d < bs.length; d++) {
                for (int w = 0; w < 3; w++) for (int t = 0; t < Math.max(1, tk / 5); t++) sink += bs[d].build(f).length(); // warm-up
                double best = Double.MAX_VALUE; long alloc = 0;
                for (int rep = 0; rep < 5; rep++) {
                    long a0 = MX.getThreadAllocatedBytes(Thread.currentThread().getId());
                    long t0 = System.nanoTime();
                    for (int t = 0; t < tk; t++) sink += bs[d].build(f).length();
                    double ms = (System.nanoTime() - t0) / 1e6;
                    alloc = MX.getThreadAllocatedBytes(Thread.currentThread().getId()) - a0;
                    best = Math.min(best, ms);
                }
                if (d == 0) base = best;
                System.out.printf("%-20s %10.2f %14.2f %9.1fx %b%n", names[d], best, alloc / 1048576.0, base / best, bs[d].build(f).equals(ref));
            }
        }
    }
}
