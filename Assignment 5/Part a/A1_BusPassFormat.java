import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * TASK A1 - Bus-pass file format design.
 *  (a) Delimited text file  (b) Binary file with DataOutputStream
 *  + text-file operations (add / find / list by route) + damaged-data handling
 *  + Alternative: RandomAccessFile with fixed-length records.
 *
 * Text layout (one record per line, UTF-8, '\n' terminated):
 *   passId , regNo , name , routeNo , boardingStop , validityDate(yyyy-MM-dd) , amount
 * Delimiter = ','   Escape char = '\'  ->  ',' inside a field is written "\,"  and '\' as "\\"
 *
 * Binary layout (DataOutputStream, variable length):
 *   writeUTF(passId) writeUTF(regNo) writeUTF(name) writeInt(routeNo)
 *   writeUTF(stop) writeLong(validity.toEpochDay()) writeDouble(amount)
 *   writeUTF is length-prefixed, so a name containing ',' needs no escaping at all.
 */
public class A1_BusPassFormat {

    // ------------------------------------------------------------------ record
    static final class Pass {
        final String passId, regNo, name, stop;
        final int route; final LocalDate validity; final double amount;
        Pass(String passId, String regNo, String name, int route, String stop,
             LocalDate validity, double amount) {
            this.passId = passId; this.regNo = regNo; this.name = name; this.route = route;
            this.stop = stop; this.validity = validity; this.amount = amount;
        }
        @Override public String toString() {
            return String.join(" | ", passId, regNo, name, String.valueOf(route), stop,
                    validity.toString(), String.valueOf(amount));
        }
    }

    // ------------------------------------------------- (a) text-line encoding
    static String esc(String s) { return s.replace("\\", "\\\\").replace(",", "\\,"); }

    static String toLine(Pass p) {
        return String.join(",", esc(p.passId), esc(p.regNo), esc(p.name),
                String.valueOf(p.route), esc(p.stop), p.validity.toString(), String.valueOf(p.amount));
    }

    /** Splits on unescaped commas. */
    static List<String> splitLine(String line) {
        List<String> f = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) cur.append(line.charAt(++i));
            else if (c == ',') { f.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        f.add(cur.toString());
        return f;
    }

    /** Returns a Pass, or throws IllegalArgumentException for a damaged line. */
    static Pass parseLine(String line) {
        List<String> f = splitLine(line);
        if (f.size() != 7) throw new IllegalArgumentException("expected 7 fields, got " + f.size());
        try {
            return new Pass(f.get(0), f.get(1), f.get(2), Integer.parseInt(f.get(3)), f.get(4),
                    LocalDate.parse(f.get(5)), Double.parseDouble(f.get(6)));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("bad number/date: " + e.getMessage());
        }
    }

    // ------------------------------------------------------ (b) binary layout
    static void writeBinary(DataOutputStream out, Pass p) throws IOException {
        out.writeUTF(p.passId); out.writeUTF(p.regNo); out.writeUTF(p.name);
        out.writeInt(p.route);  out.writeUTF(p.stop);
        out.writeLong(p.validity.toEpochDay()); out.writeDouble(p.amount);
    }
    static Pass readBinary(DataInputStream in) throws IOException {
        return new Pass(in.readUTF(), in.readUTF(), in.readUTF(), in.readInt(), in.readUTF(),
                LocalDate.ofEpochDay(in.readLong()), in.readDouble());
    }

    // ------------------------------------------- A1.2 text-file operations
    /**
     * Stream choice:
     *  - Text data  -> character streams (Reader/Writer), not byte streams, so UTF-8 names work.
     *  - BufferedWriter/BufferedReader wrap them: ~8 KB buffer => far fewer OS calls, and
     *    readLine() gives record-at-a-time access. FileInputStream/OutputStream are used
     *    underneath only where we need getFD().sync() (force data to disk).
     *
     * PSEUDOCODE add(pass):
     *   open file for APPEND (create if missing)
     *   if file is non-empty and last byte != '\n'  -> write '\n'   (repair torn last line)
     *   write toLine(pass) + '\n';  flush;  fsync;  close
     * PSEUDOCODE find(id):
     *   if file missing -> return NOT_FOUND
     *   for each line in file: try parse; on damage -> skip & count; if passId == id -> return
     *   return NOT_FOUND
     * PSEUDOCODE listByRoute(r):
     *   result = []; for each valid line: if route == r -> result.add(pass); return result
     */
    static class TextStore {
        final Path file; int damagedSeen = 0;
        TextStore(Path file) { this.file = file; }

        void add(Pass p) throws IOException {
            boolean needNewline = false;
            if (Files.exists(file) && Files.size(file) > 0) {
                try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
                    raf.seek(raf.length() - 1);
                    needNewline = raf.read() != '\n';           // previous write was torn
                }
            }
            try (FileOutputStream fos = new FileOutputStream(file.toFile(), true);
                 BufferedWriter w = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
                if (needNewline) w.newLine();
                w.write(toLine(p)); w.newLine();
                w.flush();
                fos.getFD().sync();                               // survive power failure
            }
        }

        Pass find(String id) throws IOException {
            if (!Files.exists(file)) return null;                 // missing file = empty store
            try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = r.readLine()) != null) {
                    try {
                        Pass p = parseLine(line);
                        if (p.passId.equals(id)) return p;
                    } catch (IllegalArgumentException damaged) { damagedSeen++; }
                }
            }
            return null;
        }

        List<Pass> listByRoute(int route) throws IOException {
            List<Pass> out = new ArrayList<>();
            if (!Files.exists(file)) return out;
            try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = r.readLine()) != null) {
                    try { Pass p = parseLine(line); if (p.route == route) out.add(p); }
                    catch (IllegalArgumentException damaged) { damagedSeen++; }
                }
            }
            return out;
        }
    }

    // ---------------------------------------- Alternative: RandomAccessFile
    /**
     * Fixed-length records: passId 10 chars, regNo 12, name 30, route int, stop 30,
     * validity long (epoch day), amount double. Strings are written with writeChars (2 B/char).
     * recordSize = (10+12+30+30)*2 + 4 + 8 + 8 = 184 bytes.
     * Record i lives at offset i * 184  => O(1) seek when i is known.
     * Pass IDs like "P1001" give i = numeric(id) - 1001  => O(1) find by ID.
     * (If IDs are arbitrary: sorted file -> O(log n) binary search, otherwise O(n) scan.)
     */
    static class FixedStore implements Closeable {
        static final int ID = 10, REG = 12, NAME = 30, STOP = 30;
        static final int RECORD = (ID + REG + NAME + STOP) * 2 + 4 + 8 + 8;
        final RandomAccessFile raf;
        FixedStore(Path f) throws IOException { raf = new RandomAccessFile(f.toFile(), "rw"); }

        static void writeFixed(RandomAccessFile r, String s, int len) throws IOException {
            StringBuilder b = new StringBuilder(s.length() > len ? s.substring(0, len) : s);
            while (b.length() < len) b.append('\0');
            r.writeChars(b.toString());
        }
        static String readFixed(RandomAccessFile r, int len) throws IOException {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < len; i++) { char c = r.readChar(); if (c != '\0') b.append(c); }
            return b.toString();
        }
        void put(long index, Pass p) throws IOException {
            raf.seek(index * RECORD);
            writeFixed(raf, p.passId, ID); writeFixed(raf, p.regNo, REG); writeFixed(raf, p.name, NAME);
            raf.writeInt(p.route); writeFixed(raf, p.stop, STOP);
            raf.writeLong(p.validity.toEpochDay()); raf.writeDouble(p.amount);
        }
        Pass get(long index) throws IOException {
            if (index < 0 || (index + 1) * RECORD > raf.length()) return null;
            raf.seek(index * RECORD);                              // O(1)
            return new Pass(readFixed(raf, ID), readFixed(raf, REG), readFixed(raf, NAME), raf.readInt(),
                    readFixed(raf, STOP), LocalDate.ofEpochDay(raf.readLong()), raf.readDouble());
        }
        Pass findById(String id) throws IOException { return get(Long.parseLong(id.substring(1)) - 1001); }
        @Override public void close() throws IOException { raf.close(); }
    }

    // ------------------------------------------------------------------ demo
    public static void main(String[] args) throws Exception {
        Path txt = Files.createTempFile("passes", ".txt");
        TextStore s = new TextStore(txt);
        s.add(new Pass("P1001", "192311001", "Arun", 12, "Tambaram", LocalDate.of(2027, 3, 31), 4500));
        s.add(new Pass("P1002", "192311002", "Rao, K. (Jr)", 7, "Guindy", LocalDate.of(2027, 3, 31), 3800)); // comma in name
        Files.writeString(txt, "P1003,DAMAGED\n", StandardOpenOption.APPEND);                                // damaged line
        Files.writeString(txt, "P1004,1923,Half-written", StandardOpenOption.APPEND);                         // torn write, no newline
        s.add(new Pass("P1005", "192311005", "Divya", 12, "Adyar", LocalDate.of(2027, 3, 31), 4500));       // add repairs the torn line
        System.out.println("--- raw text file ---\n" + Files.readString(txt));
        System.out.println("find P1002 : " + s.find("P1002"));
        System.out.println("find P9999 : " + s.find("P9999"));
        System.out.println("route 12   : " + s.listByRoute(12));
        System.out.println("damaged lines skipped so far: " + s.damagedSeen);
        System.out.println("missing file find: " + new TextStore(Path.of("no_such_file.txt")).find("P1001"));

        Path bin = Files.createTempFile("passes", ".bin");
        try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(bin.toFile())))) {
            writeBinary(o, new Pass("P1001", "192311001", "Rao, K. (Jr)", 12, "Tambaram", LocalDate.of(2027, 3, 31), 4500));
        }
        try (DataInputStream i = new DataInputStream(new BufferedInputStream(new FileInputStream(bin.toFile())))) {
            System.out.println("binary round-trip: " + readBinary(i) + "  (" + Files.size(bin) + " bytes)");
        }

        Path fix = Files.createTempFile("passes", ".fix");
        try (FixedStore fs = new FixedStore(fix)) {
            fs.put(0, new Pass("P1001", "192311001", "Arun", 12, "Tambaram", LocalDate.of(2027, 3, 31), 4500));
            fs.put(1, new Pass("P1002", "192311002", "Rao, K. (Jr)", 7, "Guindy", LocalDate.of(2027, 3, 31), 3800));
            System.out.println("fixed find P1002: " + fs.findById("P1002") + "  recordSize=" + FixedStore.RECORD);
        }
        long n = 8000;
        double avgText = 7 + 9 + 8 + 2 + 8 + 10 + 7 + 7;   // approx bytes per text line
        System.out.printf("Space for 8,000 passes: fixed = %,d B, text ~ %,.0f B  (padding waste ~ %.0f%%)%n",
                n * FixedStore.RECORD, n * avgText, 100 * (1 - avgText / FixedStore.RECORD));
        System.out.println("Find by ID: text scan O(n)=~" + n + " line reads; fixed seek O(1)=1 seek (+log n if sorted instead).");
        Files.delete(txt); Files.delete(bin); Files.delete(fix);
    }
}
