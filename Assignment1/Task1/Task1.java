import java.util.*;

/**
 * TASK 1 - Problem analysis modelled in code.
 * INPUTS : vehicle registration, vehicle type, entry time, exit time, slot request.
 * OUTPUTS: ticket (number, floor, slot, entry time), fee, "facility full", rejection messages.
 * NOUNS -> CLASSES : Vehicle (identity + type), ParkingTicket (one stay), ParkingFacility (floors + slots + rules).
 * VERBS -> METHODS : enter(), exit(), calculateFee(), findTicket().
 * BUSINESS RULES  : (1) a slot holds at most one vehicle  (2) a ticket number is never reused
 *                   (3) a registration can be parked only once at a time  (4) exit must not be before entry
 * ASSUMPTIONS     : times are minutes since midnight, same day; every started hour is charged;
 *                   rates: TWO_WHEELER Rs10/h, CAR Rs30/h; slots are allocated first-free.
 */
class Vehicle {
    private final String regNo, type;
    Vehicle(String regNo, String type) {
        if (regNo == null || regNo.isBlank()) throw new IllegalArgumentException("Registration required");
        if (!type.equals("TWO_WHEELER") && !type.equals("CAR")) throw new IllegalArgumentException("Unknown type " + type);
        this.regNo = regNo.trim().toUpperCase(); this.type = type;
    }
    String getRegNo() { return regNo; }
    String getType() { return type; }
}
class ParkingTicket {
    private static int next = 1;                       // rule 2: numbers only ever increase
    private final int ticketNo; private final Vehicle vehicle; private final int floor, slot, entryMinute;
    ParkingTicket(Vehicle v, int floor, int slot, int entryMinute) {
        this.ticketNo = next++; this.vehicle = v; this.floor = floor; this.slot = slot; this.entryMinute = entryMinute;
    }
    int getTicketNo() { return ticketNo; } Vehicle getVehicle() { return vehicle; }
    int getFloor() { return floor; } int getSlot() { return slot; } int getEntryMinute() { return entryMinute; }
    @Override public String toString() { return "Ticket#" + ticketNo + " " + vehicle.getRegNo() + " F" + floor + "-S" + slot; }
}
class ParkingFacility {
    private final ParkingTicket[][] slots;             // null = free (rule 1: one cell, one vehicle)
    ParkingFacility(int... slotsPerFloor) {
        slots = new ParkingTicket[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) slots[f] = new ParkingTicket[slotsPerFloor[f]];
    }
    ParkingTicket findTicket(String reg) {
        for (ParkingTicket[] floor : slots) for (ParkingTicket t : floor)
            if (t != null && t.getVehicle().getRegNo().equals(reg.trim().toUpperCase())) return t;
        return null;
    }
    ParkingTicket enter(Vehicle v, int minute) {
        if (findTicket(v.getRegNo()) != null) throw new IllegalStateException("Already parked: " + v.getRegNo());   // rule 3
        for (int f = 0; f < slots.length; f++)
            for (int s = 0; s < slots[f].length; s++)
                if (slots[f][s] == null) return slots[f][s] = new ParkingTicket(v, f, s, minute);
        throw new IllegalStateException("Facility full");
    }
    double exit(String reg, int minute) {
        ParkingTicket t = findTicket(reg);
        if (t == null) throw new IllegalArgumentException("No active ticket for " + reg);
        if (minute < t.getEntryMinute()) throw new IllegalArgumentException("Exit before entry");                   // rule 4
        slots[t.getFloor()][t.getSlot()] = null;
        return calculateFee(t.getVehicle().getType(), minute - t.getEntryMinute());
    }
    static double calculateFee(String type, int minutes) {
        int hours = (minutes + 59) / 60;
        return hours * (type.equals("CAR") ? 30 : 10);
    }
}

/* ---------- ALTERNATIVE: plain Strings in parallel arrays ---------- */
class ParallelStore {
    private String[] regNo = new String[4]; private String[] type = new String[4]; private int[] entry = new int[4];
    private int n;
    void add(String r, String t, int e) {
        if (n == regNo.length) {                       // EVERY array must be grown together
            regNo = Arrays.copyOf(regNo, n * 2); type = Arrays.copyOf(type, n * 2); entry = Arrays.copyOf(entry, n * 2);
        }
        regNo[n] = r; type[n] = t; entry[n] = e; n++;
    }
    int find(String r) { for (int i = 0; i < n; i++) if (regNo[i].equals(r)) return i; return -1; }   // O(n)
    // New attribute (e.g. colour): add a 4th array, grow it in add(), shift it in remove(), pass it everywhere.
}

public class Task1 {
    static void attempt(String label, Runnable r) {
        try { r.run(); System.out.println(label + " -> accepted"); }
        catch (RuntimeException e) { System.out.println(label + " -> REJECTED: " + e.getMessage()); }
    }
    public static void main(String[] args) {
        ParkingFacility pf = new ParkingFacility(1, 1);
        System.out.println(pf.enter(new Vehicle("TN01A1", "CAR"), 540));
        attempt("duplicate registration (rule 3)", () -> pf.enter(new Vehicle("tn01a1", "CAR"), 545));
        System.out.println(pf.enter(new Vehicle("TN02B2", "TWO_WHEELER"), 550));
        attempt("third vehicle, no free slot (rule 1)", () -> pf.enter(new Vehicle("TN03C3", "CAR"), 555));
        attempt("exit before entry (rule 4)", () -> pf.exit("TN01A1", 500));
        System.out.println("Fee TN01A1 (540 -> 670 = 130 min): " + pf.exit("TN01A1", 670));
        System.out.println(pf.enter(new Vehicle("TN04D4", "CAR"), 700) + "   <- slot reused, ticket number NOT reused (rule 2)");

        System.out.println("\n--- Search: objects vs parallel arrays vs HashMap (n = 50,000) ---");
        int n = 50_000; ParallelStore ps = new ParallelStore();
        List<Vehicle> list = new ArrayList<>(); Map<String, Vehicle> map = new HashMap<>();
        for (int i = 0; i < n; i++) { Vehicle v = new Vehicle("R" + i, "CAR"); list.add(v); map.put(v.getRegNo(), v); ps.add("R" + i, "CAR", 0); }
        long t0 = System.nanoTime(); ps.find("R" + (n - 1)); long a = System.nanoTime() - t0;
        t0 = System.nanoTime(); for (Vehicle v : list) if (v.getRegNo().equals("R" + (n - 1))) break; long b = System.nanoTime() - t0;
        t0 = System.nanoTime(); map.get("R" + (n - 1)); long c = System.nanoTime() - t0;
        System.out.println("parallel arrays O(n): " + a + " ns | object list O(n): " + b + " ns | HashMap<String,Vehicle> O(1): " + c + " ns");
        System.out.println("Space/vehicle: parallel = 2 refs + 1 int (no object header); objects = header + 2 refs (+ list slot).");
    }
}
