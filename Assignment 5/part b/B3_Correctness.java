import java.sql.*;
import java.util.*;

/** B3.1 - Insert 10 records with Statement, PreparedStatement and Batch; verify with SELECT + ResultSetMetaData. */
public class B3_Correctness {
    public static void main(String[] args) throws Exception {
        List<String[]> data = TestData.generate(10);
        try (Connection con = DbConfig.connect()) {
            con.setAutoCommit(true);
            // (a) Statement
            TestData.clear(con);
            try (Statement st = con.createStatement()) {
                for (String[] p : data) st.executeUpdate("INSERT INTO bus_pass VALUES ('" + p[0] + "','" + p[1] + "','" + p[2] + "'," + p[3] + "," + p[4] + ")");
            }
            show(con, "Statement");
            // (b) PreparedStatement
            TestData.clear(con);
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO bus_pass VALUES (?,?,?,?,?)")) {
                for (String[] p : data) { bind(ps, p); ps.executeUpdate(); }
            }
            show(con, "PreparedStatement");
            // (c) Batch
            TestData.clear(con);
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO bus_pass VALUES (?,?,?,?,?)")) {
                for (String[] p : data) { bind(ps, p); ps.addBatch(); }
                ps.executeBatch();
            }
            show(con, "Batch");
            // (d) PassLoader (transaction + batches)
            TestData.clear(con);
            System.out.println("PassLoader.loadAll returned " + B3_PassLoader.loadAll(con, data));
            show(con, "PassLoader (txn+batch)");
        }
    }
    static void bind(PreparedStatement ps, String[] p) throws SQLException {
        ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]); ps.setInt(4, Integer.parseInt(p[3])); ps.setDouble(5, Double.parseDouble(p[4]));
    }
    static void show(Connection con, String label) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM bus_pass ORDER BY pass_id")) {
            ResultSetMetaData md = rs.getMetaData();
            System.out.println("=== " + label + " ===");
            StringBuilder h = new StringBuilder("columns: ");
            for (int i = 1; i <= md.getColumnCount(); i++) h.append(md.getColumnName(i)).append(':').append(md.getColumnTypeName(i)).append("  ");
            System.out.println(h);
            int rows = 0;
            while (rs.next()) { rows++; if (rows <= 3) System.out.println("  " + rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3) + " | " + rs.getInt(4) + " | " + rs.getDouble(5)); }
            System.out.println("  ... total rows = " + rows + (rows == 10 ? "  OK" : "  MISMATCH"));
        }
    }
}
