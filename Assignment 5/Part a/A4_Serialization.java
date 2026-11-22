import java.io.*;
import java.util.*;

/**
 * TASK A4 - What should be serialized?
 *
 * A4.1 transient: Resident.password (security - never write secrets to disk) and
 *      Resident.connection (a live OS/network resource: not Serializable, meaningless after reload,
 *      and would throw NotSerializableException). name & regNo are persisted.
 *      Serializing Hostel writes the WHOLE reachable object graph: Hostel -> ArrayList<Room> ->
 *      each Room -> ArrayList<Resident> -> each Resident. Every class on the path must implement
 *      Serializable; shared references are written once (back-references), cycles are handled.
 * A4.2 Versioning: see partA/A4/ (v1 writes, v2 reads) and run_a4_versioning.sh.
 *      - same serialVersionUID, new field 'phone' -> old file loads, phone gets its default (null/0).
 *      - different (or auto-computed, changed) serialVersionUID -> InvalidClassException.
 * Alternative (CSV/JSON): see main() comparison. CSV: smaller, human-readable, slower per-object
 *      parsing, a new column is handled by the reader (default for old rows). Java serialization:
 *      larger (class metadata + overhead), fast to write, brittle across versions, Java-only.
 */
public class A4_Serialization {

    static class Resident implements Serializable {
        private static final long serialVersionUID = 1L;
        String name; String regNo;
        transient String password;
        transient Object connection;            // stands for java.sql.Connection (not serializable)
        Resident(String n, String r, String p) { name = n; regNo = r; password = p; connection = new Object(); }
    }
    static class Room implements Serializable {
        private static final long serialVersionUID = 1L;
        int no; List<Resident> residents = new ArrayList<>();
        Room(int no) { this.no = no; }
    }
    static class Hostel implements Serializable {
        private static final long serialVersionUID = 1L;
        String name; List<Room> rooms = new ArrayList<>();
        Hostel(String n) { name = n; }
    }

    static Hostel build(int roomsN, int perRoom) {
        Hostel h = new Hostel("Kaveri Hostel");
        for (int r = 0; r < roomsN; r++) {
            Room room = new Room(100 + r);
            for (int k = 0; k < perRoom; k++) room.residents.add(new Resident("Student" + r + "_" + k, "19" + (2300000 + r * 10 + k), "pw" + k));
            h.rooms.add(room);
        }
        return h;
    }

    public static void main(String[] args) throws Exception {
        // --- A4.1: what survives?
        Hostel h = build(2, 2);
        File f = File.createTempFile("hostel", ".ser");
        try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) { o.writeObject(h); }
        try (ObjectInputStream i = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            Hostel back = (Hostel) i.readObject();
            Resident r = back.rooms.get(0).residents.get(0);
            System.out.println("Reloaded: " + back.name + ", rooms=" + back.rooms.size() + ", first resident=" + r.name + "/" + r.regNo
                    + ", password=" + r.password + ", connection=" + r.connection + "   (transient => null)");
        }

        // --- Alternative: CSV vs serialization for n residents
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            Hostel big = build(n / 10, 10);
            File ser = File.createTempFile("hostel", ".ser"), csv = File.createTempFile("hostel", ".csv");

            long t = System.nanoTime();
            try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(ser)))) { o.writeObject(big); }
            double serSave = (System.nanoTime() - t) / 1e6;
            t = System.nanoTime();
            try (ObjectInputStream i = new ObjectInputStream(new BufferedInputStream(new FileInputStream(ser)))) { i.readObject(); }
            double serLoad = (System.nanoTime() - t) / 1e6;

            t = System.nanoTime();
            try (PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter(csv)))) {
                for (Room room : big.rooms) for (Resident r : room.residents) w.println(room.no + "," + r.name + "," + r.regNo);
            }
            double csvSave = (System.nanoTime() - t) / 1e6;
            t = System.nanoTime();
            int rows = 0;
            try (BufferedReader rd = new BufferedReader(new FileReader(csv))) {
                String line; while ((line = rd.readLine()) != null) { String[] p = line.split(","); rows++;
                    String phone = p.length > 3 ? p[3] : "";   // new column? old rows simply lack it -> default
                } }
            double csvLoad = (System.nanoTime() - t) / 1e6;

            System.out.printf("n=%,7d | serialization: %,9d B save %7.1f ms load %7.1f ms | CSV: %,9d B save %7.1f ms load %7.1f ms (%d rows)%n",
                    n, ser.length(), serSave, serLoad, csv.length(), csvSave, csvLoad, rows);
            ser.delete(); csv.delete();
        }
        f.delete();
    }
}
