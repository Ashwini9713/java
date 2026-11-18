import java.util.*;

/** Q1: String (+) vs StringBuilder vs StringBuffer, plus join/StringJoiner/format and thread-safety demo. */
public class Q1_StringBench {
    static String[] fields(int n) {
        String[] f = new String[n];
        for (int i = 0; i < n; i++) f[i] = "F" + i + "=Value" + i; // ~10-char fields
        return f;
    }
    static String viaPlus(String[] f) { String s = ""; for (String x : f) s = s + x + ";"; return s; }
    static String viaSB(String[] f) { StringBuilder sb = new StringBuilder(); for (String x : f) sb.append(x).append(';'); return sb.toString(); }
    static String viaSBuf(String[] f) { StringBuffer sb = new StringBuffer(); for (String x : f) sb.append(x).append(';'); return sb.toString(); }
    static String viaJoin(String[] f) { return String.join(";", f); }
    static String viaJoiner(String[] f) { StringJoiner j = new StringJoiner(";"); for (String x : f) j.add(x); return j.toString(); }
    static String viaFormat(String[] f) { // deliberately format-per-field (typical misuse)
        StringBuilder sb = new StringBuilder(); for (String x : f) sb.append(String.format("%s;", x)); return sb.toString(); }

    interface Builder { String build(String[] f); }

    static double timeMs(Builder b, String[] f, int tickets, int reps) {
        long total = 0; int sink = 0;
        for (int r = 0; r < reps; r++) {
            long t0 = System.nanoTime();
            for (int t = 0; t < tickets; t++) sink += b.build(f).length();
            total += System.nanoTime() - t0;
        }
        if (sink == 42) System.out.print(""); // defeat dead-code elimination
        return total / (double) reps / 1e6;
    }
    static long usedMem() { Runtime rt = Runtime.getRuntime(); System.gc(); System.gc(); return rt.totalMemory() - rt.freeMemory(); }

    static long allocBytes(Builder b, String[] f, int tickets) {
        com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        long id = Thread.currentThread().getId();
        long a0 = mx.getThreadAllocatedBytes(id);
        int sink = 0;
        for (int t = 0; t < tickets; t++) sink += b.build(f).length();
        if (sink == 42) System.out.print("");
        return mx.getThreadAllocatedBytes(id) - a0;
    }

    public static void main(String[] args) throws Exception {
        int[][] workloads = { {10, 1}, {50, 10000}, {2000, 100} }; // {fields, tickets}
        String[] names = {"W1 short ticket (10 fields x 1)", "W2 bulk (50 fields x 10,000)", "W3 long (2,000 fields x 100)"};
        String[] mn = {"String +", "StringBuilder", "StringBuffer", "String.join", "StringJoiner", "String.format"};
        Builder[] bs = {Q1_StringBench::viaPlus, Q1_StringBench::viaSB, Q1_StringBench::viaSBuf, Q1_StringBench::viaJoin, Q1_StringBench::viaJoiner, Q1_StringBench::viaFormat};

        for (int w = 0; w < workloads.length; w++) {
            int n = workloads[w][0], tk = workloads[w][1];
            String[] f = fields(n);
            System.out.println("\n=== " + names[w] + " ===");
            System.out.printf("%-14s %12s %16s %16s %14s%n", "Method", "avg time ms", "alloc MB(total)", "est. temp Strings", "heap delta KB");
            for (int m = 0; m < bs.length; m++) {
                for (int warm = 0; warm < 3; warm++) timeMs(bs[m], f, Math.max(1, tk / 10), 1); // warm-up
                int reps = (tk == 1) ? 2000 : 5;
                int tkRun = (tk == 1) ? 1000 : tk;
                double ms = timeMs(bs[m], f, tkRun, reps) / (tk == 1 ? 1000.0 : 1.0); // per-ticket for W1
                long before = usedMem();
                String keep = bs[m].build(f); // result retained
                long after = usedMem();
                long alloc = allocBytes(bs[m], f, Math.min(tk, 200));
                String est = m == 0 ? String.valueOf(2L * n) : (m <= 2 ? "1 (final toString)" : (m == 5 ? "~" + n + " (format temps)" : "1"));
                System.out.printf("%-14s %12.4f %16.2f %16s %14d%n", mn[m], ms, alloc / 1048576.0 / Math.min(tk, 200) * tk, est, (after - before) / 1024);
                if (keep.length() == 0) System.out.println();
            }
        }

        // Thread-safety demonstration for the shared booking log
        System.out.println("\n=== Shared log with 8 threads x 5,000 appends (expected 40,000 lines) ===");
        StringBuilder unsafe = new StringBuilder(); StringBuffer safe = new StringBuffer();
        Thread[] th = new Thread[8];
        for (int i = 0; i < th.length; i++) { final int id = i; th[i] = new Thread(() -> {
            for (int k = 0; k < 5000; k++) { try { unsafe.append("T").append(id).append('\n'); } catch (Exception e) { } safe.append("T").append(id).append('\n'); } }); th[i].start(); }
        for (Thread t : th) t.join();
        System.out.println("StringBuilder lines: " + unsafe.toString().split("\n").length + "  (length " + unsafe.length() + ")");
        System.out.println("StringBuffer  lines: " + safe.toString().split("\n").length + "  (length " + safe.length() + ")");
    }
}
