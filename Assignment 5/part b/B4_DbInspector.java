import java.sql.*;

/**
 * B4.2 - DatabaseMetaData report.
 * Why useful when moving to another server: confirms WHICH product/version/driver the code is really
 * talking to (SQL dialect, feature support such as batch/transactions/LOAD DATA), proves the target schema
 * exists (tables/columns/PKs) before the migration runs, and lets the app adapt or fail early with a
 * clear message instead of a confusing SQL error in production.
 */
public class B4_DbInspector {
    public static void main(String[] args) throws SQLException {
        try (Connection con = DbConfig.connect()) {
            DatabaseMetaData md = con.getMetaData();
            System.out.println("Database product : " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.out.println("Driver           : " + md.getDriverName() + " " + md.getDriverVersion());
            System.out.println("JDBC URL         : " + md.getURL());
            System.out.println("User             : " + md.getUserName());
            System.out.println("Supports batch   : " + md.supportsBatchUpdates() + "   transactions: " + md.supportsTransactions());
            System.out.println("Tables:");
            try (ResultSet rs = md.getTables(con.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) System.out.println("  - " + rs.getString("TABLE_NAME"));
            }
            System.out.println("Columns of bus_pass:");
            try (ResultSet rs = md.getColumns(con.getCatalog(), null, "bus_pass", "%")) {
                while (rs.next()) System.out.println("  " + rs.getString("COLUMN_NAME") + " " + rs.getString("TYPE_NAME"));
            }
        }
    }
}
