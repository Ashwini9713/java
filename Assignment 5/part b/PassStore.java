import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TASK B1 - Bus-pass file store (text format, 5 fields: passId,regNo,name,route,amount).
 * TODO 1 is completed in find(). Extra methods listByRoute() and findValid() were added for A1/B2.
 *
 * Why the damaged line for P1002 does not stop the search: find() reads line by line and, for every
 * line, checks the field count BEFORE using it. "P1002,DAMAGED LINE" splits into 2 fields (not 5),
 * so the loop executes 'continue' and moves to the next line - no exception is thrown, so the
 * search reaches P1003. (And find("P1002") returns null because the only P1002 line is damaged.)
 */
public class PassStore {
    private final Path file;
    PassStore(Path file) { this.file = file; }

    public void add(String passId, String regNo, String name, int route, double amount)
            throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(String.join(",", passId, regNo, name,
                    String.valueOf(route), String.valueOf(amount)));
            w.newLine();
        }
    }

    /** Returns the five fields of the pass with the given ID, or null if not found. */
    public String[] find(String passId) throws IOException {
        // ---- TODO 1 (completed) ----
        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = line.split(",", -1);       // -1 keeps trailing empty fields
                if (f.length != 5) continue;            // damaged line: skip it
                if (f[0].equals(passId)) return f;
            }
        }
        return null;
    }

    /** Stricter lookup: also rejects lines whose route/amount are not numbers (used in B2/B4). */
    public String[] findValid(String passId) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = line.split(",", -1);
                if (validate(f) != null) continue;
                if (f[0].equals(passId)) return f;
            }
        }
        return null;
    }

    public List<String[]> listByRoute(int route) throws IOException {
        List<String[]> out = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = line.split(",", -1);
                if (validate(f) == null && Integer.parseInt(f[3]) == route) out.add(f);
            }
        }
        return out;
    }

    /** Returns null if the 5 fields are valid, otherwise the reason the line is damaged. */
    public static String validate(String[] f) {
        if (f.length != 5) return "expected 5 fields, found " + f.length;
        if (f[0].isBlank() || f[1].isBlank() || f[2].isBlank()) return "empty id/regNo/name";
        try { Integer.parseInt(f[3]); } catch (NumberFormatException e) { return "route is not an integer: '" + f[3] + "'"; }
        try {
            double a = Double.parseDouble(f[4]);
            if (a < 0 || Double.isNaN(a)) return "amount is negative/NaN: '" + f[4] + "'";
        } catch (NumberFormatException e) { return "amount is not a number: '" + f[4] + "'"; }
        return null;
    }

    public static void main(String[] args) throws IOException {
        Path f = Files.createTempFile("passes", ".csv");
        PassStore store = new PassStore(f);
        store.add("P1001", "192311001", "Arun", 12, 4500);
        Files.writeString(f, "P1002,DAMAGED LINE\n", StandardOpenOption.APPEND);
        store.add("P1003", "192311003", "Divya", 7, 3800);
        System.out.println(Arrays.toString(store.find("P1003")));
        System.out.println(Arrays.toString(store.find("P1002")));
        Files.delete(f);
    }
}
