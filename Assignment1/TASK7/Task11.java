import java.time.*;
import java.util.*;

interface Chargeable { int RATE_PER_KWH = 12; double chargingFee(int kWh); }

abstract class Vehicle {
    private final String regNo;
    protected Vehicle(String regNo) {
        if (regNo == null || regNo.isBlank()) throw new IllegalArgumentException("Registration number required");
        this.regNo = regNo.trim().toUpperCase();
    }
    public final String getRegNo() { return regNo; }
    protected abstract double ratePerHour();
    public double calculateFee(int minutes) {                       // overload 1
        if (minutes < 0) throw new IllegalArgumentException("Negative duration: " + minutes);
        return ((minutes + 59) / 60) * ratePerHour();               // every started hour
    }
    public double calculateFee(LocalTime in, LocalTime out) {       // overload 2
        if (out.isBefore(in)) throw new IllegalArgumentException("Exit before entry");
        return calculateFee((int) Duration.between(in, out).toMinutes());
    }
    @Override public String toString() { return getClass().getSimpleName() + "[" + regNo + "]"; }
    @Override public boolean equals(Object o) { return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo); }
    @Override public int hashCode() { return regNo.hashCode(); }
}
class TwoWheeler extends Vehicle { TwoWheeler(String r) { super(r); } @Override protected double ratePerHour() { return 10; } }
class Car extends Vehicle { Car(String r) { super(r); } @Override protected double ratePerHour() { return 30; } }
class ElectricCar extends Car implements Chargeable {
    ElectricCar(String r) { super(r); }
    @Override public double chargingFee(int kWh) {
        if (kWh < 0) throw new IllegalArgumentException("kWh negative");
        return kWh * RATE_PER_KWH;
    }
}
final class ParkingTicket {
    final Vehicle vehicle; final int floor, slot; final LocalTime entry;
    ParkingTicket(Vehicle v, int floor, int slot, LocalTime entry) { this.vehicle = v; this.floor = floor; this.slot = slot; this.entry = entry; }
}

class Check {
    static int pass = 0, fail = 0;
    static void eq(String name, Object exp, Object act) {
        boolean ok = String.valueOf(exp).equals(String.valueOf(act));
        if (ok) pass++; else fail++;
        System.out.printf("%-4s %-50s expected=%-10s actual=%s%n", ok ? "PASS" : "FAIL", name, exp, act);
    }
    static void throwsEx(String name, Runnable r) {
        String res; try { r.run(); res = "no exception"; } catch (RuntimeException e) { res = e.getClass().getSimpleName(); }
        boolean ok = !res.equals("no exception");
        if (ok) pass++; else fail++;
        System.out.printf("%-4s %-50s expected=exception actual=%s%n", ok ? "PASS" : "FAIL", name, res);
    }
    static void summary() { System.out.println("\nRESULT: " + pass + " passed, " + fail + " failed"); }
}

class VehicleFactory {
    static Vehicle create(String type, String reg) {
        switch (type.trim().toUpperCase()) {
            case "TWOWHEELER":  return new TwoWheeler(reg);
            case "CAR":         return new Car(reg);
            case "ELECTRICCAR": return new ElectricCar(reg);
            default: throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }
}

/** Original design: tickets in a flat array, slots 1..capacity. */
class ParkingLot {
    private final ParkingTicket[] slots;
    private double collection;
    ParkingLot(int capacity) { slots = new ParkingTicket[capacity]; }
    private void checkSlot(int s) { if (s < 1 || s > slots.length) throw new IllegalArgumentException("Invalid slot " + s); }
    int enter(Vehicle v, LocalTime at) {
        for (int i = 0; i < slots.length; i++) if (slots[i] == null) return enterAt(v, i + 1, at);
        throw new IllegalStateException("Lot full");
    }
    int enterAt(Vehicle v, int slot, LocalTime at) {
        checkSlot(slot);
        for (ParkingTicket t : slots) if (t != null && t.vehicle.equals(v)) throw new IllegalStateException("Duplicate " + v.getRegNo());
        if (slots[slot - 1] != null) throw new IllegalStateException("Slot occupied");
        slots[slot - 1] = new ParkingTicket(v, 1, slot, at);
        return slot;
    }
    double exit(String reg, LocalTime at, int kWh) {                    // O(n) search
        for (int i = 0; i < slots.length; i++) {
            ParkingTicket t = slots[i];
            if (t != null && t.vehicle.getRegNo().equalsIgnoreCase(reg)) {
                double fee = t.vehicle.calculateFee(t.entry, at);        // dynamic dispatch
                if (t.vehicle instanceof Chargeable c) fee += c.chargingFee(kWh);
                slots[i] = null; collection += fee; return fee;
            }
        }
        throw new IllegalArgumentException("No active ticket: " + reg);
    }
    int free() { int n = 0; for (ParkingTicket t : slots) if (t == null) n++; return n; }
    double collection() { return collection; }
}

public class Task11 {
    static LocalTime t(String s) { return LocalTime.parse(s); }
    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot(3);
        System.out.println("=== Normal + boundary + invalid tests ===");
        Check.eq("TwoWheeler 90 min", 20.0, new TwoWheeler("A").calculateFee(90));
        Check.eq("Car exactly 60 min (slab boundary)", 30.0, new Car("A").calculateFee(60));
        Check.eq("Car 61 min", 60.0, new Car("A").calculateFee(61));
        Check.eq("Car 0 minutes", 0.0, new Car("A").calculateFee(0));
        Check.eq("equals Car/ElectricCar same reg", true, new Car("X").equals(new ElectricCar("X")));
        lot.enter(new TwoWheeler("TN01A1"), t("09:00"));
        lot.enter(new Car("TN02B2"), t("09:15"));
        lot.enter(new ElectricCar("TN03C3"), t("10:00"));
        Check.eq("last free slot used, free slots", 0, lot.free());
        Check.throwsEx("lot full", () -> lot.enter(new Car("N9"), t("10:05")));
        Check.throwsEx("duplicate registration", () -> lot.enterAt(new Car("TN02B2"), 1, t("10:05")));
        Check.throwsEx("slot 0", () -> lot.enterAt(new Car("N1"), 0, t("10:05")));
        Check.throwsEx("slot 4", () -> lot.enterAt(new Car("N2"), 4, t("10:05")));
        Check.throwsEx("unknown type BUS", () -> VehicleFactory.create("BUS", "B1"));
        Check.throwsEx("negative minutes", () -> new Car("N3").calculateFee(-1));
        Check.throwsEx("exit unknown reg", () -> lot.exit("NOPE", t("11:00"), 0));

        System.out.println("\n=== Hand-calculated day ===");
        double f1 = lot.exit("TN01A1", t("10:30"), 0);     // 90 min  -> 2 x 10      = 20
        double f2 = lot.exit("TN02B2", t("12:15"), 0);     // 180 min -> 3 x 30      = 90
        double f3 = lot.exit("TN03C3", t("11:01"), 10);    // 61 min  -> 2 x 30 + 120 = 180
        Check.eq("TwoWheeler (hand 20)", 20.0, f1);
        Check.eq("Car (hand 90)", 90.0, f2);
        Check.eq("ElectricCar (hand 180)", 180.0, f3);
        Check.eq("Day total (hand 290)", 290.0, lot.collection());
        Check.summary();

        System.out.println("\n=== Alternative: array vs ArrayList vs HashMap (find LAST ticket) ===");
        for (int n : new int[] {1_000, 10_000, 100_000}) {
            ParkingTicket[] arr = new ParkingTicket[n];
            List<ParkingTicket> list = new ArrayList<>(); Map<String, ParkingTicket> map = new HashMap<>();
            for (int i = 0; i < n; i++) {
                ParkingTicket tk = new ParkingTicket(new Car("R" + i), 1, i + 1, LocalTime.NOON);
                arr[i] = tk; list.add(tk); map.put(tk.vehicle.getRegNo(), tk);
            }
            String target = "R" + (n - 1); int reps = 100; long sink = 0;
            long s = System.nanoTime();
            for (int r = 0; r < reps; r++) for (ParkingTicket tk : arr) if (tk.vehicle.getRegNo().equals(target)) { sink += tk.slot; break; }
            long a = (System.nanoTime() - s) / reps;
            s = System.nanoTime();
            for (int r = 0; r < reps; r++) for (ParkingTicket tk : list) if (tk.vehicle.getRegNo().equals(target)) { sink += tk.slot; break; }
            long l = (System.nanoTime() - s) / reps;
            s = System.nanoTime();
            for (int r = 0; r < reps; r++) sink += map.get(target).slot;
            long m = (System.nanoTime() - s) / reps;
            System.out.printf("n=%-7d array=%-8d ns  ArrayList=%-8d ns  HashMap=%-6d ns (sink=%d)%n", n, a, l, m, sink);
        }
        System.out.println("Space: array = n refs; ArrayList = n refs + spare capacity; HashMap = n Entry objects + table (largest).");
    }
}
