import java.sql.*;

/**
 * TASK A5 - Transactions and SQL injection.
 *
 * A5.1 PSEUDOCODE transfer(from, to, amt):
 *        setAutoCommit(false)
 *        try { UPDATE wallet SET bal = bal - amt WHERE id = from AND bal >= amt   (must affect 1 row)
 *              UPDATE wallet SET bal = bal + amt WHERE id = to                    (must affect 1 row)
 *              commit()
 *        } catch (any) { rollback(); rethrow } finally { restore autoCommit }
 *
 * A5.2 Injection: with id = "admin' -- " (note the space after --) the SQL becomes
 *        SELECT * FROM users WHERE id='admin' -- ' AND pwd=''      -> password test is a comment.
 *      or pwd = "' OR '1'='1" gives  ... AND pwd='' OR '1'='1'    -> always true.
 *      PreparedStatement sends SQL and data separately: the input is only ever a VALUE.
 *
 * Alternative (two auto-commit statements): failure between them => the debit is permanent,
 *      the credit never happens => Rs.500 is LOST. Time: one transaction = 2 updates + 1 commit
 *      (one log flush) - usually cheaper than 2 auto-commits (2 flushes); the risk to correctness
 *      is the real difference.
 */
public class A5_Transaction_Injection {

    static void transfer(Connection con, int from, int to, double amt, boolean failBetween) throws SQLException {
        boolean old = con.getAutoCommit();
        con.setAutoCommit(false);
        try (PreparedStatement debit = con.prepareStatement("UPDATE wallet SET bal = bal - ? WHERE id = ? AND bal >= ?");
             PreparedStatement credit = con.prepareStatement("UPDATE wallet SET bal = bal + ? WHERE id = ?")) {
            debit.setDouble(1, amt); debit.setInt(2, from); debit.setDouble(3, amt);
            if (debit.executeUpdate() != 1) throw new SQLException("debit failed (no such wallet / insufficient balance)");
            if (failBetween) throw new IllegalStateException("simulated crash between the two updates");
            credit.setDouble(1, amt); credit.setInt(2, to);
            if (credit.executeUpdate() != 1) throw new SQLException("credit failed");
            con.commit();
        } catch (SQLException | RuntimeException e) {
            con.rollback();
            throw e;
        } finally { con.setAutoCommit(old); }
    }

    /** The auto-commit alternative - UNSAFE. */
    static void transferAutoCommit(Connection con, int from, int to, double amt, boolean failBetween) throws SQLException {
        con.setAutoCommit(true);
        try (Statement st = con.createStatement()) {
            st.executeUpdate("UPDATE wallet SET bal = bal - " + amt + " WHERE id = " + from);   // committed immediately
            if (failBetween) throw new IllegalStateException("simulated crash between the two updates");
            st.executeUpdate("UPDATE wallet SET bal = bal + " + amt + " WHERE id = " + to);
        }
    }

    static String unsafeSql(String id, String pwd) {
        return "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
    }
    static boolean unsafeLogin(Connection con, String id, String pwd) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(unsafeSql(id, pwd))) { return rs.next(); }
    }
    static boolean safeLogin(Connection con, String id, String pwd) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM users WHERE id=? AND pwd=?")) {
            ps.setString(1, id); ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    static void printBalances(Connection con, String label) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT id, bal FROM wallet ORDER BY id")) {
            StringBuilder sb = new StringBuilder(label + ": ");
            double total = 0;
            while (rs.next()) { sb.append("wallet").append(rs.getInt(1)).append("=").append(rs.getDouble(2)).append("  "); total += rs.getDouble(2); }
            System.out.println(sb + "TOTAL=" + total);
        }
    }

    public static void main(String[] args) throws SQLException {
        // No DB needed for this line: shows the crafted SQL.
        System.out.println("Crafted SQL #1: " + unsafeSql("admin' -- ", "whatever"));
        System.out.println("Crafted SQL #2: " + unsafeSql("nobody", "' OR '1'='1"));

        try (Connection con = DbConfig.connect(); Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS wallet(id INT PRIMARY KEY, bal DOUBLE NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS users(id VARCHAR(30) PRIMARY KEY, pwd VARCHAR(60) NOT NULL) ");
            st.execute("DELETE FROM wallet"); st.execute("DELETE FROM users");
            st.execute("INSERT INTO wallet VALUES (1, 1000), (2, 200)");
            st.execute("INSERT INTO users VALUES ('admin', 'S3cret!')");

            printBalances(con, "start              ");
            try { transfer(con, 1, 2, 500, true); } catch (RuntimeException e) { System.out.println("  -> " + e.getMessage()); }
            printBalances(con, "after txn failure  ");     // unchanged: rollback worked
            transfer(con, 1, 2, 500, false);
            printBalances(con, "after txn success  ");
            try { transferAutoCommit(con, 1, 2, 500, true); } catch (RuntimeException e) { System.out.println("  -> " + e.getMessage()); }
            printBalances(con, "after autocommit failure (money LOST)");

            System.out.println("unsafe, correct pwd      : " + unsafeLogin(con, "admin", "S3cret!"));
            System.out.println("unsafe, wrong pwd        : " + unsafeLogin(con, "admin", "wrong"));
            System.out.println("unsafe, id=admin' --     : " + unsafeLogin(con, "admin' -- ", "wrong") + "   <-- INJECTION");
            System.out.println("unsafe, pwd=' OR '1'='1  : " + unsafeLogin(con, "nobody", "' OR '1'='1") + "   <-- INJECTION");
            System.out.println("safe,   id=admin' --     : " + safeLogin(con, "admin' -- ", "wrong"));
            System.out.println("safe,   pwd=' OR '1'='1  : " + safeLogin(con, "nobody", "' OR '1'='1"));
            System.out.println("safe,   correct pwd      : " + safeLogin(con, "admin", "S3cret!"));
        }
    }
}
