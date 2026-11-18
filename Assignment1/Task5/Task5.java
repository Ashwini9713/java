import java.lang.ref.WeakReference;

/** TASK 5 - static members, object life cycle, passing/returning objects. */
class ParkingTicket {
    private static int counter;                          // shared by ALL tickets: one number sequence
    private static final double[] RATES;
    static {                                             // static block: runs once when the class is loaded
        RATES = new double[]{10, 30};                    // [0] two-wheeler, [1] car
        counter = 0;
        System.out.println("[static block] ParkingTicket class loaded, rates ready");
    }
    private final int ticketNo; private final String regNo;
    ParkingTicket(String regNo) { this.ticketNo = ++counter; this.regNo = regNo; }
    static int issued() { return counter; }              // static method: needs no object
    static boolean isValidReg(String r) { return r != null && r.length() >= 6; }
    static double rate(int index) { return RATES[index]; }
    int getTicketNo() { return ticketNo; } String getRegNo() { return regNo; }
    @Override public String toString() { return "#" + ticketNo + ":" + regNo; }
}

class Facility {
    private static int totalOccupancy = 0;               // across ALL facilities (north + south)
    private int myOccupancy = 0;                         // this facility only -> instance variable
    private final String name;
    Facility(String name) { this.name = name; }
    ParkingTicket park(String reg) { myOccupancy++; totalOccupancy++; return new ParkingTicket(reg); }   // returns an object
    void leave(ParkingTicket t) { myOccupancy--; totalOccupancy--; }
    static int total() { return totalOccupancy; }
    int mine() { return myOccupancy; }
    String getName() { return name; }
}

/* ---------- ALTERNATIVE: counter as an INSTANCE variable of the facility ---------- */
class FacilityWithOwnCounter {
    private int counter = 0;                             // k facilities -> k counters
    String issue() { return "T" + (++counter); }
}

public class Task5 {
    static void rename(ParkingTicket t) { t = new ParkingTicket("REASSIGNED"); }     // reassigns the LOCAL copy of the reference
    static ParkingTicket pick(ParkingTicket a, ParkingTicket b) { return a.getTicketNo() > b.getTicketNo() ? a : b; }

    public static void main(String[] args) throws Exception {
        System.out.println("ParkingTicket class not loaded yet - its static block has not run.");
        Facility north = new Facility("North"), south = new Facility("South");
        ParkingTicket a = north.park("TN01AB1111"), b = south.park("TN02CD2222"), c = north.park("TN03EF3333");
        System.out.println(a + " " + b + " " + c + "   issued = " + ParkingTicket.issued() + "  (one sequence across both gates)");
        System.out.println("occupancy: north=" + north.mine() + ", south=" + south.mine() + ", total=" + Facility.total());

        System.out.println("\n--- Passing / returning objects (references are passed BY VALUE) ---");
        int before = ParkingTicket.issued();
        rename(a);                                       // creates one ticket, but 'a' in main still points to the old one
        System.out.println("a after rename(a) = " + a + "  (unchanged), new ticket was still numbered: issued " + before + " -> " + ParkingTicket.issued());
        System.out.println("pick(a, c) returns reference to " + pick(a, c));

        System.out.println("\n--- Life cycle: leave -> unreachable -> GC-eligible ---");
        ParkingTicket leaving = north.park("TN09ZZ9999");
        WeakReference<ParkingTicket> watch = new WeakReference<>(leaving);
        north.leave(leaving);
        leaving = null;                                  // last strong reference gone
        System.gc(); Thread.sleep(100);
        System.out.println("object collected? " + (watch.get() == null) + "  (GC timing is not guaranteed; usually true here)");

        System.out.println("\n--- Alternative: instance counters with two facilities ---");
        FacilityWithOwnCounter n2 = new FacilityWithOwnCounter(), s2 = new FacilityWithOwnCounter();
        System.out.println("North issues " + n2.issue() + ", South issues " + s2.issue() + "  -> SAME number T1 twice: duplicate ticket numbers!");
        System.out.println("Fix 1: keep counter static (one global sequence).  Fix 2: keep it per-facility but prefix: N-1, S-1.");
        System.out.println("Space: static = 1 int total; instance = k ints for k facilities; tickets use n objects either way.");
    }
}
