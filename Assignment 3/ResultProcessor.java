import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/** Q7 - Concurrent, fault-tolerant processing of 10,000 score files with ExecutorService/Callable/Future. */
public class ResultProcessor {

    static class CorruptFileException extends Exception {
        CorruptFileException(String m, Throwable c) { super(m, c); }
        CorruptFileException(String m) { super(m); }
    }

    static class Outcome {                                   // immutable result of one file
        final String file; final boolean ok; final String detail;
        Outcome(String file, boolean ok, String detail) { this.file = file; this.ok = ok; this.detail = detail; }
    }

    static class Summary {
        final AtomicInteger success = new AtomicInteger(), missing = new AtomicInteger(), corrupt = new AtomicInteger(),
                            other = new AtomicInteger();
        final ConcurrentHashMap<String, Double> results  = new ConcurrentHashMap<>();   // studentFile -> average
        final ConcurrentHashMap<String, String> failures = new ConcurrentHashMap<>();   // file -> reason
        int total() { return success.get() + missing.get() + corrupt.get() + other.get(); }
        public String toString() { return "ok=" + success + " missing=" + missing + " corrupt=" + corrupt + " other=" + other; }
    }

    // ---- the work: read + validate + compute ----
    static double compute(Path p) throws IOException, CorruptFileException {
        List<String> lines = Files.readAllLines(p);                        // NoSuchFileException if missing
        if (lines.isEmpty()) throw new CorruptFileException("empty file");
        String[] f = lines.get(0).split(",");
        if (f.length != 4) throw new CorruptFileException("expected 4 fields, got " + f.length);
        double sum = 0;
        try {
            for (int i = 1; i < 4; i++) {
                int m = Integer.parseInt(f[i].trim());
                if (m < 0 || m > 100) throw new CorruptFileException("mark out of range: " + m);
                sum += m;
            }
        } catch (NumberFormatException e) { throw new CorruptFileException("non-numeric mark", e); }
        LockSupport.parkNanos(300_000);                                    // simulate per-file work (~0.3 ms)
        return sum / 3;
    }

    // Callable that NEVER lets a per-file failure escape: each failure becomes an Outcome
    static Callable<Outcome> task(Path p) {
        return () -> {
            String name = p.getFileName().toString();
            try { return new Outcome(name, true, String.valueOf(compute(p))); }
            catch (NoSuchFileException e)      { return new Outcome(name, false, "MISSING"); }
            catch (CorruptFileException e)     { return new Outcome(name, false, "CORRUPT: " + e.getMessage()); }
            catch (IOException e)              { return new Outcome(name, false, "OTHER: " + e); }
            catch (RuntimeException e)         { return new Outcome(name, false, "OTHER: " + e); }
        };
    }

    static void record(Summary s, Outcome o) {
        if (o.ok) { s.success.incrementAndGet(); s.results.put(o.file, Double.parseDouble(o.detail)); return; }
        s.failures.put(o.file, o.detail);
        if (o.detail.equals("MISSING")) s.missing.incrementAndGet();
        else if (o.detail.startsWith("CORRUPT")) s.corrupt.incrementAndGet();
        else s.other.incrementAndGet();
    }

    static Summary runPool(List<Path> files, int threads) throws InterruptedException {
        Summary s = new Summary();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Outcome>> futures = new ArrayList<>();
        for (Path p : files) futures.add(pool.submit(task(p)));
        for (int i = 0; i < futures.size(); i++) {
            try { record(s, futures.get(i).get()); }
            catch (ExecutionException e) {                          // safety net (e.g. an Error inside the task)
                String n = files.get(i).getFileName().toString();
                s.failures.put(n, "OTHER: " + e.getCause()); s.other.incrementAndGet();
            }
        }
        pool.shutdown();
        return s;
    }

    // ---- Alternative: parallel stream (common ForkJoinPool, size ~ cores-1) ----
    static Summary runParallelStream(List<Path> files) {
        Summary s = new Summary();
        files.parallelStream().map(p -> { try { return task(p).call(); } catch (Exception e) { return new Outcome(p.toString(), false, "OTHER: " + e); } })
             .forEach(o -> record(s, o));
        return s;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("scores");

        // ---- hand-checked small set: 7 good, 2 corrupt, 1 missing ----
        List<Path> small = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Path p = dir.resolve("small" + i + ".csv"); small.add(p);
            if (i == 3 || i == 6) Files.writeString(p, "S" + i + ",abc,80,90");      // corrupt
            else if (i == 8) continue;                                               // missing (never created)
            else Files.writeString(p, "S" + i + ",60,70,80");
        }
        Summary hs = runPool(small, 4);
        System.out.println("Hand-checked set: " + hs + "  (expected ok=7 missing=1 corrupt=2) -> "
                + ((hs.success.get() == 7 && hs.missing.get() == 1 && hs.corrupt.get() == 2 && hs.total() == 10) ? "PASS" : "FAIL"));

        // ---- the big run: 10,000 files with injected faults ----
        int N = 10_000, expMissing = 0, expCorrupt = 0;
        List<Path> files = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            Path p = dir.resolve("score" + i + ".csv"); files.add(p);
            if (i % 70 == 69) { expMissing++; continue; }                            // missing
            else if (i % 50 == 49) { expCorrupt++; Files.writeString(p, "garbage"); }
            else Files.writeString(p, "S" + i + ",70,80,90");
        }
        System.out.println("\nExpected: ok=" + (N - expMissing - expCorrupt) + " missing=" + expMissing + " corrupt=" + expCorrupt);

        for (int threads : new int[] {1, 2, 4, 8}) {
            long t0 = System.nanoTime();
            Summary s = runPool(files, threads);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            boolean correct = s.total() == N && s.missing.get() == expMissing && s.corrupt.get() == expCorrupt;
            System.out.printf("pool=%d  time=%5d ms  %s  summary %s%n", threads, ms, s, correct ? "[CORRECT]" : "[MISMATCH]");
        }

        long t0 = System.nanoTime();
        Summary ps = runParallelStream(files);
        System.out.printf("parallelStream time=%5d ms  %s%n", (System.nanoTime() - t0) / 1_000_000, ps);
    }
}
