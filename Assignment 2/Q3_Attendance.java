import java.util.*;

/** Q3: equals/hashCode contract experiments. Run once with BROKEN, once with FIXED. */
public class Q3_Attendance {
    public static void main(String[] args) {
        // Test 1: all scans distinct students (works even when broken)
        Set<BrokenStudent> t1 = new HashSet<>();
        t1.add(new BrokenStudent("192311001", "Arun")); t1.add(new BrokenStudent("192311002", "Divya")); t1.add(new BrokenStudent("192311003", "Kiran"));
        System.out.println("Test1 broken, 3 distinct scans  -> size " + t1.size() + "   (predicted 3)");

        // Test 2: repeated scans (the assignment's program)
        Set<BrokenStudent> t2 = new HashSet<>();
        t2.add(new BrokenStudent("192311001", "Arun")); t2.add(new BrokenStudent("192311002", "Divya")); t2.add(new BrokenStudent("192311001", "Arun"));
        System.out.println("Test2 BROKEN, Arun scanned twice -> size " + t2.size() + "   (predicted 2 by intent; expected 3 in practice)");
        Set<Student> t2f = new HashSet<>();
        t2f.add(new Student("192311001", "Arun")); t2f.add(new Student("192311002", "Divya")); t2f.add(new Student("192311001", "Arun"));
        System.out.println("Test2 FIXED,  Arun scanned twice -> size " + t2f.size() + "   (expected 2)");

        // Test 3: mutate regNo after insertion
        Set<MutableStudent> t3 = new HashSet<>();
        MutableStudent m = new MutableStudent("192311001", "Arun");
        t3.add(m);
        System.out.println("Test3 before mutation: contains = " + t3.contains(m));
        m.regNo = "192311999";
        System.out.println("Test3 after mutation : contains = " + t3.contains(m) + ", size = " + t3.size() + " (object is 'lost' in wrong bucket)");
        t3.add(new MutableStudent("192311001", "Arun2"));
        System.out.println("Test3 re-add old regNo: size = " + t3.size());

        // Contract verification of the fixed class
        Student a = new Student("1", "A"), b = new Student("1", "B");
        System.out.println("Verify: equals=" + a.equals(b) + ", hash equal=" + (a.hashCode() == b.hashCode()) + ", stable hash=" + (a.hashCode() == a.hashCode()));

        // Alternatives
        Map<String, Student> map = new HashMap<>(); TreeSet<Student> ts = new TreeSet<>(Comparator.comparing(s -> s.toString().split(" ")[0]));
        String[] scans = {"1", "2", "1", "3", "2"};
        for (String s : scans) { map.putIfAbsent(s, new Student(s, "x")); ts.add(new Student(s, "x")); }
        System.out.println("HashMap<regNo,Student> unique = " + map.size() + ", TreeSet(comparator on regNo) unique = " + ts.size());

        // Inconsistent comparator test: compareTo by name while equals by regNo
        TreeSet<Student> bad = new TreeSet<>(Comparator.comparing(s -> s.toString().split(" ")[1]));
        bad.add(new Student("1", "Same")); bad.add(new Student("2", "Same"));
        System.out.println("TreeSet with comparator on name (inconsistent with equals): size = " + bad.size() + " (two different regNo collapsed)");
    }
}

class BrokenStudent {
    final String regNo, name;
    BrokenStudent(String r, String n) { regNo = r; name = n; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof BrokenStudent)) return false; return regNo.equals(((BrokenStudent) o).regNo); }
    // hashCode NOT overridden -> identity hash
    @Override public String toString() { return regNo + " " + name; }
}
class Student {
    private final String regNo, name;
    Student(String r, String n) { regNo = r; name = n; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Student)) return false; return regNo.equals(((Student) o).regNo); }
    @Override public int hashCode() { return regNo.hashCode(); }   // TODO 1 : same field as equals()
    @Override public String toString() { return regNo + " " + name; }
}
class MutableStudent {       // used for test 3: field used in hashCode is mutable
    String regNo; final String name;
    MutableStudent(String r, String n) { regNo = r; name = n; }
    @Override public boolean equals(Object o) { return o instanceof MutableStudent && regNo.equals(((MutableStudent) o).regNo); }
    @Override public int hashCode() { return regNo.hashCode(); }
}
