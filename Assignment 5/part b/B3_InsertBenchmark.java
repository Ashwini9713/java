import java.sql.*;
import java.util.*;

/**
 * B3.2 / B3.3 - Statement vs PreparedStatement vs Batch; auto-commit vs one transaction; forced failure.
 * Usage: java -cp ... B3_InsertBenchmark [maxN]    (default 100000; use 10000 first - Statement+autocommit at 100k is SLOW)
 * Alternative approaches (multi-row INSERT, LOAD DATA) are included for the 'Alternative Concept' analysis.
 */
public class B3_InsertBenchmark {
    static double ms(long t0) { return (System.nanoTime() - t0) / 1e6; }

    static double viaStatement(Connection con, List<String[]> d) throws SQLException {
        con.setAutoCommit(true); TestData.clear(con); long t = System.nanoTime();
        try (Statement st = con.createStatement()) {
            for (String[] p : d) st.executeUpdate("INSERT INTO bus_pass VALUES ('" + p[0] + "','" + p[1] + "','" + p[2] + "'," + p[3] + "," + p[4] + ")");
        }
        return ms(t);
    }
    static double viaPrepared(Connection con, List<String[]> d) throws SQLException {
        con.setAutoCommit(true); TestData.clear(con); long t = System.nanoTime();
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO bus_pass VALUES (?,?,?,?,?)")) {
            for (String[] p : d) { B3_Correctness.bind(ps, p); ps.executeUpdate(); }
        }
        return ms(t);
    }
    static double viaBatch(Connection con, List<String[]> d, boolean singleTxn) throws SQLException {
        con.setAutoCommit(!singleTxn); TestData.clear(con); long t = System.nanoTime();
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO bus_pass VALUES (?,?,?,?,?)")) {
            int n = 0;
            for (String[] p : d) { B3_Correctness.bind(ps, p); ps.addBatch(); if (++n % 1000 == 0) ps.executeBatch(); }
            ps.executeBatch();
            if (singleTxn) con.commit();
        }
        double r = ms(t); con.setAutoCommit(true); return r;
    }
    /** Alternative 1: one multi-row INSERT per b rows. Memory: the SQL text grows with b (O(b)). */
    static double viaMultiRow(Connection con, List<String[]> d, int b) throws SQLException {
        con.setAutoCommit(false); TestData.clear(con); long t = System.nanoTime();
        for (int i = 0; i < d.size(); i += b) {
            int end = Math.min(i + b, d.size());
            StringBuilder sql = new StringBuilder("INSERT INTO bus_pass VALUES ");
            for (int k = i; k < end; k++) sql.append(k > i ? "," : "").append("(?,?,?,?,?)");
            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                int c = 1;
                for (int k = i; k < end; k++) { String[] p = d.get(k);
                    ps.setString(c++, p[0]); ps.setString(c++, p[1]); ps.setString(c++, p[2]); ps.setInt(c++, Integer.parseInt(p[3])); ps.setDouble(c++, Double.parseDouble(p[4])); }
                ps.executeUpdate();
            }
        }
        con.commit(); double r = ms(t); con.setAutoCommit(true); return r;
    }
    /** Alternative 2: LOAD DATA LOCAL INFILE (needs local_infile=1 on server and allowLoadLocalInfile=true in URL). */
    static double viaLoadData(Connection con, List<String[]> d) throws Exception {
        java.nio.file.Path f = java.nio.file.Files.createTempFile("load", ".csv");
        StringBuilder sb = new StringBuilder(); for (String[] p : d) sb.append(String.join(",", p)).append('\n');
        java.nio.file.Files.writeString(f, sb.toString());
        con.setAutoCommit(true); TestData.clear(con); long t = System.nanoTime();
        try (Statement st = con.createStatement()) {
            st.execute("LOAD DATA LOCAL INFILE '" + f.toString().replace("\\", "/") + "' INTO TABLE bus_pass FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n'");
        }
        double r = ms(t); java.nio.file.Files.delete(f); return r;
    }

    public static void main(String[] args) throws Exception {
        int max = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;
        try (Connection con = DbConfig.connect()) {
            System.out.println("rows    | Statement ms | Prepared ms | Batch(autocommit) ms | Batch(1 txn) ms | MultiRow(b=1000) ms | LOAD DATA ms");
            for (int n : new int[]{1_000, 10_000, 100_000}) {
                if (n > max) break;
                List<String[]> d = TestData.generate(n);
                double a = viaStatement(con, d), b = viaPrepared(con, d), c = viaBatch(con, d, false), e = viaBatch(con, d, true),
                       m = viaMultiRow(con, d, 1000);
                String ld; try { ld = String.format("%.1f", viaLoadData(con, d)); } catch (Exception ex) { ld = "n/a (" + ex.getClass().getSimpleName() + ")"; }
                System.out.printf("%,7d | %12.1f | %11.1f | %20.1f | %15.1f | %19.1f | %s%n", n, a, b, c, e, m, ld);
            }

            // ---------- B3.3 forced failure half-way ----------
            int n = 10_000; List<String[]> bad = TestData.generate(n, n / 2);
            System.out.println("\nForced failure at row " + n / 2 + " (amount = 'ABC'):");
            // (1) auto-commit ON, batches of 1000
            TestData.clear(con); con.setAutoCommit(true);
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO bus_pass VALUES (?,?,?,?,?)")) {
                int k = 0; for (String[] p : bad) { B3_Correctness.bind(ps, p); ps.addBatch(); if (++k % 1000 == 0) ps.executeBatch(); }
                ps.executeBatch();
            } catch (RuntimeException ex) { System.out.println("  auto-commit run failed: " + ex.getClass().getSimpleName()); }
            System.out.println("  rows left with AUTO-COMMIT   : " + TestData.count(con) + "   <-- partial data (inconsistent)");
            // (2) single transaction via PassLoader
            TestData.clear(con);
            try { B3_PassLoader.loadAll(con, bad); } catch (RuntimeException ex) { System.out.println("  transaction run failed: " + ex.getClass().getSimpleName()); }
            System.out.println("  rows left with TRANSACTION   : " + TestData.count(con) + "   <-- all-or-nothing");
        }
    }
}
