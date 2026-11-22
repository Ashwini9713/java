import java.nio.file.*;
import java.util.*;

/** B1.1 - Test table for add / find / list. Prints PASS/FAIL per test and what each verifies. */
public class B1_PassStoreTest {
    static int failures = 0;
    static void check(String name, String verifies, boolean ok) {
        System.out.printf("%-4s | %-34s | %s%n", ok ? "PASS" : "FAIL", name, verifies);
        if (!ok) failures++;
    }
    public static void main(String[] args) throws Exception {
        Path f = Files.createTempFile("passes", ".csv");
        PassStore s = new PassStore(f);
        System.out.println("RESULT | TEST                               | WHAT IT VERIFIES");
        s.add("P1001", "192311001", "Arun", 12, 4500);
        s.add("P1002", "192311002", "Meena", 7, 3800);
        Files.writeString(f, "P1003,DAMAGED LINE\n", StandardOpenOption.APPEND);
        s.add("P1004", "192311004", "Divya", 12, 4500);

        String[] a = s.find("P1004");
        check("find existing (P1004)", "add wrote a readable line; find returns all 5 fields",
                a != null && a.length == 5 && a[2].equals("Divya") && a[4].equals("4500.0"));
        check("find non-existent (P9999)", "search reaches EOF and returns null, no exception", s.find("P9999") == null);
        check("find damaged (P1003)", "a damaged line never matches and never aborts the search", s.find("P1003") == null);
        check("find AFTER damaged line", "records after the damaged line are still reachable", s.find("P1004") != null);
        check("list route 12", "list returns exactly the 2 valid route-12 passes",
                s.listByRoute(12).size() == 2);
        check("list route 99 (none)", "empty list, not null / exception", s.listByRoute(99).isEmpty());
        check("add appends (does not overwrite)", "earlier record P1001 still present after later adds", s.find("P1001") != null);
        Files.delete(f);
        System.out.println(failures == 0 ? "ALL TESTS PASSED" : failures + " TEST(S) FAILED");
    }
}
