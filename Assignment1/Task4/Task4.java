/** TASK 4 - Classes, constructors, this, chaining, copy constructor, memory model. */
class Vehicle {
    private final String regNo; private final String type;
    Vehicle(String regNo, String type) {
        if (regNo == null || regNo.isBlank()) throw new IllegalArgumentException("Registration required");
        this.regNo = regNo.trim().toUpperCase();          // this.field = parameter
        this.type = type;
    }
    Vehicle(String regNo) { this(regNo, "CAR"); }          // overloaded + chained: default type is CAR
    String getRegNo() { return regNo; }
    String getType() { return type; }
}

class ParkingTicket {
    private static int counter = 0;
    private final int ticketNo;
    private final Vehicle vehicle;
    private final int floor, slot, entryMinute;
    private final boolean duplicate;

    /** Default constructor: an "unassigned" placeholder (used rarely, e.g. by frameworks/tests). */
    ParkingTicket() { this(new Vehicle("UNASSIGNED"), 0, 0, 0); }

    /** Parameterised constructor with ALL the validation - the single place objects are really built. */
    ParkingTicket(Vehicle v, int floor, int slot, int entryMinute) {
        if (v == null) throw new IllegalArgumentException("Vehicle required");
        if (floor < 0 || slot < 0 || entryMinute < 0) throw new IllegalArgumentException("Negative value");
        this.ticketNo = ++counter;
        this.vehicle = v; this.floor = floor; this.slot = slot; this.entryMinute = entryMinute;
        this.duplicate = false;
    }
    /** Overloaded: entry time defaults to 0 - delegates with this(...) so no code is repeated. */
    ParkingTicket(Vehicle v, int floor, int slot) { this(v, floor, slot, 0); }

    /** Copy constructor: re-issue a DUPLICATE of a lost ticket - same ticket number, flagged as duplicate. */
    ParkingTicket(ParkingTicket other) {
        this.ticketNo = other.ticketNo;                    // must NOT take a new number
        this.vehicle = other.vehicle;                      // Vehicle is immutable, safe to share
        this.floor = other.floor; this.slot = other.slot; this.entryMinute = other.entryMinute;
        this.duplicate = true;
    }
    int getTicketNo() { return ticketNo; }
    boolean isDuplicate() { return duplicate; }
    @Override public String toString() {
        return "Ticket#" + ticketNo + (duplicate ? "(DUP) " : " ") + vehicle.getRegNo() + " F" + floor + "-S" + slot;
    }
}

/* ---------- ALTERNATIVE A: default constructor + setters (object can exist half-built) ---------- */
class SetterTicket {
    private String regNo; private int floor = -1, slot = -1;
    void setRegNo(String r) { regNo = r; } void setFloor(int f) { floor = f; } void setSlot(int s) { slot = s; }
    boolean isValid() { return regNo != null && floor >= 0 && slot >= 0; }   // caller must remember to check
}
/* ---------- ALTERNATIVE B: static factory methods (named, can return cached/null/subclass) ---------- */
class FactoryTicket {
    private final String regNo; private final int floor, slot;
    private FactoryTicket(String r, int f, int s) { regNo = r; floor = f; slot = s; }
    static FactoryTicket of(String reg, int floor, int slot) {
        if (reg == null || floor < 0 || slot < 0) throw new IllegalArgumentException("invalid");
        return new FactoryTicket(reg, floor, slot);
    }
    static FactoryTicket atGround(String reg, int slot) { return of(reg, 0, slot); }   // descriptive name
}

public class Task4 {
    public static void main(String[] args) {
        Vehicle v = new Vehicle("tn01ab1111");                        // overloaded -> type defaults to CAR
        System.out.println(v.getRegNo() + " " + v.getType());
        ParkingTicket t1 = new ParkingTicket(v, 0, 1, 540);
        ParkingTicket t2 = new ParkingTicket(v, 1, 0);               // chained
        ParkingTicket t0 = new ParkingTicket();                      // default
        System.out.println(t1 + " | " + t2 + " | " + t0);
        ParkingTicket dup = new ParkingTicket(t1);                   // copy constructor
        System.out.println(dup + "  same number as original? " + (dup.getTicketNo() == t1.getTicketNo()));

        System.out.println("\n--- Memory: references, stack and heap ---");
        ParkingTicket alias = t1;                                    // copies the REFERENCE, not the object
        System.out.println("alias == t1 : " + (alias == t1) + "   dup == t1 : " + (dup == t1) + "   (dup is a different heap object)");
        ParkingTicket lost = new ParkingTicket(v, 2, 2); lost = null; // object now unreachable -> GC-eligible
        System.out.println("Local variables (t1, alias, ...) live on the STACK; the ParkingTicket objects live on the HEAP.");

        System.out.println("\n--- Validity guarantee ---");
        try { new ParkingTicket(null, 0, 0); } catch (IllegalArgumentException e) { System.out.println("Constructor rejected: " + e.getMessage()); }
        SetterTicket bad = new SetterTicket();
        System.out.println("Setter version: object exists but isValid() = " + bad.isValid() + "  (caller must remember)");
        System.out.println("Factory: " + (FactoryTicket.atGround("TN09", 3) != null));

        System.out.println("\n--- Cost of creating n tickets (all O(n) time, O(n) space) ---");
        int n = 1_000_000; Vehicle sv = new Vehicle("X");
        long t = System.nanoTime(); for (int i = 0; i < n; i++) new ParkingTicket(sv, 0, i, 0);
        System.out.println("constructor : " + (System.nanoTime() - t) / 1_000_000 + " ms");
        t = System.nanoTime(); for (int i = 0; i < n; i++) { SetterTicket s = new SetterTicket(); s.setRegNo("X"); s.setFloor(0); s.setSlot(i); }
        System.out.println("setters     : " + (System.nanoTime() - t) / 1_000_000 + " ms (3 extra calls per object)");
        t = System.nanoTime(); for (int i = 0; i < n; i++) FactoryTicket.of("X", 0, i);
        System.out.println("factory     : " + (System.nanoTime() - t) / 1_000_000 + " ms");
    }
}
