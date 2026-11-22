import java.io.*;
import java.util.*;

/**
 * B4 Alternative - each of 12 campuses keeps its OWN serialized file.
 * Search all passes  : must open and deserialize 12 files => O(n) total, plus 12 file opens
 *                      (and every file is fully loaded into memory: O(n) heap).
 * Space              : same data, but with Java-serialization overhead (~1.2-1.5x CSV, see A4) and duplication
 *                      if a student appears at several campuses.
 * Concurrency        : a serialized file is rewritten as a WHOLE, so two counters => lost updates. Needs an
 *                      external FileLock / single-writer rule per campus, and no cross-campus transactions.
 * Shared database    : row-level locking + transactions handle concurrent counters; indexed lookup is O(log n).
 */
public class B4_CampusSerializedDemo {
    static class Pass implements Serializable { private static final long serialVersionUID = 1L; String id, name; int route; double amt;
        Pass(String i, String n, int r, double a) { id = i; name = n; route = r; amt = a; } }

    public static void main(String[] args) throws Exception {
        int total = args.length > 0 ? Integer.parseInt(args[0]) : 120_000, campuses = 12, per = total / campuses;
        File[] files = new File[campuses];
        for (int c = 0; c < campuses; c++) {
            files[c] = File.createTempFile("campus" + c + "_", ".ser");
            List<Pass> l = new ArrayList<>();
            for (int i = 0; i < per; i++) l.add(new Pass("P" + (c * per + i), "S" + i, 1 + i % 40, 3000 + i % 2000));
            try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(files[c])))) { o.writeObject(l); }
        }
        String target = "P" + (total - 1);          // lives in the LAST campus file -> worst case
        long t = System.nanoTime(); Pass found = null; long bytes = 0;
        for (File f : files) {
            bytes += f.length();
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) {
                @SuppressWarnings("unchecked") List<Pass> l = (List<Pass>) in.readObject();
                for (Pass p : l) if (p.id.equals(target)) found = p;
            }
        }
        System.out.printf("12 campus files, %,d passes, %,d bytes total: search-all took %.1f ms, found=%s%n",
                total, bytes, (System.nanoTime() - t) / 1e6, found != null);
        for (File f : files) f.delete();
    }
}
