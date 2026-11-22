import java.util.*;

/** Generates n synthetic passes: {passId, regNo, name, route, amount}. */
public class TestData {
    static List<String[]> generate(int n) { return generate(n, -1); }

    /** If badIndex >= 0, that row gets an invalid amount ("ABC") to force a mid-list failure. */
    static List<String[]> generate(int n, int badIndex) {
        List<String[]> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            l.add(new String[]{"P" + (100000 + i), "19" + String.format("%07d", i), "Student" + i,
                    String.valueOf(1 + i % 40), i == badIndex ? "ABC" : (3000 + i % 2000) + ".0"});
        return l;
    }
    static int count(java.sql.Connection c) throws java.sql.SQLException {
        try (java.sql.Statement s = c.createStatement(); java.sql.ResultSet r = s.executeQuery("SELECT COUNT(*) FROM bus_pass")) { r.next(); return r.getInt(1); }
    }
    static void clear(java.sql.Connection c) throws java.sql.SQLException {
        boolean a = c.getAutoCommit(); c.setAutoCommit(true);
        try (java.sql.Statement s = c.createStatement()) { s.execute("TRUNCATE TABLE bus_pass"); }
        c.setAutoCommit(a);
    }
}
