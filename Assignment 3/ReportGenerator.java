import java.io.*;
import java.util.*;
import java.util.logging.*;

/** Q3 - Manual close() vs try-with-resources, handle counting, suppressed exceptions, logging. */
public class ReportGenerator {
    static final Logger LOG = Logger.getLogger("report");
    static int openHandles = 0;                       // simulated count of open file handles

    static class StoreFile implements AutoCloseable {
        final String name; final boolean corrupt, badClose;
        StoreFile(String name, boolean corrupt, boolean badClose) {
            this.name = name; this.corrupt = corrupt; this.badClose = badClose; openHandles++;
        }
        int readSales() throws IOException {
            if (corrupt) throw new IOException("Corrupt data in " + name);
            return 100 * (name.charAt(name.length() - 1) - '0');
        }
        @Override public void close() throws IOException {
            openHandles--;
            if (badClose) throw new IOException("close() failed for " + name);
        }
    }

    // 1) Manual: close() is in the try body -> skipped when readSales() throws  => LEAK
    static int manualLeaky(List<String> names, Set<String> bad) {
        int total = 0;
        for (String n : names) {
            StoreFile f = null;
            try {
                f = new StoreFile(n, bad.contains(n), false);
                total += f.readSales();
                f.close();
            } catch (IOException e) {
                LOG.severe("manual: failure caused by file " + n + " -> " + e.getMessage());
                break;
            }
        }
        return total;
    }

    // 2) Manual but correct: close() in finally (verbose, nested try needed for close())
    static int manualFinally(List<String> names, Set<String> bad) {
        int total = 0;
        for (String n : names) {
            StoreFile f = null;
            try {
                f = new StoreFile(n, bad.contains(n), false);
                total += f.readSales();
            } catch (IOException e) {
                LOG.severe("finally: failure caused by file " + n + " -> " + e.getMessage());
                break;
            } finally {
                if (f != null) { try { f.close(); } catch (IOException ignored) { } }
            }
        }
        return total;
    }

    // 3) try-with-resources
    static int twr(List<String> names, Set<String> bad, boolean badClose) {
        int total = 0;
        for (String n : names) {
            try (StoreFile f = new StoreFile(n, bad.contains(n), badClose && bad.contains(n))) {
                total += f.readSales();
            } catch (IOException e) {
                LOG.severe("twr: failure caused by file " + n + " -> " + e.getMessage()
                        + " | suppressed: " + Arrays.toString(e.getSuppressed()));
                break;
            }
        }
        return total;
    }

    static String report(int total) { return "DAILY SALES TOTAL = " + total; }

    static void writeSummary(String file, int total) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) { out.println(report(total)); }
    }

    static class Res implements AutoCloseable {          // to show closing order
        final String n; Res(String n) { this.n = n; System.out.println("  open  " + n); }
        public void close() { System.out.println("  close " + n); }
    }

    public static void main(String[] args) throws Exception {
        List<String> names = List.of("store1", "store2", "store3", "store4", "store5");

        System.out.println("== Same report, no failure ==");
        Set<String> none = Set.of();
        int a = manualFinally(names, none), b = twr(names, none, false);
        System.out.println(report(a) + " | " + report(b) + " | identical = " + (a == b)
                + " | open handles = " + openHandles);
        writeSummary("summary.txt", b);

        System.out.println("\n== store3 is corrupt ==");
        Set<String> bad = Set.of("store3");
        openHandles = 0; manualLeaky(names, bad);
        System.out.println("Manual (close in try body) -> files left open: " + openHandles);
        openHandles = 0; manualFinally(names, bad);
        System.out.println("Manual (close in finally)  -> files left open: " + openHandles);
        openHandles = 0; twr(names, bad, false);
        System.out.println("try-with-resources         -> files left open: " + openHandles);

        System.out.println("\n== Suppressed exception: read fails AND close fails ==");
        openHandles = 0; twr(names, bad, true);

        System.out.println("\n== Closing order (reverse of opening) ==");
        try (Res r1 = new Res("A"); Res r2 = new Res("B")) { System.out.println("  body"); }
    }
}
