import java.sql.*;
import java.util.*;

/**
 * TASK A3 - Generic data-access design (DAO pattern).
 *
 * A3.1 Repeated in StudentDB/CourseDB/MarksDB: (1) open connection, (2) build SQL string,
 *      (3) create statement, (4) bind parameters/execute, (5) iterate ResultSet and map each
 *      row to an object, (6) close resources, (7) handle SQLException.
 *      What differs: the table/column names + SQL text, the entity class, and the row->object mapping.
 * A3.2 Dao<T,K> below; StudentDao implements it; the repeated steps live once in JdbcSupport.
 * A3.3 Object-table mapping: see Student + CREATE TABLE in SCHEMA.
 * Alternative (SQL inside service/GUI classes): renaming a column forces edits in EVERY class that
 *      holds that SQL (n places) vs ONE place (StudentDao). Per-operation time/space cost is the
 *      same (same SQL goes to the same DB; the DAO adds one method call, negligible).
 */
public class A3_DataAccess {

    public interface Dao<T, K> {
        void insert(T t) throws SQLException;
        Optional<T> findById(K id) throws SQLException;
        List<T> findAll() throws SQLException;
        int update(T t) throws SQLException;
        int delete(K id) throws SQLException;
    }

    /** Object <-> table mapping:  Student.regNo -> student.reg_no (PK), name -> name, dept -> dept, cgpa -> cgpa */
    public static final String SCHEMA =
        "CREATE TABLE IF NOT EXISTS student (" +
        " reg_no VARCHAR(12) PRIMARY KEY," +
        " name   VARCHAR(60) NOT NULL," +
        " dept   VARCHAR(30)," +
        " cgpa   DOUBLE)";

    public static class Student {
        final String regNo, name, dept; final double cgpa;
        public Student(String regNo, String name, String dept, double cgpa) {
            this.regNo = regNo; this.name = name; this.dept = dept; this.cgpa = cgpa;
        }
        @Override public String toString() { return regNo + " " + name + " " + dept + " " + cgpa; }
    }

    /** The steps that were duplicated in all three classes, written ONCE. */
    interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }
    interface ParamSetter { void set(PreparedStatement ps) throws SQLException; }

    static final class JdbcSupport {
        private final Connection con;
        JdbcSupport(Connection con) { this.con = con; }
        <T> List<T> query(String sql, ParamSetter ps, RowMapper<T> m) throws SQLException {
            try (PreparedStatement st = con.prepareStatement(sql)) {
                ps.set(st);
                try (ResultSet rs = st.executeQuery()) {
                    List<T> out = new ArrayList<>();
                    while (rs.next()) out.add(m.map(rs));
                    return out;
                }
            }
        }
        int update(String sql, ParamSetter ps) throws SQLException {
            try (PreparedStatement st = con.prepareStatement(sql)) { ps.set(st); return st.executeUpdate(); }
        }
    }

    public static class StudentDao implements Dao<Student, String> {
        private final JdbcSupport jdbc;
        public StudentDao(Connection con) { this.jdbc = new JdbcSupport(con); }
        private static final RowMapper<Student> MAP = rs ->
            new Student(rs.getString("reg_no"), rs.getString("name"), rs.getString("dept"), rs.getDouble("cgpa"));

        @Override public void insert(Student s) throws SQLException {
            jdbc.update("INSERT INTO student(reg_no,name,dept,cgpa) VALUES(?,?,?,?)", ps -> {
                ps.setString(1, s.regNo); ps.setString(2, s.name); ps.setString(3, s.dept); ps.setDouble(4, s.cgpa); });
        }
        @Override public Optional<Student> findById(String id) throws SQLException {
            return jdbc.query("SELECT * FROM student WHERE reg_no=?", ps -> ps.setString(1, id), MAP)
                       .stream().findFirst();
        }
        @Override public List<Student> findAll() throws SQLException {
            return jdbc.query("SELECT * FROM student", ps -> {}, MAP);
        }
        @Override public int update(Student s) throws SQLException {
            return jdbc.update("UPDATE student SET name=?, dept=?, cgpa=? WHERE reg_no=?", ps -> {
                ps.setString(1, s.name); ps.setString(2, s.dept); ps.setDouble(3, s.cgpa); ps.setString(4, s.regNo); });
        }
        @Override public int delete(String id) throws SQLException {
            return jdbc.update("DELETE FROM student WHERE reg_no=?", ps -> ps.setString(1, id));
        }
    }

    /** CourseDao / MarksDao would only add their own SQL + RowMapper - no new plumbing. */

    public static void main(String[] args) throws SQLException {
        try (Connection con = DbConfig.connect(); Statement st = con.createStatement()) {
            st.execute(SCHEMA);
            st.execute("DELETE FROM student");
            Dao<Student, String> dao = new StudentDao(con);
            dao.insert(new Student("192465065", "A. Ashwini", "CSE", 8.9));
            dao.insert(new Student("192465066", "B. Student", "IT", 8.1));
            System.out.println("findById : " + dao.findById("192465065").orElse(null));
            dao.update(new Student("192465065", "A. Ashwini", "CSE", 9.1));
            System.out.println("findAll  : " + dao.findAll());
            System.out.println("deleted  : " + dao.delete("192465066"));
            System.out.println("findAll  : " + dao.findAll());
        }
    }
}
