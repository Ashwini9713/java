import java.sql.*;

/** Connection settings for the LOCAL TEST database. Override with env vars DB_URL, DB_USER, DB_PASS. */
public class DbConfig {
    static final String URL = System.getenv().getOrDefault("DB_URL",
        "jdbc:mysql://localhost:3306/bus_pass_test?rewriteBatchedStatements=true&allowLoadLocalInfile=true&useSSL=false&allowPublicKeyRetrieval=true");
    static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    static final String PASS = System.getenv().getOrDefault("DB_PASS", "root");
    static Connection connect() throws SQLException { return DriverManager.getConnection(URL, USER, PASS); }
}
