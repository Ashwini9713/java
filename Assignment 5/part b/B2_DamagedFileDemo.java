import java.nio.file.*;
import java.util.*;

/** B2 - Damaged bus-pass file: wrong field count, non-numeric amount, empty line. */
public class B2_DamagedFileDemo {
    public static void main(String[] args) throws Exception {
        Path f = Files.createTempFile("damaged", ".csv");
        Files.writeString(f, String.join("\n",
            "P2001,192311001,Arun,12,4500.0",            // good
            "P2002,192311002,Meena,7",                   // wrong number of fields (4)
            "P2003,192311003,Divya,7,ABC",               // non-numeric amount (5 fields!)
            "",                                          // empty line
            "P2005,192311005,Kiran,x,3000.0",            // non-numeric route
            "P2006,192311006,Rao, K,12,3000.0",          // comma inside the name -> 6 fields
            "P2007,192311007,Latha,12,3000.0") + "\n");
        PassStore s = new PassStore(f);
        System.out.println("line | content                                  | reason (strict validator)");
        int n = 0;
        for (String line : Files.readAllLines(f)) {
            String why = PassStore.validate(line.split(",", -1));
            System.out.printf("%4d | %-40s | %s%n", ++n, line.isEmpty() ? "<empty line>" : line, why == null ? "OK" : why);
        }
        System.out.println();
        for (String id : new String[]{"P2001", "P2002", "P2003", "P2005", "P2006", "P2007"}) {
            System.out.printf("%s  find()=%-45s findValid()=%s%n", id,
                    Arrays.toString(s.find(id)), Arrays.toString(s.findValid(id)));
        }
        System.out.println("list route 12 (strict): " + s.listByRoute(12).size() + " pass(es)");
        System.out.println();
        System.out.println("WHAT EACH INPUT REVEALS:");
        System.out.println(" - wrong field count : find() survives (it skips), but the record is silently LOST.");
        System.out.println(" - amount 'ABC'      : find() ACCEPTS it (only the field COUNT is checked) -> bad data flows on; it would crash later in Double.parseDouble.");
        System.out.println(" - empty line        : harmless here (1 field), but shows the format has no end-of-record check.");
        System.out.println(" - comma in a name   : the delimiter is not escaped -> a VALID pass looks damaged. A1's escape rule or a different delimiter is needed.");
        Files.delete(f);
    }
}
