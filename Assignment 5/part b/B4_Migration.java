import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * B4.1 - Migrate the existing 5-field pass file into MySQL: validate, collect failures, load the valid
 * rows in batches of 1,000 inside ONE transaction (all-or-nothing), write a failure report.
 * Usage: java B4_Migration [passes.csv]   (if no file given, a demo file with bad lines is generated)
 */
public class B4_Migration {
    public static void main(String[] args) throws Exception {
        Path src = args.length > 0 ? Path.of(args[0]) : makeDemoFile();
        List<String[]> valid = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int lineNo = 0;
        try (BufferedReader r = Files.newBufferedReader(src)) {
            String line;
            while ((line = r.readLine()) != null) {
                lineNo++;
                String[] f = line.split(",", -1);
                String why = PassStore.validate(f);
                if (why == null && !seen.add(f[0])) why = "duplicate pass id " + f[0];
                if (why == null) valid.add(f); else failures.add("line " + lineNo + ": " + why + "  -> [" + line + "]");
            }
        }
        System.out.println("Read " + lineNo + " lines: " + valid.size() + " valid, " + failures.size() + " failed validation.");

        int loaded;
        try (Connection con = DbConfig.connect()) {
            long t = System.nanoTime();
            loaded = B3_PassLoader.loadAll(con, valid);            // batches + single transaction
            System.out.printf("Loaded %,d rows in %.1f ms (single transaction, batches of 1000).%n", loaded, (System.nanoTime() - t) / 1e6);
        }
        Path report = Path.of("migration_failures.txt");
        List<String> rep = new ArrayList<>();
        rep.add("MIGRATION REPORT  source=" + src + "  read=" + lineNo + "  loaded=" + loaded + "  failed=" + failures.size());
        rep.addAll(failures);
        Files.write(report, rep);
        System.out.println("Failure report written to " + report.toAbsolutePath());
        failures.forEach(x -> System.out.println("  " + x));
    }
    static Path makeDemoFile() throws IOException {
        Path f = Files.createTempFile("legacy_passes", ".csv");
        List<String> l = new ArrayList<>();
        for (int i = 0; i < 5000; i++) l.add("P" + (500000 + i) + ",19" + String.format("%07d", i) + ",Student" + i + "," + (1 + i % 12) + "," + (3000 + i % 1500) + ".0");
        l.add(2, "P500002,DAMAGED");              // wrong field count
        l.add(10, "P600001,1923,Bad Amt,5,ABC");  // non-numeric amount
        l.add(20, "");                             // empty line
        l.add(30, "P500001,1923,Dup,5,3000.0");    // duplicate id
        Files.write(f, l); return f;
    }
}
