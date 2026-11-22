import java.sql.*;
import java.util.regex.Pattern;

/**
 * B2 - Unsafe login (string concatenation) vs PreparedStatement, on a LOCAL TEST database.
 * Alternative: quote-escaping / regex validation is weaker because (1) it must anticipate every
 * dangerous character/encoding of every DB (escape rules differ, e.g. backslash handling in MySQL),
 * (2) one missed case = injection, (3) it blocks legitimate input (e.g. O'Brien) when it is strict.
 * Cost: regex validation ~ O(L) per input of length L (microseconds); PreparedStatement binding is also
 * O(L) and the server can reuse the parsed plan - so safety is not paid for with speed.
 */
public class B2_AdversarialLogin {
    static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_]{1,30}");

    static boolean validatedUnsafe(Connection con, String id, String pwd) throws SQLException {
        if (!SAFE_ID.matcher(id).matches()) return false;                     // regex whitelist on id only
        String escaped = pwd.replace("'", "''");                              // naive escaping of pwd
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1 FROM users WHERE id='" + id + "' AND pwd='" + escaped + "'")) {
            return rs.next();
        }
    }

    public static void main(String[] args) throws Exception {
        try (Connection con = DbConfig.connect(); Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users(id VARCHAR(30) PRIMARY KEY, pwd VARCHAR(60) NOT NULL)");
            st.execute("DELETE FROM users");
            st.execute("INSERT INTO users VALUES ('admin','S3cret!'), ('arun','Pa55')");

            String[][] attacks = {
                {"admin", "S3cret!", "valid login"},
                {"admin", "wrong", "wrong password"},
                {"admin' -- ", "anything", "comment-out attack (id)"},
                {"nobody", "' OR '1'='1", "tautology attack (pwd)"},
                {"x' OR 1=1 -- ", "", "tautology attack (id)"},
            };
            System.out.printf("%-26s | %-8s | %-8s | %s%n", "INPUT", "UNSAFE", "PREPARED", "SQL built by unsafe version");
            for (String[] a : attacks) {
                boolean u = A5_Transaction_Injection.unsafeLogin(con, a[0], a[1]);
                boolean p = A5_Transaction_Injection.safeLogin(con, a[0], a[1]);
                System.out.printf("%-26s | %-8s | %-8s | %s%n", a[2], u ? "ACCEPT" : "reject", p ? "ACCEPT" : "reject",
                        A5_Transaction_Injection.unsafeSql(a[0], a[1]));
            }
            System.out.println("\nWhat it reveals: the unsafe design treats user DATA as SQL CODE; any input containing a quote can change the query's meaning.");

            // Per-query cost: regex+escape vs PreparedStatement (rough, local)
            int N = 20_000; long t = System.nanoTime();
            for (int i = 0; i < N; i++) SAFE_ID.matcher("admin").matches();
            System.out.printf("regex validation : %.3f us/query%n", (System.nanoTime() - t) / 1e3 / N);
            try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM users WHERE id=? AND pwd=?")) {
                t = System.nanoTime();
                for (int i = 0; i < 2_000; i++) { ps.setString(1, "admin"); ps.setString(2, "S3cret!"); try (ResultSet rs = ps.executeQuery()) { rs.next(); } }
                System.out.printf("prepared (full round trip incl. DB) : %.1f us/query%n", (System.nanoTime() - t) / 1e3 / 2_000);
            }
            System.out.println("validatedUnsafe(\"admin\", \"S3cret!\") = " + validatedUnsafe(con, "admin", "S3cret!"));
            System.out.println("validatedUnsafe(\"admin' -- \", \"x\")  = " + validatedUnsafe(con, "admin' -- ", "x") + "  (blocked by whitelist, but still fragile)");
        }
    }
}
